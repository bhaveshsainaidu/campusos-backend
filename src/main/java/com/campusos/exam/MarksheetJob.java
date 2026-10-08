package com.campusos.exam;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "marksheet_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarksheetJob {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private com.campusos.student.Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(length = 500)
    private String error;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum Status { PENDING, COMPLETED, FAILED }
}
