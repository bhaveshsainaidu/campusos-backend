package com.campusos;

import com.campusos.academics.Semester;
import com.campusos.academics.SemesterRepository;
import com.campusos.fees.dto.FeeDtos.*;
import com.campusos.security.JwtService;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private Long semesterId;

    @BeforeEach
    void setup() {
        User admin = userRepository.findByEmailIgnoreCase("feeadmin@campusos.edu")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("feeadmin@campusos.edu")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .fullName("Admin Fees")
                        .active(true).build()));

        adminToken = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");

        var sem = semesterRepository.save(Semester.builder().name("FeeSem" + System.nanoTime()).academicYear("2026-27")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusMonths(6)).active(true).build());
        semesterId = sem.getId();
    }

    @Test
    void testCreateFeeStructureAndDuesSummary() throws Exception {
        FeeStructureRequest req = new FeeStructureRequest(
                "Test Term Fee " + System.nanoTime(),
                semesterId,
                null,
                new BigDecimal("5000.00"),
                List.of(new FeeComponent("T1", new BigDecimal("5000.00")))
        );

        mockMvc.perform(post("/api/v1/fees/structures")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/fees/structures")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/fees/dues/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
