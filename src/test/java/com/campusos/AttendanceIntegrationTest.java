package com.campusos;

import com.campusos.academics.*;
import com.campusos.attendance.*;
import com.campusos.attendance.dto.AttendanceDtos.*;
import com.campusos.security.JwtService;
import com.campusos.student.Student;
import com.campusos.student.StudentRepository;
import com.campusos.user.Role;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AttendanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseAssignmentRepository assignmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Long assignmentId;
    private Long batchId;
    private Long studentId;
    private String adminToken;

    @BeforeEach
    void setup() {
        User admin = userRepository.findByEmailIgnoreCase("attadmin@campusos.edu")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("attadmin@campusos.edu")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .fullName("Admin Attendance")
                        .active(true).build()));

        adminToken = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

        var dept = departmentRepository.save(Department.builder().code("TESTCSE").name("Computer Science").build());
        var batch = batchRepository.save(Batch.builder().department(dept).name("TESTCSE-2026").year(2026).build());
        batchId = batch.getId();

        var sem = semesterRepository.save(Semester.builder().name("TestFall").academicYear("2026-27")
                .startDate(LocalDate.now().minusDays(10)).endDate(LocalDate.now().plusDays(60)).active(true).build());

        var course = courseRepository.save(Course.builder().code("TEST101").title("Test CS").credits(4).semesterNum(1).department(dept).build());

        var fac = userRepository.save(User.builder().email("facatt" + System.nanoTime() + "@campusos.edu")
                .passwordHash(passwordEncoder.encode("Faculty@123"))
                .role(Role.FACULTY).fullName("Prof Attendance").active(true).build());

        var ca = assignmentRepository.save(CourseAssignment.builder().course(course).semester(sem).faculty(fac).section("A").build());
        assignmentId = ca.getId();

        var stuUser = userRepository.save(User.builder().email("stuatt" + System.nanoTime() + "@campusos.edu")
                .passwordHash(passwordEncoder.encode("Student@123"))
                .role(Role.STUDENT).fullName("Student Attendance").active(true).build());

        var stu = studentRepository.save(Student.builder().user(stuUser).rollNumber("TESTATT" + System.nanoTime()).name("Student Attendance")
                .email(stuUser.getEmail()).admissionDate(LocalDate.now()).status(Student.Status.ACTIVE)
                .department(dept).batch(batch).build());
        studentId = stu.getId();
    }

    @Test
    void testMarkAttendanceSessionAndGetThreshold() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/threshold")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimumPercent").value(75.0));

        MarkSessionRequest req = new MarkSessionRequest(
                assignmentId, batchId, LocalDate.now(),
                List.of(new MarkRecord(studentId, AttendanceRecord.Status.PRESENT))
        );

        mockMvc.perform(post("/api/v1/attendance/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("TEST101"));

        mockMvc.perform(get("/api/v1/attendance/shortage?batchId=" + batchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
