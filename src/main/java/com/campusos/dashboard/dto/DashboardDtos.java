package com.campusos.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record AdminKpis(long totalStudents, long totalFaculty, long totalDepartments,
                            long activeCourses, long pendingFees, BigDecimal outstandingDues) {}

    public record AdmissionTrendPoint(String month, long count) {}

    public record AttendanceTrendPoint(String date, double percentage) {}

    public record DepartmentStat(String departmentCode, String departmentName, long students, double avgAttendance) {}

    public record AdminDashboard(AdminKpis kpis, List<AdmissionTrendPoint> admissionTrend,
                                 List<AttendanceTrendPoint> attendanceTrend,
                                 List<DepartmentStat> departmentStats) {}

    public record FacultyCourseStat(Long assignmentId, String courseCode, String courseTitle,
                                    String section, long sessionsTaken, String batchName) {}

    public record FacultyDashboard(String facultyName, long totalCourses, long totalSessions,
                                   List<FacultyCourseStat> courses,
                                   long upcomingExams) {}

    public record StudentSummary(double overallAttendance, long totalPresent, long totalClasses,
                                 BigDecimal outstandingFees, double cgpa,
                                 List<com.campusos.attendance.dto.AttendanceDtos.ShortageAlert> shortageAlerts) {}

    public record StudentDashboard(String studentName, String rollNumber, String departmentName,
                                   String batchName, StudentSummary summary) {}
}
