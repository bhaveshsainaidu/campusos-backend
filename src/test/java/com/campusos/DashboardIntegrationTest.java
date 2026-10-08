package com.campusos;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setup() {
        User admin = userRepository.findByEmailIgnoreCase("dashadmin@campusos.edu")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("dashadmin@campusos.edu")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .fullName("Admin Dashboard")
                        .active(true).build()));

        adminToken = jwtService.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");
    }

    @Test
    void testAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis").exists())
                .andExpect(jsonPath("$.attendanceTrend").isArray());
    }
}
