package com.campusos.attendance;

import com.campusos.attendance.dto.AttendanceDtos.*;
import com.campusos.security.AuthPrincipal;
import com.campusos.student.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final StudentService studentService;

    @PostMapping("/attendance/sessions")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Mark attendance for one class session (bulk records)")
    public SessionResponse markSession(@Valid @RequestBody MarkSessionRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return attendanceService.markSession(principal, request);
    }

    @GetMapping("/attendance/sessions")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "List attendance sessions for a course assignment")
    public List<SessionResponse> sessions(@RequestParam Long assignmentId) {
        return attendanceService.sessionsForAssignment(assignmentId);
    }

    @GetMapping("/attendance/sessions/{id}/records")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Records of a marked session")
    public List<MarkedRecordResponse> sessionRecords(@PathVariable Long id) {
        return attendanceService.sessionRecords(id);
    }

    @GetMapping("/attendance/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Current student's attendance summary with shortage alerts")
    public StudentAttendanceResponse myAttendance(@AuthenticationPrincipal AuthPrincipal principal) {
        var student = studentService.getByUserId(principal.id());
        return attendanceService.studentAttendance(student.getId());
    }

    @GetMapping("/attendance/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Attendance summary for any student")
    public StudentAttendanceResponse studentAttendance(@PathVariable Long studentId) {
        return attendanceService.studentAttendance(studentId);
    }

    @GetMapping("/attendance/shortage")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Students of a batch below 75% attendance")
    public List<ShortageAlert> shortage(@RequestParam Long batchId) {
        return attendanceService.batchShortage(batchId);
    }

    @GetMapping("/attendance/roster")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Roster of a batch for marking")
    public List<RosterEntry> roster(@RequestParam Long batchId) {
        return attendanceService.roster(batchId);
    }

    @GetMapping("/attendance/threshold")
    @Operation(summary = "Minimum attendance threshold")
    public Map<String, Object> threshold() {
        return Map.of("minimumPercent", AttendanceService.MINIMUM_ATTENDANCE_PERCENT);
    }

    @DeleteMapping("/attendance/sessions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable Long id) {
        attendanceService.deleteSession(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Session deleted"));
    }
}
