package com.campusos.exam.dto;

import com.campusos.exam.ExamSchedule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ExamDtos {
    private ExamDtos() {}

    public record ScheduleRequest(
            @NotNull Long assignmentId,
            @NotNull ExamSchedule.ExamType examType,
            @NotNull LocalDate examDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @NotBlank @Size(max = 50) String room,
            @Min(1) @Max(1000) int maxMarks) {}

    public record ScheduleResponse(Long id, Long assignmentId, String courseCode, String courseTitle,
                                   String facultyName, String semesterName, ExamSchedule.ExamType examType,
                                   LocalDate examDate, LocalTime startTime, LocalTime endTime,
                                   String room, int maxMarks) {}

    public record MarkEntry(@NotNull Long studentId,
                            @NotNull @DecimalMin("0") BigDecimal marks) {}

    public record MarksEntryRequest(@NotNull Long examScheduleId,
                                    @NotEmpty List<@Valid MarkEntry> marks) {}

    public record MarksEntryResult(int saved, int errors, List<String> messages) {}

    public record ResultRow(Long courseId, String courseCode, String courseTitle, int credits,
                            Long semesterId, String semesterName, String examType,
                            BigDecimal marks, int maxMarks, String grade, BigDecimal gradePoints) {}

    public record StudentResultsResponse(Long studentId, String rollNumber, String studentName,
                                         double cgpa, List<SemesterGpa> semesters, List<ResultRow> results) {}

    public record SemesterGpa(Long semesterId, String semesterName, double sgpa) {}

    public record MarksheetJobResponse(String jobId, String status, String downloadUrl) {}
}
