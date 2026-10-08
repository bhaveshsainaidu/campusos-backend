package com.campusos.notice;

import com.campusos.academics.Batch;
import com.campusos.academics.Department;
import com.campusos.user.Role;
import com.campusos.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Audience audience = Audience.ALL;

    /** Populated when audience = ROLE */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_role")
    private Role targetRole;

    /** Populated when audience = DEPARTMENT */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Populated when audience = BATCH */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    private Instant createdAt;

    public enum Audience { ALL, ROLE, DEPARTMENT, BATCH }
}
