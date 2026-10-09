package com.campusos.dashboard;

import com.campusos.academics.CourseAssignmentRepository;
import com.campusos.attendance.AttendanceRecordRepository;
import com.campusos.attendance.dto.AttendanceDtos.CourseAttendanceResponse;
import com.campusos.attendance.dto.AttendanceDtos.ShortageAlert;
import com.campusos.common.ApiException;
import com.campusos.dashboard.dto.DashboardDtos.*;
import com.campusos.exam.ExamService;
import com.campusos.fees.FeeService;
import com.campusos.fees.StudentFee;
import com.campusos.student.Student;
import com.campusos.student.StudentRepository;
import com.campusos.user.Role;
import com.campusos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ExamService examService;
    private final FeeService feeService;
    private final com.campusos.academics.DepartmentRepository departmentRepository;
    private final com.campusos.academics.CourseRepository courseRepository;

    // ---------- Admin ----------
    @Transactional(readOnly = true)
    public AdminDashboard adminDashboard() {
        long students = studentRepository.count();
        long faculty = userRepository.countByRole(Role.FACULTY);
        long departments = departmentRepository.count();
        long courses = courseRepository.count();
        var dues = feeService.duesSummary();

        List<AdmissionTrendPoint> trend = studentRepository.admissionTrend(LocalDate.now().minusMonths(12))
                .stream()
                .map(row -> {
                    int y = ((Number) row[0]).intValue();
                    int m = ((Number) row[1]).intValue();
                    String ym = String.format("%04d-%02d", y, m);
                    return new AdmissionTrendPoint(ym, ((Number) row[2]).longValue());
                })
                .toList();

        List<AttendanceTrendPoint> att = attendanceRecordRepository.dailyTrend(LocalDate.now().minusDays(14))
                .stream()
                .map(row -> {
                    long present = ((Number) row[1]).longValue();
                    long total = ((Number) row[2]).longValue();
                    double pct = total > 0 ? Math.round((present * 10000.0) / total) / 100.0 : 0;
                    return new AttendanceTrendPoint(String.valueOf(row[0]), pct);
                })
                .toList();

        List<DepartmentStat> deptStats = studentRepository.perDepartmentCounts().stream()
                .map(row -> new DepartmentStat((String) row[0], (String) row[1],
                        ((Number) row[2]).longValue(), 0))
                .toList();

        return new AdminDashboard(
                new AdminKpis(students, faculty, departments, courses,
                        dues.studentsWithDues(), dues.totalOutstanding()),
                trend, att, deptStats);
    }

    // ---------- Faculty ----------
    @Transactional(readOnly = true)
    public FacultyDashboard facultyDashboard(Long facultyUserId) {
        var assignments = assignmentRepository.findByFacultyId(facultyUserId);
        List<FacultyCourseStat> courses = assignments.stream()
                .map(a -> new FacultyCourseStat(a.getId(), a.getCourse().getCode(),
                        a.getCourse().getTitle(), a.getSection(),
                        attendanceRecordRepository.countSessionsForAssignment(a.getId()),
                        null))
                .toList();
        var faculty = userRepository.findById(facultyUserId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        long upcomingExams = examService.schedules(null).stream()
                .filter(s -> s.facultyName().equals(faculty.getFullName()))
                .filter(s -> s.examDate().isAfter(LocalDate.now().minusDays(1)))
                .count();
        return new FacultyDashboard(faculty.getFullName(), courses.size(),
                courses.stream().mapToLong(FacultyCourseStat::sessionsTaken).sum(), courses, upcomingExams);
    }

    // ---------- Student ----------
    @Transactional(readOnly = true)
    public StudentDashboard studentDashboard(Long studentUserId) {
        Student s = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> ApiException.notFound("No student profile for this user"));
        var att = attendanceRecordRepository.statsOverall(s.getId());
        var fees = feeService.studentFees(s.getId());
        BigDecimal outstanding = fees.stream()
                .filter(f -> !"PAID".equals(f.status().name()))
                .map(f -> f.dueAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var results = examService.studentResults(s.getId());
        List<ShortageAlert> alerts = attendanceRecordRepository.statsPerCourse(s.getId()).stream()
                .filter(c -> c.total() > 0 && c.percentage() < 75.0)
                .map(c -> new ShortageAlert(s.getId(), s.getRollNumber(), s.getName(),
                        c.courseId(), c.courseCode(), c.percentage()))
                .toList();
        StudentSummary summary = new StudentSummary(att.percentage(),
                att.present() == null ? 0 : att.present(),
                att.total() == null ? 0 : att.total(),
                outstanding, results.cgpa(), alerts);
        return new StudentDashboard(s.getName(), s.getRollNumber(),
                s.getDepartment() != null ? s.getDepartment().getName() : null,
                s.getBatch() != null ? s.getBatch().getName() : null, summary);
    }

}
