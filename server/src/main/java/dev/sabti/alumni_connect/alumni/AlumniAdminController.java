package dev.sabti.alumni_connect.alumni;

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

    @PostMapping("/import")
    public ResponseEntity<AlumniImportResultDTO> importRoster(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(alumniImportService.importCsv(file, dryRun));
    }

    @GetMapping
    public ResponseEntity<Page<AlumniRecordDTO>> getRecords(
            @RequestParam(required = false) Integer promotionYear,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alumniImportService.getRecords(promotionYear, pageable));
    }
}
