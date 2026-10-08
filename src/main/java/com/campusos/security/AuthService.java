package com.campusos.security;

import com.campusos.common.ApiException;
import com.campusos.security.dto.AuthDtos.*;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}

interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(Long userId);
}

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository resetRepo;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!user.isActive()) {
            throw ApiException.unauthorized("Account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refresh = refreshTokenService.issue(user.getId());
        return new TokenResponse(access, refresh, user.getRole().name(), user.getFullName(), user.getId());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        User user = refreshTokenService.rotate(request.refreshToken());
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refresh = refreshTokenService.issue(user.getId());
        return new TokenResponse(access, refresh, user.getRole().name(), user.getFullName(), user.getId());
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenService.revokeAll(userId);
    }

    /** Always returns silently — never reveals whether the email exists. */
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            resetRepo.deleteByUserId(user.getId());
            String token = HexFormat.of().formatHex(secureRandom(32));
            resetRepo.save(PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(Instant.now().plus(java.time.Duration.ofMinutes(30)))
                    .build());
            // In production this token would be emailed. For the demo it is logged.
            log.info("Password reset token for {}: {}", user.getEmail(), token);
        });
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirm request) {
        PasswordResetToken prt = resetRepo.findByToken(request.token())
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));
        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("Invalid or expired reset token");
        }
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        prt.setUsed(true);
        refreshTokenService.revokeAll(user.getId());
    }

    @Transactional(readOnly = true)
    public MeResponse me(AuthPrincipal principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return new MeResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getFullName());
    }

    private byte[] secureRandom(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return buf;
    }
}
