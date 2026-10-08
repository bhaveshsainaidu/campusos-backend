package com.campusos.fees;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    List<Payment> findByStudentFeeId(Long studentFeeId);

    List<Payment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
