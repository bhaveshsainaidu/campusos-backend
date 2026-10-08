package com.campusos.fees;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    boolean existsByNameIgnoreCaseAndSemesterId(String name, Long semesterId);
}
