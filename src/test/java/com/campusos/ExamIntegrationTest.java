package com.campusos;

import com.campusos.academics.*;
import com.campusos.exam.ExamSchedule;
import com.campusos.exam.dto.ExamDtos.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ExamIntegrationTest {

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
    private Long studentId;
    private String adminToken;

    @BeforeEach
    void setup() {
        User admin = userRepository.findByEmailIgnoreCase("examadmin@campusos.edu")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("examadmin@campusos.edu")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .fullName("Admin Exams")
                        .active(true).build()));

        adminToken = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

        var dept = departmentRepository.save(Department.builder().code("EXAM" + System.nanoTime()).name("Exam Dept").build());
        var batch = batchRepository.save(Batch.builder().department(dept).name("EXAM-2026").year(2026).build());
        var sem = semesterRepository.save(Semester.builder().name("ExamSem" + System.nanoTime()).academicYear("2026-27")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusMonths(4)).active(true).build());
        var course = courseRepository.save(Course.builder().code("EX" + System.nanoTime()).title("Exam Course").credits(3).semesterNum(1).department(dept).build());

        var fac = userRepository.save(User.builder().email("facexam" + System.nanoTime() + "@campusos.edu")
                .passwordHash(passwordEncoder.encode("Faculty@123"))
                .role(Role.FACULTY).fullName("Prof Exam").active(true).build());
        var ca = assignmentRepository.save(CourseAssignment.builder().course(course).semester(sem).faculty(fac).section("A").build());
        assignmentId = ca.getId();

        var stuUser = userRepository.save(User.builder().email("stuexam" + System.nanoTime() + "@campusos.edu")
                .passwordHash(passwordEncoder.encode("Student@123"))
                .role(Role.STUDENT).fullName("Student Exam").active(true).build());
        var stu = studentRepository.save(Student.builder().user(stuUser).rollNumber("EXAMSTU" + System.nanoTime()).name("Student Exam")
                .email(stuUser.getEmail()).admissionDate(LocalDate.now()).status(Student.Status.ACTIVE)
                .department(dept).batch(batch).build());
        studentId = stu.getId();
    }

    @Test
    void testScheduleExamAndEnterMarks() throws Exception {
        ScheduleRequest req = new ScheduleRequest(
                assignmentId,
                ExamSchedule.ExamType.MIDTERM,
                LocalDate.now().plusDays(10),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Hall-101",
                100
        );

        var schedRes = mockMvc.perform(post("/api/v1/exams/schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room").value("Hall-101"))
                .andReturn().getResponse().getContentAsString();

        Long schedId = objectMapper.readTree(schedRes).get("id").asLong();

        MarksEntryRequest marksReq = new MarksEntryRequest(
                schedId,
                List.of(new MarkEntry(studentId, new BigDecimal("85.00")))
        );

        mockMvc.perform(post("/api/v1/exams/marks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(marksReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(1));

        mockMvc.perform(get("/api/v1/exams/results/students/" + studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].grade").value("A+"));
    }
}
