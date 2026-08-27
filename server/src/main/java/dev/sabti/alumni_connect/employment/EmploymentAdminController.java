package dev.sabti.alumni_connect.employment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Admin-only via the /api/admin/** rule. Runs the sweep now instead of waiting for Monday.
@RestController
@RequestMapping("/api/admin/employment")
@RequiredArgsConstructor
public class EmploymentAdminController {
    private final EmploymentNudgeService employmentNudgeService;

    @PostMapping("/nudge")
    public Map<String, Integer> nudge() {
        return Map.of("sent", employmentNudgeService.sendNudges());
    }
}
