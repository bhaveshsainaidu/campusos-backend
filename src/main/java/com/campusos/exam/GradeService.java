package com.campusos.exam;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 10-point grading scale (UGC-style). */
@Service
public class GradeService {

    public record Grade(String letter, double points) {}

    public Grade compute(double percent) {
        if (percent >= 90) return new Grade("O", 10.0);
        if (percent >= 80) return new Grade("A+", 9.0);
        if (percent >= 70) return new Grade("A", 8.0);
        if (percent >= 60) return new Grade("B+", 7.0);
        if (percent >= 50) return new Grade("B", 6.0);
        if (percent >= 45) return new Grade("C", 5.0);
        if (percent >= 40) return new Grade("P", 4.0);
        return new Grade("F", 0.0);
    }

    public String letterFor(double percent) {
        return compute(percent).letter();
    }

    public BigDecimal pointsFor(double percent) {
        return BigDecimal.valueOf(compute(percent).points()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * SGPA = Σ(gradePoints × credits) / Σ(credits) over the semester's FINAL exam results.
     * CGPA = credit-weighted mean of all semesters' grade points.
     */
    public double computeGpa(java.util.List<ExamResultRepository.StudentResultRow> rows, Long semesterId) {
        double weighted = 0;
        int credits = 0;
        for (ExamResultRepository.StudentResultRow row : rows) {
            if (semesterId != null && (row.getSemesterId() == null || !row.getSemesterId().equals(semesterId))) continue;
            if (!"FINAL".equals(row.getExamType())) continue;
            if (row.getGradePoints() == null) continue;
            weighted += row.getGradePoints().doubleValue() * row.getCredits();
            credits += row.getCredits();
        }
        if (credits == 0) return 0.0;
        return Math.round((weighted / credits) * 100.0) / 100.0;
    }
}
