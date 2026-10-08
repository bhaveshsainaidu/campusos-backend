package com.campusos.attendance;

import com.campusos.academics.Batch;
import com.campusos.academics.CourseAssignment;
import com.campusos.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CourseAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taken_by", nullable = false)
    private User takenBy;

    @Column(name = "created_at")
    private Instant createdAt;
}
