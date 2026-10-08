package com.campusos.academics;

import com.campusos.academics.dto.AcademicsDtos.*;
import com.campusos.common.PageResponse;
import com.campusos.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Academics")
public class AcademicsController {

    private final AcademicsService service;

    // ---- Departments ----
    @GetMapping("/departments")
    @Operation(summary = "List departments")
    public List<DepartmentResponse> departments() {
        return service.listDepartments().stream().map(AcademicsMapper::toResponse).toList();
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest req) {
        return AcademicsMapper.toResponse(service.createDepartment(req));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteDepartment(@PathVariable Long id) {
        service.deleteDepartment(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ---- Semesters ----
    @GetMapping("/semesters")
    @Operation(summary = "List semesters")
    public List<SemesterResponse> semesters() {
        return service.listSemesters().stream().map(AcademicsMapper::toResponse).toList();
    }

    @PostMapping("/semesters")
    @PreAuthorize("hasRole('ADMIN')")
    public SemesterResponse createSemester(@Valid @RequestBody SemesterRequest req) {
        return AcademicsMapper.toResponse(service.createSemester(req));
    }

    @PatchMapping("/semesters/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public SemesterResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return AcademicsMapper.toResponse(service.setActive(id, active));
    }

    // ---- Courses ----
    @GetMapping("/courses")
    @Operation(summary = "List courses (paginated), optional departmentId filter")
    public PageResponse<CourseResponse> courses(@RequestParam(required = false) Long departmentId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        Page<Course> p = service.listCourses(departmentId, page, size);
        return PageResponse.of(p, AcademicsMapper::toResponse);
    }

    @PostMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse createCourse(@Valid @RequestBody CourseRequest req) {
        return AcademicsMapper.toResponse(service.createCourse(req));
    }

    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCourse(@PathVariable Long id) {
        service.deleteCourse(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ---- Faculty assignment ----
    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "List course assignments for a semester")
    public List<AssignmentResponse> assignments(@RequestParam Long semesterId) {
        return service.listAssignments(semesterId).stream().map(AcademicsMapper::toResponse).toList();
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public AssignmentResponse createAssignment(@Valid @RequestBody AssignmentRequest req) {
        return AcademicsMapper.toResponse(service.assignFaculty(req));
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteAssignment(@PathVariable Long id) {
        service.deleteAssignment(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ---- Timetable ----
    @GetMapping("/timetable")
    @Operation(summary = "Timetable for a batch, or for the current faculty user")
    public List<TimetableSlotResponse> timetable(@RequestParam(required = false) Long batchId,
                                                 @AuthenticationPrincipal AuthPrincipal principal) {
        if (batchId != null) {
            return service.timetableForBatch(batchId).stream().map(AcademicsMapper::toResponse).toList();
        }
        if ("FACULTY".equals(principal.role())) {
            return service.timetableForFaculty(principal.id()).stream().map(AcademicsMapper::toResponse).toList();
        }
        return List.of();
    }

    @PostMapping("/timetable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a timetable slot with clash detection (batch, faculty, room)")
    public TimetableSlotResponse createSlot(@Valid @RequestBody TimetableSlotRequest req) {
        return AcademicsMapper.toResponse(service.createSlot(req));
    }

    @DeleteMapping("/timetable/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteSlot(@PathVariable Long id) {
        service.deleteSlot(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
