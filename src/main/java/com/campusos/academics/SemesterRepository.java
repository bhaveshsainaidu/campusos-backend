package com.campusos.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {
    Optional<Semester> findByNameAndAcademicYear(String name, String academicYear);
    Optional<Semester> findByActiveTrue();
}
