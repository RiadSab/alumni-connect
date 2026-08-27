package dev.sabti.alumni_connect.report;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Admin-only via the /api/admin/** rule.
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportController {
    private final EmploymentReportService employmentReportService;

    @GetMapping("/promotions")
    public List<Integer> getPromotionYears() {
        return employmentReportService.getPromotionYears();
    }

    @GetMapping("/employment")
    public EmploymentReportDTO getEmploymentReport(@RequestParam int promotionYear) {
        return employmentReportService.getReport(promotionYear);
    }
}
