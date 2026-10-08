package com.campusos.fees;

import com.campusos.academics.Department;
import com.campusos.academics.Semester;
import com.campusos.student.Student;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fee_structures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class FeeStructure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** JSON array of components, e.g. [{"name":"Tuition","amount":50000},{"name":"Library","amount":2000}] */
    @Column(name = "components_json", columnDefinition = "TEXT")
    private String componentsJson;
}

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_fee_id", nullable = false)
    private StudentFee studentFee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "receipt_no", nullable = false, unique = true, length = 50)
    private String receiptNo;

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    public enum PaymentStatus { CREATED, PAID, FAILED }
}
