package com.campusos.config;

import com.campusos.academics.*;
import com.campusos.attendance.AttendanceRecord;
import com.campusos.attendance.AttendanceRecordRepository;
import com.campusos.attendance.AttendanceSession;
import com.campusos.attendance.AttendanceSessionRepository;
import com.campusos.fees.*;
import com.campusos.notice.Notice;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeds realistic demo data on first run when app.seed.enabled=true.
 * Produces ~10,000 students across 5 departments and 20 batches.
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

    @Value("${app.seed.enabled:false}")
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
    private static final String[] FIRST = {"Aarav", "Diya", "Vihaan", "Ananya", "Ishaan", "Meera", "Kabir", "Saanvi",
            "Arjun", "Riya", "Aditya", "Ira", "Rohan", "Tara", "Neel", "Kiara", "Dev", "Zara", "Yash", "Myra"};
    private static final String[] LAST = {"Sharma", "Patel", "Reddy", "Iyer", "Khan", "Gupta", "Nair", "Mehta",
            "Singh", "Kulkarni", "Das", "Bose", "Rao", "Joshi", "Verma", "Menon"};

    @Bean
    ApplicationRunner seedRunner() {
        return args -> {
            if (!seedEnabled) return;
            if (studentRepository.count() > 0) {
                log.info("Seed skipped: data already present");
                return;
            }
            seed();
        };
    }

    @Transactional
    protected void seed() throws Exception {
        long start = System.currentTimeMillis();
        Random rnd = ThreadLocalRandom.current();
        log.info("Seeding demo data...");

        // Admin + faculty
        User admin = saveUser("admin@campusos.edu", "Admin@123", Role.ADMIN, "Dr. Priya Sharma");
        List<User> faculty = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            faculty.add(saveUser(String.format("faculty%d@campusos.edu", i), "Faculty@123", Role.FACULTY,
                    "Prof. " + FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)]));
        }

        // Departments & batches
        List<Department> depts = new ArrayList<>();
        for (String[] d : DEPARTMENTS) {
            depts.add(departmentRepository.save(Department.builder().code(d[0]).name(d[1]).build()));
        }
        List<Batch> batches = new ArrayList<>();
        int batchNo = 0;
        for (Department dept : depts) {
            for (int year = 2023; year <= 2025; year++) {
                batchNo++;
                batches.add(batchRepository.save(Batch.builder()
                        .department(dept).name(dept.getCode() + "-" + year).year(year).build()));
            }
        }

        // Active semester
        Semester semester = semesterRepository.save(Semester.builder()
                .name("Fall").academicYear("2026-27")
                .startDate(LocalDate.now().minusMonths(2)).endDate(LocalDate.now().plusMonths(2))
                .active(true).build());

        // Courses
        List<Course> courses = new ArrayList<>();
        for (Object[] c : COURSES) {
            Department dept = depts.stream().filter(d -> d.getCode().equals(c[4])).findFirst().orElseThrow();
            courses.add(courseRepository.save(Course.builder()
                    .code((String) c[0]).title((String) c[1]).credits((Integer) c[2]).semesterNum((Integer) c[3]).department(dept).build()));
        }

        // Assignments + timetable (every course gets a faculty, section A)
        List<CourseAssignment> assignments = new ArrayList<>();
        int fi = 0;
        for (Course course : courses) {
            CourseAssignment ca = assignmentRepository.save(CourseAssignment.builder()
                    .course(course).semester(semester).faculty(faculty.get(fi % faculty.size()))
                    .section("A").build());
            fi++;
            assignments.add(ca);
            Batch batch = batches.stream().filter(b -> b.getDepartment().getId().equals(course.getDepartment().getId()))
                    .findFirst().orElse(batches.get(0));
            for (int day = 1; day <= 5; day++) {
                slotRepository.save(TimetableSlot.builder()
                        .assignment(ca).batch(batch).dayOfWeek(day)
                        .startTime(LocalTime.of(9 + (day % 4), 0)).endTime(LocalTime.of(9 + (day % 4), 50))
                        .room("R-" + (100 + fi)).build());
            }
        }

        // 10,000 students in batches of 1,000 (jdbc-batched inserts)
        log.info("Creating 10,000 students...");
        List<Student> allStudents = new ArrayList<>();
        int roll = 1;
        for (Batch batch : batches) {
            int count = "2025".equals(String.valueOf(batch.getYear())) ? 1500 : 1000;
            for (int i = 0; i < count; i++) {
                String name = FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)];
                String email = String.format("student%05d@campusos.edu", roll);
                User u = saveUser(email, "Student@123", Role.STUDENT, name);
                Student s = Student.builder()
                        .user(u).rollNumber(String.format("%s%04d", batch.getName().replace("-", ""), roll))
                        .name(name).email(email).phone("+91-9" + (100000000 + rnd.nextInt(899999999)))
                        .gender(rnd.nextBoolean() ? Student.Gender.MALE : Student.Gender.FEMALE)
                        .dob(LocalDate.of(batch.getYear() - 18, 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)))
                        .admissionDate(LocalDate.of(batch.getYear(), 7, 15))
                        .guardianName(FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)])
                        .address("Hostel Block " + (char) ('A' + rnd.nextInt(8)) + ", Campus Road")
                        .status(Student.Status.ACTIVE).department(batch.getDepartment()).batch(batch)
                        .build();
                allStudents.add(studentRepository.save(s));
                roll++;
            }
        }
        log.info("Students created: {}", allStudents.size());

        // Attendance: ~30 days history for the 3 CSE/ECE/MBA first batches (keeps seed time sane,
        // other batches accumulate data during use)
        log.info("Creating attendance history...");
        List<Student> sample = allStudents.stream().limit(3000).toList();
        List<Batch> sampleBatches = sample.stream().map(Student::getBatch).distinct().toList();
        for (Batch batch : sampleBatches) {
            List<Student> batchStudents = sample.stream().filter(s -> s.getBatch().equals(batch)).toList();
            if (batchStudents.isEmpty()) continue;
            List<CourseAssignment> batchAssignments = assignments.stream()
                    .filter(a -> a.getCourse().getDepartment().getId().equals(batch.getDepartment().getId()))
                    .toList();
            for (int day = 1; day <= 30; day++) {
                LocalDate date = LocalDate.now().minusDays(day);
                CourseAssignment ca = batchAssignments.get(day % batchAssignments.size());
                if (sessionRepository.existsByAssignmentIdAndClassDate(ca.getId(), date)) continue;
                AttendanceSession session = sessionRepository.save(AttendanceSession.builder()
                        .assignment(ca).batch(batch).classDate(date)
                        .takenBy(ca.getFaculty()).createdAt(Instant.now()).build());
                List<AttendanceRecord> records = new ArrayList<>(batchStudents.size());
                for (Student s : batchStudents) {
                    double p = rnd.nextDouble();
                    records.add(AttendanceRecord.builder()
                            .session(session).student(s).classDate(date)
                            .status(p < 0.85 ? AttendanceRecord.Status.PRESENT
                                    : p < 0.95 ? AttendanceRecord.Status.LATE : AttendanceRecord.Status.ABSENT)
                            .markedAt(Instant.now()).build());
                }
                recordRepository.saveAll(records);
            }
        }

        // Fees
        log.info("Creating fee structures...");
        FeeStructure fs = feeStructureRepository.save(FeeStructure.builder()
                .name("Semester Fee 2026-27").semester(semester)
                .totalAmount(new BigDecimal("65000"))
                .componentsJson("[{\"name\":\"Tuition\",\"amount\":50000},{\"name\":\"Library\",\"amount\":5000},{\"name\":\"Lab\",\"amount\":7000},{\"name\":\"Sports\",\"amount\":3000}]")
                .build());
        int assigned = 0;
        for (Student s : allStudents) {
            studentFeeRepository.save(StudentFee.builder()
                    .student(s).feeStructure(fs).amount(new BigDecimal("65000"))
                    .dueDate(LocalDate.now().plusDays(20))
                    .status(StudentFee.FeeStatus.PENDING).paidAmount(BigDecimal.ZERO).build());
            if (++assigned >= 500) break; // dues demo data for a subset
        }

        // Notices
        noticeRepository.save(Notice.builder().title("Welcome to CampusOS")
                .body("The new campus management system is live. Explore your dashboard.")
                .audience(Notice.Audience.ALL).createdBy(admin).build());
        noticeRepository.save(Notice.builder().title("Midterm exam schedule published")
                .body("Midterm exams begin next month. Check the Exams section for dates and rooms.")
                .audience(Notice.Audience.ROLE).targetRole(Role.STUDENT).createdBy(admin).build());
        noticeRepository.save(Notice.builder().title("CSE department seminar")
                .body("Industry seminar on distributed systems this Friday, 3 PM, Auditorium B.")
                .audience(Notice.Audience.DEPARTMENT).department(depts.get(0)).createdBy(admin).build());

        log.info("Seed completed in {} ms", System.currentTimeMillis() - start);
    }

    private User saveUser(String email, String password, Role role, String fullName) {
        return userRepository.save(User.builder()
                .email(email).passwordHash(passwordEncoder.encode(password))
                .role(role).fullName(fullName).active(true).build());
    }
}
