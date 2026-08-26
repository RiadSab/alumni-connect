package dev.sabti.alumni_connect.alumni;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// Admin-only: /api/admin/** is already restricted to ADMINISTRATOR in SecurityConfig.
@RestController
@RequestMapping("/api/admin/alumni")
@RequiredArgsConstructor
public class AlumniAdminController {
    private final AlumniImportService alumniImportService;
    private final AlumniClaimService alumniClaimService;

    @PostMapping("/import")
    public ResponseEntity<AlumniImportResultDTO> importRoster(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(alumniImportService.importCsv(file, dryRun));
    }

    // Emails a one-time claim link to every unclaimed row that has an address.
    @PostMapping("/invite")
    public ResponseEntity<ClaimInviteResultDTO> invite(@RequestParam(required = false) Integer promotionYear) {
        return ResponseEntity.ok(alumniClaimService.invite(promotionYear));
    }

    // Fallback for graduates with no address on the school's list: link an account they made themselves.
    @PostMapping("/{id}/link")
    public ResponseEntity<AlumniRecordDTO> link(@PathVariable Long id, @RequestBody @Valid LinkAccountDTO dto) {
        return ResponseEntity.ok(alumniClaimService.link(id, dto.getEmail()));
    }

    @GetMapping
    public ResponseEntity<Page<AlumniRecordDTO>> getRecords(
            @RequestParam(required = false) Integer promotionYear,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alumniImportService.getRecords(promotionYear, pageable));
    }
}
