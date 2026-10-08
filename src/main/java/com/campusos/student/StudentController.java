package com.campusos.student;

import com.campusos.common.ApiException;
import com.campusos.common.PageResponse;
import com.campusos.security.AuthPrincipal;
import com.campusos.student.dto.StudentDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Students")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Search students (paginated). Filters: query, departmentId, batchId, status")
    public PageResponse<StudentResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Student.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Student> p = studentService.search(query, departmentId, batchId, status, page, size);
        return PageResponse.of(p, StudentMapper::toResponse);
    }

    @GetMapping("/students/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Get a student profile")
    public StudentResponse get(@PathVariable Long id) {
        return StudentMapper.toResponse(studentService.getById(id));
    }

    @GetMapping("/students/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get the current student's own profile")
    public StudentResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return StudentMapper.toResponse(studentService.getByUserId(principal.id()));
    }

    @PostMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admit a new student (creates the linked user account)")
    public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.admit(request);
    }

    @PutMapping("/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a student profile")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/students/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a student and their user account")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Student deleted"));
    }

    @PostMapping(value = "/students/import-csv", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk import students from CSV")
    public CsvImportResult importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw ApiException.badRequest("File is empty");
        try {
            return studentService.importCsv(file.getBytes());
        } catch (java.io.IOException ex) {
            throw ApiException.badRequest("Could not read uploaded file");
        }
    }
}
