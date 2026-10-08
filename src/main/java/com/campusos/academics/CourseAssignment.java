package com.campusos.academics;

import com.campusos.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @Column(nullable = false, length = 10)
    private String section = "A";
}
