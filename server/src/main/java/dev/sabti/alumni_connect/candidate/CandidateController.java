package dev.sabti.alumni_connect.candidate;

import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.storage.FileDownload;
import dev.sabti.alumni_connect.storage.StoredFileDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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

import java.util.Set;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {
    private final CandidateService candidateService;

    // Profile photos are limited to the common web image formats (a chosen avatar, not an arbitrary
    // file). Same idea as the PDF-only guard on resume uploads.
    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of(
            MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/webp");

    // 403 if the caller isn't a candidate (no CandidateProfile) — thrown by the service. Must be
    // registered (Spring matches literal path segments before path variables) before /{id} below,
    // so "/me" isn't parsed as a user id.
    @GetMapping("/me")
    public CandidateProfileDTO getMyProfile(@AuthenticationPrincipal UserDetails principal) {
        return candidateService.getMyProfile(principal.getUsername());
    }

    @PatchMapping("/me")
    public CandidateProfileDTO updateMyProfile(@AuthenticationPrincipal UserDetails principal,
                                               @RequestBody @Valid UpdateCandidateProfileDTO dto) {
        return candidateService.updateMyProfile(principal.getUsername(), dto);
    }

    // Upload (or replace) the caller's own CV. Multipart; the content-type guard keeps it to a
    // non-empty PDF (400 BadRequestException, distinct from the 403 the service throws for "not a
    // candidate"). Returns the stored file's metadata, including the storageId.
    @PostMapping(value = "/me/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredFileDTO uploadMyResume(@AuthenticationPrincipal UserDetails principal,
                                        @RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("Resume must be a non-empty PDF");
        }
        return candidateService.uploadResume(principal.getUsername(), file);
    }

    // Stream back the caller's own CV. The service throws 403 (not a candidate) / 404 (no resume).
    // Content-Disposition inline so a browser can preview the PDF; the original filename is echoed.
    @GetMapping("/me/resume")
    public ResponseEntity<Resource> downloadMyResume(@AuthenticationPrincipal UserDetails principal) {
        FileDownload file = candidateService.getMyResume(principal.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMetadata().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getMetadata().getOriginalFilename() + "\"")
                .body(file.getResource());
    }

    // Upload (or replace) the caller's own profile photo. Multipart; the content-type guard keeps it
    // to a known image format (400 BadRequestException, distinct from the 403 for "not a candidate").
    // Returns the stored file's metadata. Mirrors the resume upload above.
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredFileDTO uploadMyPhoto(@AuthenticationPrincipal UserDetails principal,
                                       @RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null
                || !ALLOWED_PHOTO_TYPES.contains(file.getContentType().toLowerCase())) {
            throw new BadRequestException("Photo must be a PNG, JPEG, or WEBP image");
        }
        return candidateService.uploadPhoto(principal.getUsername(), file);
    }

    // Stream back the caller's own profile photo. The service throws 403 (not a candidate) / 404 (no
    // photo). Content-Disposition inline so a browser can render it. Mirrors the resume download.
    @GetMapping("/me/photo")
    public ResponseEntity<Resource> downloadMyPhoto(@AuthenticationPrincipal UserDetails principal) {
        FileDownload file = candidateService.getMyPhoto(principal.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMetadata().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getMetadata().getOriginalFilename() + "\"")
                .body(file.getResource());
    }

    // Admin-only: lookup by User id, e.g. reviewing a pending candidate from
    // /api/admin/pending-users before approve/reject. Restricted to ADMINISTRATOR in SecurityConfig
    // (declared after /me so "/me" isn't shadowed by this matcher). The service throws 404 if the id
    // doesn't exist or isn't a candidate.
    @GetMapping("/{id}")
    public CandidateProfileDTO getProfileById(@PathVariable Long id) {
        return candidateService.getProfileById(id);
    }
}
