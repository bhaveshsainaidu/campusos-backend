package com.campusos.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySessionId(Long sessionId);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    @Query(value = "SELECT COUNT(*) FROM attendance_sessions WHERE assignment_id = :assignmentId", nativeQuery = true)
    long countSessionsForAssignment(@Param("assignmentId") Long assignmentId);

    @Query(value = """
        SELECT r.class_date,
               SUM(CASE WHEN r.status IN ('PRESENT','LATE') THEN 1 ELSE 0 END),
               COUNT(*)
        FROM attendance_records r
        WHERE r.class_date >= :since
        GROUP BY r.class_date
        ORDER BY r.class_date
        """, nativeQuery = true)
    List<Object[]> dailyTrend(@Param("since") LocalDate since);

    /** Per-course attendance percentage for one student (single grouped query, no N+1). */
    @Query("""
        SELECT new com.campusos.attendance.CourseAttendanceStat(
            a.course.id, a.course.code, a.course.title,
            SUM(CASE WHEN r.status IN (AttendanceRecord.Status.PRESENT, AttendanceRecord.Status.LATE) THEN 1 ELSE 0 END),
            COUNT(r))
        FROM AttendanceRecord r
        JOIN r.session s
        JOIN s.assignment a
        WHERE r.student.id = :studentId
        GROUP BY a.course.id, a.course.code, a.course.title
        """)
    List<CourseAttendanceStat> statsPerCourse(@Param("studentId") Long studentId);

    /** Overall counts for one student. */
    @Query("""
        SELECT new com.campusos.attendance.OverallAttendanceStat(
            SUM(CASE WHEN r.status IN (AttendanceRecord.Status.PRESENT, AttendanceRecord.Status.LATE) THEN 1 ELSE 0 END),
            COUNT(r))
        FROM AttendanceRecord r
        WHERE r.student.id = :studentId
        """)
    OverallAttendanceStat statsOverall(@Param("studentId") Long studentId);

    /** Overall counts for every student of a batch in one grouped query (shortage scan, no N+1). */
    @Query("""
        SELECT new com.campusos.attendance.StudentAttendanceStat(
            st.id, st.rollNumber, st.name,
            SUM(CASE WHEN r.status IN (AttendanceRecord.Status.PRESENT, AttendanceRecord.Status.LATE) THEN 1 ELSE 0 END),
            COUNT(r))
        FROM AttendanceRecord r
        JOIN r.student st
        WHERE st.batch.id = :batchId
        GROUP BY st.id, st.rollNumber, st.name
        HAVING COUNT(r) > 0
        """)
    List<StudentAttendanceStat> statsPerStudentInBatch(@Param("batchId") Long batchId);
}
