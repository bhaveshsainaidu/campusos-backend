package com.campusos.attendance;

import com.campusos.student.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_records")
@IdClass(AttendanceRecord.Pk.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    @Column(name = "class_date", nullable = false, insertable = false, updatable = false)
    private LocalDate classDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "marked_at")
    private Instant markedAt;

    public enum Status { PRESENT, ABSENT, LATE }

    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Pk implements java.io.Serializable {
        private Long id;
        private LocalDate classDate;
    }
}
