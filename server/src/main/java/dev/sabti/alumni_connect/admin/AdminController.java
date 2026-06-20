package dev.sabti.alumni_connect.admin;

import dev.sabti.alumni_connect.auth.entities.UserStatus;
import dev.sabti.alumni_connect.auth.entities.UserType;
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

    // Admin dashboard counts.
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/pending-users")
    public ResponseEntity<Page<AdminUserDTO>> getPendingUsers(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingUsers(pageable));
    }

    // Moderation browse over every user, optionally filtered by status and/or type (no filter =
    // all). Lets the admin find an already-active account (e.g. a scammer) to suspend — the
    // suspend/profile endpoints already exist, this is the missing "find the target" step.
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDTO>> getUsers(@RequestParam(required = false) UserStatus status,
                                                       @RequestParam(required = false) UserType type,
                                                       @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(adminService.getUsers(status, type, pageable));
    }

    @GetMapping("/pending-companies")
    public ResponseEntity<Page<AdminCompanyDTO>> getPendingCompanies(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingCompanies(pageable));
    }

    // Moderation browse over every company, optionally filtered by status (no filter = all) —
    // the company counterpart to GET /users, surfacing already-active companies to suspend.
    @GetMapping("/companies")
    public ResponseEntity<Page<AdminCompanyDTO>> getCompanies(@RequestParam(required = false) CompanyStatus status,
                                                              @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(adminService.getCompanies(status, pageable));
    }

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeUserStatus(id, dto, UserStatus.ACTIVE, "approved");
    }

    @PostMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeUserStatus(id, dto, UserStatus.REJECTED, "rejected");
    }

    // Suspend/reactivate are post-approval lifecycle actions (acting on an already-ACTIVE
    // account), distinct from approve/reject which act on a PENDING one. Both reuse the same
    // generic changeUserStatus path — reactivate targets ACTIVE just like approve, but stays
    // a separate endpoint so the admin's intent (un-suspending) reads clearly.
    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeUserStatus(id, dto, UserStatus.SUSPENDED, "suspended");
    }

    @PostMapping("/users/{id}/reactivate")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeUserStatus(id, dto, UserStatus.ACTIVE, "reactivated");
    }

    @PostMapping("/companies/{id}/approve")
    public ResponseEntity<?> approveCompany(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeCompanyStatus(id, dto, CompanyStatus.ACTIVE, "approved");
    }

    @PostMapping("/companies/{id}/reject")
    public ResponseEntity<?> rejectCompany(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeCompanyStatus(id, dto, CompanyStatus.REJECTED, "rejected");
    }

    @PostMapping("/companies/{id}/suspend")
    public ResponseEntity<?> suspendCompany(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeCompanyStatus(id, dto, CompanyStatus.SUSPENDED, "suspended");
    }

    @PostMapping("/companies/{id}/reactivate")
    public ResponseEntity<?> reactivateCompany(@PathVariable Long id, @RequestBody @Valid StatusChangeDTO dto) {
        return changeCompanyStatus(id, dto, CompanyStatus.ACTIVE, "reactivated");
    }

    // The service throws on failure (404 no such user, 409 already in the target status); reaching
    // here means it succeeded, so we just confirm the action.
    private ResponseEntity<?> changeUserStatus(Long id, StatusChangeDTO dto, UserStatus newStatus, String action) {
        adminService.changeUserStatus(id, dto.getReason(), newStatus);
        return ResponseEntity.ok("User " + action + " successfully");
    }

    private ResponseEntity<?> changeCompanyStatus(Long id, StatusChangeDTO dto, CompanyStatus newStatus, String action) {
        adminService.changeCompanyStatus(id, dto.getReason(), newStatus);
        return ResponseEntity.ok("Company " + action + " successfully");
    }
}
