package com.campusos.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByRollNumberIgnoreCase(String rollNumber);

    boolean existsByRollNumberIgnoreCase(String rollNumber);

    // Single-join queries with distinct pagination-friendly results; filters hit indexed columns.
    @Query(value = """
        SELECT s FROM Student s
        LEFT JOIN FETCH s.department d
        LEFT JOIN FETCH s.batch b
        WHERE (:query IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:departmentId IS NULL OR s.department.id = :departmentId)
          AND (:batchId IS NULL OR s.batch.id = :batchId)
          AND (:status IS NULL OR s.status = :status)
        """,
        countQuery = """
        SELECT count(s) FROM Student s
        WHERE (:query IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:departmentId IS NULL OR s.department.id = :departmentId)
          AND (:batchId IS NULL OR s.batch.id = :batchId)
          AND (:status IS NULL OR s.status = :status)
        """)
    Page<Student> search(@Param("query") String query,
                         @Param("departmentId") Long departmentId,
                         @Param("batchId") Long batchId,
                         @Param("status") Student.Status status,
                         Pageable pageable);

    long countByDepartmentId(Long departmentId);

    long countByBatchId(Long batchId);

    @Query("SELECT d.code, d.name, COUNT(s.id) FROM Student s JOIN s.department d GROUP BY d.code, d.name")
    List<Object[]> perDepartmentCounts();

    @Query("""
        SELECT YEAR(s.admissionDate), MONTH(s.admissionDate), COUNT(s)
        FROM Student s
        WHERE s.admissionDate >= :since
        GROUP BY YEAR(s.admissionDate), MONTH(s.admissionDate)
        ORDER BY YEAR(s.admissionDate), MONTH(s.admissionDate)
        """)
    List<Object[]> admissionTrend(@Param("since") LocalDate since);

    @Query("SELECT COUNT(d) FROM Department d")
    long countDepartments();

    @Query("SELECT COUNT(c) FROM Course c")
    long countCourses();
}
