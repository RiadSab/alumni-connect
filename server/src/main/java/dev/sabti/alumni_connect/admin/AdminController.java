package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.company.entities.Company;
import dev.sabti.alumni_connect.company.entities.CompanyStatus;
import dev.sabti.alumni_connect.shared.StatusChangeDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/pending-users")
    public ResponseEntity<Page<User>> getPendingUsers(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingUsers(pageable));
    }

    @GetMapping("/pending-companies")
    public ResponseEntity<Page<Company>> getPendingCompanies(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingCompanies(pageable));
    }

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeUserStatus(id, dto, UserStatus.ACTIVE, "approved");
    }

    @PostMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeUserStatus(id, dto, UserStatus.REJECTED, "rejected");
    }

    @PostMapping("/companies/{id}/approve")
    public ResponseEntity<?> approveCompany(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeCompanyStatus(id, dto, CompanyStatus.ACTIVE, "approved");
    }

    @PostMapping("/companies/{id}/reject")
    public ResponseEntity<?> rejectCompany(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeCompanyStatus(id, dto, CompanyStatus.REJECTED, "rejected");
    }

    private ResponseEntity<?> changeUserStatus(Long id, StatusChangeDTO dto, UserStatus newStatus, String action) {
        if (adminService.changeUserStatus(id, dto.getReason(), newStatus)) {
            return ResponseEntity.ok("User " + action + " successfully");
        }
        return ResponseEntity.status(409).body("User status change failed");
    }

    private ResponseEntity<?> changeCompanyStatus(Long id, StatusChangeDTO dto, CompanyStatus newStatus, String action) {
        if (adminService.changeCompanyStatus(id, dto.getReason(), newStatus)) {
            return ResponseEntity.ok("Company " + action + " successfully");
        }
        return ResponseEntity.status(409).body("Company status change failed");
    }
}
