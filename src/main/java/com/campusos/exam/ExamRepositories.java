package com.campusos.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findByAssignmentId(Long assignmentId);

    boolean existsByAssignmentIdAndExamType(Long assignmentId, ExamSchedule.ExamType examType);

    interface ScheduleWithCourse {
        Long getId();
        ExamSchedule.ExamType getExamType();
        java.time.LocalDate getExamDate();
        java.time.LocalTime getStartTime();
        java.time.LocalTime getEndTime();
        String getRoom();
        int getMaxMarks();
        Long getAssignmentId();
        String getCourseCode();
        String getCourseTitle();
        String getFacultyName();
        String getSemesterName();
    }

    @Query(value = """
        SELECT es.id AS id, es.exam_type AS examType, es.exam_date AS examDate,
               es.start_time AS startTime, es.end_time AS endTime, es.room AS room,
               es.max_marks AS maxMarks, ca.id AS assignmentId,
               c.code AS courseCode, c.title AS courseTitle,
               u.full_name AS facultyName, CONCAT(s.name, ' ', s.academic_year) AS semesterName
        FROM exam_schedules es
        JOIN course_assignments ca ON ca.id = es.assignment_id
        JOIN courses c ON c.id = ca.course_id
        JOIN users u ON u.id = ca.faculty_id
        JOIN semesters s ON s.id = ca.semester_id
        WHERE (:semesterId IS NULL OR ca.semester_id = :semesterId)
        """, nativeQuery = true)
    List<ScheduleWithCourse> findSchedules(@Param("semesterId") Long semesterId);
}

interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findByExamScheduleId(Long examScheduleId);

    boolean existsByExamScheduleId(Long examScheduleId);

    java.util.Optional<ExamResult> findByExamScheduleIdAndStudentId(Long examScheduleId, Long studentId);

    /** All graded results for a student in one query (course + credits included, no N+1). */
    interface StudentResultRow {
        Long getCourseId();
        String getCourseCode();
        String getCourseTitle();
        int getCredits();
        Long getSemesterId();
        String getSemesterName();
        String getExamType();
        java.math.BigDecimal getMarksObtained();
        int getMaxMarks();
        String getGrade();
        java.math.BigDecimal getGradePoints();
    }

    @Query(value = """
        SELECT c.id AS courseId, c.code AS courseCode, c.title AS courseTitle, c.credits AS credits,
               s.id AS semesterId, CONCAT(s.name, ' ', s.academic_year) AS semesterName,
               es.exam_type AS examType, er.marks_obtained AS marksObtained, es.max_marks AS maxMarks,
               er.grade AS grade, er.grade_points AS gradePoints
        FROM exam_results er
        JOIN exam_schedules es ON es.id = er.exam_schedule_id
        JOIN course_assignments ca ON ca.id = es.assignment_id
        JOIN courses c ON c.id = ca.course_id
        JOIN semesters s ON s.id = ca.semester_id
        WHERE er.student_id = :studentId
        ORDER BY s.start_date, c.code
        """, nativeQuery = true)
    List<StudentResultRow> findStudentResults(@Param("studentId") Long studentId);
}
