package dev.sabti.alumni_connect.candidate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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

    // Empty -> 403 if the caller isn't a candidate (no CandidateProfile). Must be
    // registered (Spring matches literal path segments before path variables) before
    // /{id} below, so "/me" isn't parsed as a user id.
    @GetMapping("/me")
    public ResponseEntity<CandidateProfileDTO> getMyProfile(@AuthenticationPrincipal UserDetails principal) {
        return candidateService.getMyProfile(principal.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PatchMapping("/me")
    public ResponseEntity<CandidateProfileDTO> updateMyProfile(@AuthenticationPrincipal UserDetails principal,
                                                                 @RequestBody @Valid UpdateCandidateProfileDTO dto) {
        return candidateService.updateMyProfile(principal.getUsername(), dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Upload (or replace) the caller's own CV. Multipart; the content-type guard keeps it to a
    // non-empty PDF (a caller error -> 400, distinct from the 403 for "not a candidate"). Returns
    // the stored file's metadata, including the storageId.
    @PostMapping(value = "/me/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyResume(@AuthenticationPrincipal UserDetails principal,
                                            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getContentType())) {
            return ResponseEntity.badRequest().body("Resume must be a non-empty PDF");
        }
        return candidateService.uploadResume(principal.getUsername(), file)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Stream back the caller's own CV. 403 if they're not a candidate (consistent with the other
    // /me endpoints), 404 if they are but haven't uploaded one. Content-Disposition inline so a
    // browser can preview the PDF; the original filename is echoed.
    @GetMapping("/me/resume")
    public ResponseEntity<Resource> downloadMyResume(@AuthenticationPrincipal UserDetails principal) {
        ResumeDownload result = candidateService.getMyResume(principal.getUsername());
        return switch (result.getStatus()) {
            case FOUND -> ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.getFile().getMetadata().getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + result.getFile().getMetadata().getOriginalFilename() + "\"")
                    .body(result.getFile().getResource());
            case NOT_CANDIDATE -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case NO_RESUME -> ResponseEntity.notFound().build();
        };
    }

    // Upload (or replace) the caller's own profile photo. Multipart; the content-type guard keeps
    // it to a known image format (a caller error -> 400, distinct from the 403 for "not a
    // candidate"). Returns the stored file's metadata, including the storageId. Mirrors the resume
    // upload above.
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyPhoto(@AuthenticationPrincipal UserDetails principal,
                                           @RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null
                || !ALLOWED_PHOTO_TYPES.contains(file.getContentType().toLowerCase())) {
            return ResponseEntity.badRequest().body("Photo must be a PNG, JPEG, or WEBP image");
        }
        return candidateService.uploadPhoto(principal.getUsername(), file)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    // Stream back the caller's own profile photo. 403 if they're not a candidate (consistent with
    // the other /me endpoints), 404 if they are but haven't uploaded one. Content-Disposition inline
    // so a browser can render it directly; the original filename is echoed. Mirrors the resume
    // download above.
    @GetMapping("/me/photo")
    public ResponseEntity<Resource> downloadMyPhoto(@AuthenticationPrincipal UserDetails principal) {
        PhotoDownload result = candidateService.getMyPhoto(principal.getUsername());
        return switch (result.getStatus()) {
            case FOUND -> ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.getFile().getMetadata().getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + result.getFile().getMetadata().getOriginalFilename() + "\"")
                    .body(result.getFile().getResource());
            case NOT_CANDIDATE -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case NO_PHOTO -> ResponseEntity.notFound().build();
        };
    }

    // Admin-only: lookup by User id, e.g. reviewing a pending candidate from
    // /api/admin/pending-users before approve/reject. Restricted to ADMINISTRATOR
    // in SecurityConfig (declared after /me so "/me" isn't shadowed by this matcher).
    @GetMapping("/{id}")
    public ResponseEntity<CandidateProfileDTO> getProfileById(@PathVariable Long id) {
        return candidateService.getProfileById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
