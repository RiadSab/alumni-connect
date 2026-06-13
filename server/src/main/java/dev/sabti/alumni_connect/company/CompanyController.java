package dev.sabti.alumni_connect.company;


import dev.sabti.alumni_connect.storage.FileDownload;
import dev.sabti.alumni_connect.storage.StoredFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    // Logos are limited to the common web image formats, same guard as candidate profile photos.
    private static final Set<String> ALLOWED_LOGO_TYPES = Set.of(
            MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp");

    @GetMapping
    public ResponseEntity<Page<CompanyDTO>> getActiveCompanies(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(companyService.getActiveCompanies(pageable));
    }

    // Public single-company profile — the company page a candidate reaches by clicking the
    // company on a job offer (JobOfferDTO only carries companyId + companyName). Permitted in
    // SecurityConfig by the GET /api/companies/** matcher. Empty -> 404 (no such id, or the
    // company isn't publicly visible — indistinguishable on purpose).
    @GetMapping("/{id}")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        return companyService.getVisibleCompanyById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // A company OWNER edits their own company profile. Scoped to the caller's own company (resolved
    // from their CompanyUserProfile in the service), so no path id. OWNER-only authority is checked
    // in the service; Optional empty -> 403, the same soft-failure pattern as the role-change
    // endpoint. (PATCH, so it isn't caught by the public GET /api/companies/** matcher.)
    @PatchMapping("/me")
    public ResponseEntity<CompanyDTO> updateMyCompany(@AuthenticationPrincipal UserDetails principal,
                                                      @RequestBody @Valid UpdateCompanyDTO dto) {
        return companyService.updateMyCompany(principal.getUsername(), dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Upload (or replace) the caller's own company logo. Multipart; the content-type guard keeps it
    // to a known image format (a caller error -> 400, distinct from the 403 for "not an OWNER").
    // OWNER-only authority is checked in the service. Returns the stored file's metadata, including
    // the storageId (which also lands on CompanyDTO.logoId).
    @PostMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyCompanyLogo(@AuthenticationPrincipal UserDetails principal,
                                                 @RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null
                || !ALLOWED_LOGO_TYPES.contains(file.getContentType().toLowerCase())) {
            return ResponseEntity.badRequest().body("Logo must be a PNG, JPEG, or WEBP image");
        }
        return companyService.uploadLogo(principal.getUsername(), file)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Public logo image for a company, fetched via the logoId on CompanyDTO. Public (the company
    // profile itself is public), but only for publicly visible companies — checked in the service.
    // Content-Disposition inline so a browser can render it directly. A miss (no such company, not
    // publicly visible, or no logo) -> 404.
    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> getCompanyLogo(@PathVariable Long id) {
        Optional<FileDownload> result = companyService.getVisibleCompanyLogo(id);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileDownload download = result.get();
        StoredFile metadata = download.getMetadata();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(download.getResource());
    }
}
