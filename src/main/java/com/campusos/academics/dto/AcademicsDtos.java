package com.campusos.academics.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public final class AcademicsDtos {
    private AcademicsDtos() {}

    // ---- Departments ----
    public record DepartmentRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 255) String name) {}

    public record DepartmentResponse(Long id, String code, String name) {}

    // ---- Semesters ----
    public record SemesterRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Size(max = 20) String academicYear,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            boolean active) {}

    public record SemesterResponse(Long id, String name, String academicYear,
                                   LocalDate startDate, LocalDate endDate, boolean active) {}

    // ---- Courses ----
    public record CourseRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 255) String title,
            @Min(1) @Max(10) int credits,
            @Min(1) @Max(12) int semesterNum,
            @NotNull Long departmentId) {}

    public record CourseResponse(Long id, String code, String title, int credits, int semesterNum,
                                 Long departmentId, String departmentCode) {}

    // ---- Assignments ----
    public record AssignmentRequest(
            @NotNull Long courseId,
            @NotNull Long semesterId,
            @NotNull Long facultyId,
            @Size(max = 10) String section) {}

    public record AssignmentResponse(Long id, Long courseId, String courseCode, String courseTitle,
                                     Long semesterId, String semesterName, Long facultyId,
                                     String facultyName, String section) {}

    // ---- Timetable ----
    public record TimetableSlotRequest(
            @NotNull Long assignmentId,
            @NotNull Long batchId,
            @Min(1) @Max(7) int dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @NotBlank @Size(max = 50) String room) {}

    public record TimetableSlotResponse(Long id, Long assignmentId, String courseCode, String courseTitle,
                                        String facultyName, String section, Long batchId, String batchName,
                                        int dayOfWeek, LocalTime startTime, LocalTime endTime, String room) {}

    public record ClashError(String conflictType, String detail) {}
}
