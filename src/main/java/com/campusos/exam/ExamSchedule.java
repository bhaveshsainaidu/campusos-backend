package com.campusos.exam;

import com.campusos.academics.CourseAssignment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "exam_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CourseAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private ExamType examType;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 50)
    private String room;

    @Column(name = "max_marks", nullable = false)
    private int maxMarks = 100;

    public enum ExamType { MIDTERM, FINAL, QUIZ, ASSIGNMENT }
}
