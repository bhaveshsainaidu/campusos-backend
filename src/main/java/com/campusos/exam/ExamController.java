package com.campusos.exam;

import com.campusos.common.ApiException;
import com.campusos.exam.dto.ExamDtos.*;
import com.campusos.exam.dto.ExamDtos.ResultRow;
import com.campusos.security.AuthPrincipal;
import com.campusos.student.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@Tag(name = "Exams & Results")
public class ExamController {

    private final ExamService examService;
    private final StudentService studentService;
    private final MarksheetPdfService pdfService;

    @GetMapping("/schedules")
    @Operation(summary = "Exam schedules, optional semesterId filter")
    public List<ScheduleResponse> schedules(@RequestParam(required = false) Long semesterId) {
        return examService.schedules(semesterId);
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Schedule an exam")
    public ScheduleResponse createSchedule(@Valid @RequestBody ScheduleRequest request) {
        return examService.createSchedule(request);
    }

    @PostMapping("/marks")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Bulk marks entry for an exam schedule")
    public MarksEntryResult enterMarks(@Valid @RequestBody MarksEntryRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return examService.enterMarks(principal, request);
    }

    @GetMapping("/results/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Current student's results with SGPA per semester and CGPA")
    public StudentResultsResponse myResults(@AuthenticationPrincipal AuthPrincipal principal) {
        var student = studentService.getByUserId(principal.id());
        return examService.studentResults(student.getId());
    }

    @GetMapping("/results/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Any student's results with SGPA per semester and CGPA")
    public StudentResultsResponse studentResults(@PathVariable Long studentId) {
        return examService.studentResults(studentId);
    }

    @PostMapping("/marksheets")
    @Operation(summary = "Request an async PDF marksheet; poll the returned job")
    public MarksheetJobResponse requestMarksheet(@RequestParam Long studentId,
                                                 @AuthenticationPrincipal AuthPrincipal principal) {
        if ("STUDENT".equals(principal.role())) {
            var student = studentService.getByUserId(principal.id());
            if (!student.getId().equals(studentId)) {
                throw ApiException.forbidden("Students can only request their own marksheet");
            }
        }
        return examService.requestMarksheet(studentId);
    }

    @GetMapping("/marksheets/{jobId}")
    @Operation(summary = "Poll marksheet generation job status")
    public MarksheetJobResponse marksheetStatus(@PathVariable String jobId) {
        var job = examService.marksheetJob(jobId);
        return new MarksheetJobResponse(job.getId(), job.getStatus().name(),
                job.getStatus() == MarksheetJob.Status.COMPLETED
                        ? "/api/v1/exams/marksheets/" + jobId + "/download" : null);
    }

    @GetMapping("/marksheets/{jobId}/download")
    @Operation(summary = "Download the generated PDF marksheet")
    public ResponseEntity<FileSystemResource> downloadMarksheet(@PathVariable String jobId) {
        Path path = pdfService.filePath(jobId);
        FileSystemResource resource = new FileSystemResource(path.toFile());
        if (!resource.exists()) throw ApiException.notFound("Marksheet file missing");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=marksheet.pdf")
                .body(resource);
    }
}
