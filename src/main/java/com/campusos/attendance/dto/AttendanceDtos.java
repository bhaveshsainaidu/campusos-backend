package com.campusos.attendance.dto;

import com.campusos.attendance.AttendanceRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {
    private AttendanceDtos() {}

    public record MarkRecord(@NotNull Long studentId,
                             @NotNull AttendanceRecord.Status status) {}

    public record MarkSessionRequest(
            @NotNull Long assignmentId,
            @NotNull Long batchId,
            @NotNull LocalDate classDate,
            @NotEmpty List<@Valid MarkRecord> records) {}

    public record MarkedRecordResponse(Long studentId, String rollNumber, String studentName,
                                       AttendanceRecord.Status status) {}

    public record SessionResponse(Long id, Long assignmentId, String courseCode, String courseTitle,
                                  Long batchId, String batchName, LocalDate classDate, String takenBy) {}

    public record CourseAttendanceResponse(Long courseId, String courseCode, String courseTitle,
                                           long present, long total, double percentage) {}

    public record StudentAttendanceResponse(double overallPercentage, long totalPresent, long totalClasses,
                                            List<CourseAttendanceResponse> perCourse,
                                            List<ShortageAlert> shortageAlerts) {}

    public record ShortageAlert(Long studentId, String rollNumber, String studentName,
                                Long courseId, String courseCode, double percentage) {}

    public record RosterEntry(Long studentId, String rollNumber, String studentName) {}
}
