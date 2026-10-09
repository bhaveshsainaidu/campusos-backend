package com.campusos.config;

import com.campusos.academics.*;
import com.campusos.attendance.AttendanceRecordRepository;
import com.campusos.attendance.AttendanceSessionRepository;
import com.campusos.fees.FeeStructureRepository;
import com.campusos.fees.StudentFeeRepository;
import com.campusos.notice.NoticeRepository;
import com.campusos.student.Student;
import com.campusos.student.StudentRepository;
import com.campusos.user.Role;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean baseline seeder:
 * Seeds foundational accounts (admin, faculty, demo students), departments, and courses.
 * Intentionally leaves attendance records, fees, and notices EMPTY (0 preloaded records)
 * so users can create, edit, and test all features dynamically in real time.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final BatchRepository batchRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final TimetableSlotRepository slotRepository;
    private final StudentRepository studentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final NoticeRepository noticeRepository;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    private static final String[][] DEPARTMENTS = {
            {"CSE", "Computer Science & Engineering"},
            {"ECE", "Electronics & Communication"},
            {"ME", "Mechanical Engineering"},
            {"CE", "Civil Engineering"},
            {"MBA", "Business Administration"},
    };

    private static final Object[][] COURSES = {
            {"CS101", "Programming Fundamentals", 4, 1, "CSE"},
            {"CS201", "Data Structures", 4, 3, "CSE"},
            {"CS301", "Database Systems", 4, 5, "CSE"},
            {"CS401", "Distributed Systems", 3, 7, "CSE"},
            {"EC101", "Circuit Analysis", 4, 1, "ECE"},
            {"EC201", "Digital Electronics", 4, 3, "ECE"},
            {"ME101", "Engineering Mechanics", 4, 1, "ME"},
            {"ME201", "Thermodynamics", 4, 3, "ME"},
            {"CE101", "Surveying", 3, 1, "CE"},
            {"CE201", "Structural Analysis", 4, 3, "CE"},
            {"MB101", "Principles of Management", 3, 1, "MBA"},
            {"MB201", "Marketing Analytics", 3, 3, "MBA"},
    };

    private static final String[][] DEMO_STUDENTS = {
            {"Aarav Sharma", "student00001@campusos.edu", "+91-9876543201"},
            {"Ananya Patel", "student00002@campusos.edu", "+91-9876543202"},
            {"Rohan Gupta", "student00003@campusos.edu", "+91-9876543203"},
            {"Priya Iyer", "student00004@campusos.edu", "+91-9876543204"},
            {"Ishaan Verma", "student00005@campusos.edu", "+91-9876543205"},
            {"Kavya Nair", "student00006@campusos.edu", "+91-9876543206"},
            {"Aditya Rao", "student00007@campusos.edu", "+91-9876543207"},
            {"Diya Joshi", "student00008@campusos.edu", "+91-9876543208"},
            {"Kabir Mehta", "student00009@campusos.edu", "+91-9876543209"},
            {"Meera Pillai", "student00010@campusos.edu", "+91-9876543210"},
    };

    @Bean
    ApplicationRunner seedRunner() {
        return args -> {
            if (!seedEnabled) return;
            if (userRepository.count() > 0) {
                log.info("Seed skipped: core accounts already present");
                return;
            }
            seed();
        };
    }

    @Transactional
    public void seed() throws Exception {
        long start = System.currentTimeMillis();
        log.info("Seeding clean foundational CampusOS accounts and curriculum...");

        // Pre-compute password hashes
        String adminHash = passwordEncoder.encode("Admin@123");
        String facultyHash = passwordEncoder.encode("Faculty@123");
        String studentHash = passwordEncoder.encode("Student@123");

        // 1. Admin account
        User admin = userRepository.save(User.builder()
                .email("admin@campusos.edu").passwordHash(adminHash)
                .role(Role.ADMIN).fullName("Dr. Priya Sharma").active(true).build());

        // 2. Faculty accounts
        List<User> faculty = new ArrayList<>();
        String[] facultyNames = {
                "Prof. Alan Turing", "Prof. Grace Hopper", "Prof. Claude Shannon",
                "Prof. John von Neumann", "Prof. Ada Lovelace", "Prof. Tim Berners-Lee"
        };
        for (int i = 0; i < facultyNames.length; i++) {
            faculty.add(userRepository.save(User.builder()
                    .email(String.format("faculty%d@campusos.edu", i + 1)).passwordHash(facultyHash)
                    .role(Role.FACULTY)
                    .fullName(facultyNames[i])
                    .active(true).build()));
        }

        // 3. Departments & batches
        List<Department> depts = new ArrayList<>();
        for (String[] d : DEPARTMENTS) {
            depts.add(departmentRepository.save(Department.builder().code(d[0]).name(d[1]).build()));
        }

        List<Batch> batches = new ArrayList<>();
        for (Department dept : depts) {
            batches.add(batchRepository.save(Batch.builder()
                    .department(dept).name(dept.getCode() + "-2026").year(2026).build()));
        }

        // 4. Active semester
        Semester semester = semesterRepository.save(Semester.builder()
                .name("Fall").academicYear("2026-27")
                .startDate(LocalDate.now().minusMonths(1)).endDate(LocalDate.now().plusMonths(4))
                .active(true).build());

        // 5. Courses
        List<Course> courses = new ArrayList<>();
        for (Object[] c : COURSES) {
            Department dept = depts.stream().filter(d -> d.getCode().equals(c[4])).findFirst().orElseThrow();
            courses.add(courseRepository.save(Course.builder()
                    .code((String) c[0]).title((String) c[1]).credits((Integer) c[2]).semesterNum((Integer) c[3]).department(dept).build()));
        }

        // 6. Course assignments + sample timetable
        List<CourseAssignment> assignments = new ArrayList<>();
        int fi = 0;
        for (Course course : courses) {
            CourseAssignment ca = assignmentRepository.save(CourseAssignment.builder()
                    .course(course).semester(semester).faculty(faculty.get(fi % faculty.size()))
                    .section("A").build());
            assignments.add(ca);

            Batch batch = batches.stream().filter(b -> b.getDepartment().getId().equals(course.getDepartment().getId()))
                    .findFirst().orElse(batches.get(0));

            for (int day = 1; day <= 3; day++) {
                slotRepository.save(TimetableSlot.builder()
                        .assignment(ca).batch(batch).dayOfWeek(day)
                        .startTime(LocalTime.of(9 + (fi % 4), 0)).endTime(LocalTime.of(9 + (fi % 4), 50))
                        .room("Hall " + (101 + (fi % 5))).build());
            }
            fi++;
        }

        // 7. Foundational demo students (clean roster for attendance, fees, marks)
        Batch cseBatch = batches.get(0);
        for (int i = 0; i < DEMO_STUDENTS.length; i++) {
            String[] sInfo = DEMO_STUDENTS[i];
            User studentUser = userRepository.save(User.builder()
                    .email(sInfo[1]).passwordHash(studentHash)
                    .role(Role.STUDENT).fullName(sInfo[0]).active(true).build());

            studentRepository.save(Student.builder()
                    .user(studentUser)
                    .rollNumber(String.format("CSE2026%04d", i + 1))
                    .name(sInfo[0])
                    .email(sInfo[1])
                    .phone(sInfo[2])
                    .gender(i % 2 == 0 ? Student.Gender.MALE : Student.Gender.FEMALE)
                    .dob(LocalDate.of(2004, 3 + (i % 8), 10 + (i % 15)))
                    .admissionDate(LocalDate.of(2026, 7, 1))
                    .guardianName("Parent " + sInfo[0].split(" ")[1])
                    .address("Campus Residential Hall " + (char) ('A' + (i % 4)))
                    .status(Student.Status.ACTIVE)
                    .department(cseBatch.getDepartment())
                    .batch(cseBatch)
                    .build());
        }

        // NOTE: Attendance records, fee invoices, and notices are intentionally LEFT EMPTY!
        // This gives users a clean canvas to mark attendance, assign fees, and broadcast notices in real time.
        log.info("Clean baseline seed completed in {} ms (Attendance: 0, Fees: 0, Notices: 0). Ready for live operations!",
                System.currentTimeMillis() - start);
    }
}
