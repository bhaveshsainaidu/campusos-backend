package com.campusos.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    boolean existsByAssignmentIdAndClassDate(Long assignmentId, LocalDate classDate);

    List<AttendanceSession> findByAssignmentIdOrderByClassDateDesc(Long assignmentId);
}
