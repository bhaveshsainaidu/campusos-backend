package com.campusos.security;

import com.campusos.common.ApiException;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class RefreshToken {
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
    private boolean revoked = false;

    @Column(name = "created_at")
    private Instant createdAt;
}

interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(Long userId);
}

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final UserRepository userRepository;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repo, UserRepository userRepository,
                               @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-token-days:14}") int refreshDays) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.ttl = Duration.ofDays(refreshDays);
    }

    @Transactional
    public String issue(Long userId) {
        User user = userRepository.getReferenceById(userId);
        String token = java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "");
        repo.save(RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plus(ttl))
                .build());
        return token;
    }

    /** Validates and rotates the refresh token. Returns the owning user. */
    @Transactional
    public User rotate(String token) {
        RefreshToken rt = repo.findByToken(token)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }
        rt.setRevoked(true);
        return rt.getUser();
    }

    @Transactional
    public void revokeAll(Long userId) {
        repo.deleteByUserId(userId);
    }
}
