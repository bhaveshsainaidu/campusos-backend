package com.campusos.academics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {

    @Query("""
        SELECT DISTINCT a FROM CourseAssignment a
        JOIN FETCH a.course c
        JOIN FETCH a.semester s
        JOIN FETCH a.faculty f
        WHERE a.semester.id = :semesterId
        """)
    List<CourseAssignment> findBySemesterId(@Param("semesterId") Long semesterId);

    @Query("""
        SELECT DISTINCT a FROM CourseAssignment a
        JOIN FETCH a.course c
        JOIN FETCH a.semester s
        WHERE a.faculty.id = :facultyId
        """)
    List<CourseAssignment> findByFacultyId(@Param("facultyId") Long facultyId);

    boolean existsByCourseIdAndSemesterIdAndSection(Long courseId, Long semesterId, String section);
}
