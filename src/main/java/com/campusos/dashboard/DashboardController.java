package com.campusos.dashboard;

import com.campusos.dashboard.dto.DashboardDtos.*;
import com.campusos.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin KPIs, admission trend, attendance trend, department stats")
    public AdminDashboard admin() {
        return dashboardService.adminDashboard();
    }

    @GetMapping("/faculty")
    @PreAuthorize("hasRole('FACULTY')")
    @Operation(summary = "Current faculty's teaching stats")
    public FacultyDashboard faculty(@AuthenticationPrincipal AuthPrincipal principal) {
        return dashboardService.facultyDashboard(principal.id());
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Current student's summary: attendance, dues, CGPA, alerts")
    public StudentDashboard student(@AuthenticationPrincipal AuthPrincipal principal) {
        return dashboardService.studentDashboard(principal.id());
    }
}
