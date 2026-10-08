package com.campusos.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    Optional<Batch> findByDepartmentIdAndNameIgnoreCase(Long departmentId, String name);
    Optional<Batch> findByNameIgnoreCase(String name);
}
