package com.campusos.fees;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    boolean existsByNameIgnoreCaseAndSemesterId(String name, Long semesterId);
}

interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {

    List<StudentFee> findByStudentId(Long studentId);

    Page<StudentFee> findByStatus(StudentFee.FeeStatus status, Pageable pageable);

    Optional<StudentFee> findByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

    @Query("""
        SELECT f FROM StudentFee f
        JOIN FETCH f.student st
        JOIN FETCH f.feeStructure fs
        WHERE (:status IS NULL OR f.status = :status)
          AND (:departmentId IS NULL OR st.department.id = :departmentId)
        """)
    List<StudentFee> findDues(@Param("status") StudentFee.FeeStatus status,
                              @Param("departmentId") Long departmentId);
}

interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    List<Payment> findByStudentFeeId(Long studentFeeId);

    List<Payment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
