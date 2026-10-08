package com.campusos.fees;

import com.campusos.academics.DepartmentRepository;
import com.campusos.academics.Semester;
import com.campusos.academics.SemesterRepository;
import com.campusos.common.ApiException;
import com.campusos.fees.dto.FeeDtos.*;
import com.campusos.student.Student;
import com.campusos.student.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeService {

    private final FeeStructureRepository structureRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final PaymentRepository paymentRepository;
    private final SemesterRepository semesterRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final RazorpayService razorpayService;
    private final ObjectMapper objectMapper;

    // ---------- Fee structures ----------
    @Transactional(readOnly = true)
    public List<FeeStructureResponse> listStructures() {
        return structureRepository.findAll(Sort.by("id").descending()).stream().map(this::toStructureResponse).toList();
    }

    @Transactional
    public FeeStructureResponse createStructure(FeeStructureRequest req) {
        if (req.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Total amount must be positive");
        }
        BigDecimal componentSum = req.components().stream()
                .map(FeeComponent::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (componentSum.compareTo(req.totalAmount()) != 0) {
            throw ApiException.validation("Component amounts (" + componentSum + ") must add up to the total ("
                    + req.totalAmount() + ")");
        }
        Semester semester = semesterRepository.findById(req.semesterId())
                .orElseThrow(() -> ApiException.badRequest("Unknown semester"));
        try {
            FeeStructure structure = FeeStructure.builder()
                    .name(req.name()).semester(semester)
                    .department(req.departmentId() != null
                            ? departmentRepository.getReferenceById(req.departmentId()) : null)
                    .totalAmount(req.totalAmount())
                    .componentsJson(objectMapper.writeValueAsString(req.components()))
                .build();
            return toStructureResponse(structureRepository.save(structure));
        } catch (Exception ex) {
            throw ApiException.badRequest("Could not serialize components");
        }
    }

    private FeeStructureResponse toStructureResponse(FeeStructure f) {
        List<FeeComponent> components = List.of();
        try {
            if (f.getComponentsJson() != null) {
                components = objectMapper.readValue(f.getComponentsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, FeeComponent.class));
            }
        } catch (Exception ignored) {}
        return new FeeStructureResponse(f.getId(), f.getName(), f.getSemester().getId(),
                f.getSemester().getName() + " " + f.getSemester().getAcademicYear(),
                f.getDepartment() != null ? f.getDepartment().getId() : null,
                f.getTotalAmount(), components);
    }

    // ---------- Assignment ----------
    @Transactional
    public AssignResult assignToStudents(AssignRequest req) {
        FeeStructure structure = structureRepository.findById(req.feeStructureId())
                .orElseThrow(() -> ApiException.notFound("Fee structure not found"));
        int assigned = 0, skipped = 0;
        java.util.Set<Long> existingStudentIds = studentFeeRepository.findStudentIdsByFeeStructureId(structure.getId());
        var students = studentRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();
        List<StudentFee> toSave = new ArrayList<>();
        for (Student student : students) {
            boolean matches = structure.getDepartment() == null
                    || (student.getDepartment() != null
                        && student.getDepartment().getId().equals(structure.getDepartment().getId()));
            if (!matches || existingStudentIds.contains(student.getId())) {
                skipped++;
                continue;
            }
            toSave.add(StudentFee.builder()
                    .student(student).feeStructure(structure)
                    .amount(structure.getTotalAmount()).dueDate(req.dueDate())
                    .status(StudentFee.FeeStatus.PENDING).paidAmount(BigDecimal.ZERO).build());
            assigned++;
        }
        if (!toSave.isEmpty()) {
            studentFeeRepository.saveAll(toSave);
        }
        log.info("Fee structure {} assigned: {} assigned, {} skipped", structure.getId(), assigned, skipped);
        return new AssignResult(assigned, skipped);
    }

    // ---------- Student fee view ----------
    @Transactional(readOnly = true)
    public List<StudentFeeResponse> studentFees(Long studentId) {
        return studentFeeRepository.findByStudentId(studentId).stream().map(this::toStudentFeeResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<StudentFeeResponse> dues(StudentFee.FeeStatus status, int page, int size) {
        Page<StudentFee> p = studentFeeRepository.findByStatus(status,
                PageRequest.of(Math.max(page, 0), Math.min(size, 200), Sort.by("dueDate").ascending()));
        return p.map(this::toStudentFeeResponse);
    }

    private StudentFeeResponse toStudentFeeResponse(StudentFee f) {
        Student s = f.getStudent();
        return new StudentFeeResponse(f.getId(), s.getId(), s.getRollNumber(), s.getName(),
                f.getFeeStructure().getId(), f.getFeeStructure().getName(),
                f.getAmount(), f.getPaidAmount(), f.getAmount().subtract(f.getPaidAmount()),
                f.getDueDate(), f.getStatus());
    }

    // ---------- Payments ----------
    @Transactional
    public PaymentCreateResponse createPayment(Long studentId, PaymentCreateRequest req) {
        StudentFee fee = studentFeeRepository.findById(req.studentFeeId())
                .orElseThrow(() -> ApiException.notFound("Fee record not found"));
        if (!fee.getStudent().getId().equals(studentId)) {
            throw ApiException.forbidden("You can only pay your own fees");
        }
        if (fee.getStatus() == StudentFee.FeeStatus.PAID) {
            throw ApiException.conflict("This fee is already fully paid");
        }
        BigDecimal amount = req.amount() == null ? fee.getAmount().subtract(fee.getPaidAmount()) : req.amount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.compareTo(fee.getAmount().subtract(fee.getPaidAmount())) > 0) {
            throw ApiException.badRequest("Invalid payment amount");
        }
        String receiptNo = futureReceiptNo();
        var order = razorpayService.createOrder(receiptNo, amount);
        Payment payment = paymentRepository.save(Payment.builder()
                .studentFee(fee).student(fee.getStudent()).amount(amount)
                .receiptNo(receiptNo).razorpayOrderId(order.orderId())
                .status(Payment.PaymentStatus.CREATED).createdAt(Instant.now()).build());
        return new PaymentCreateResponse(payment.getId(), amount, receiptNo,
                order.orderId(), razorpayService.isLive(),
                razorpayService.isLive() ? "rzp_test_live_checkout" : "local_sandbox");
    }

    /** Verifies the gateway signature, records the payment and updates fee status. */
    @Transactional
    public PaymentResponse verifyPayment(Long studentId, PaymentVerifyRequest req) {
        Payment payment = paymentRepository.findById(req.paymentId())
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (!payment.getStudent().getId().equals(studentId)) {
            throw ApiException.forbidden("Not your payment");
        }
        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw ApiException.conflict("Payment already verified");
        }
        boolean valid = razorpayService.verifySignature(payment.getRazorpayOrderId(),
                req.razorpayPaymentId(), req.razorpaySignature());
        if (!valid) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw ApiException.badRequest("Payment signature verification failed");
        }
        payment.setRazorpayPaymentId(req.razorpayPaymentId());
        payment.setRazorpaySignature(req.razorpaySignature());
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        StudentFee fee = payment.getStudentFee();
        fee.setPaidAmount(fee.getPaidAmount().add(payment.getAmount()));
        fee.setStatus(fee.getPaidAmount().compareTo(fee.getAmount()) >= 0
                ? StudentFee.FeeStatus.PAID : StudentFee.FeeStatus.PARTIAL);
        studentFeeRepository.save(fee);
        return toPaymentResponse(payment);
    }

    /** Local-sandbox helper so the E2E flow can complete payments without Razorpay keys. */
    public String sandboxPaymentId() {
        return "pay_local_" + Long.toHexString(System.currentTimeMillis());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> paymentsForFee(Long studentFeeId) {
        return paymentRepository.findByStudentFeeId(studentFeeId).stream().map(this::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> myPayments(Long studentId) {
        return paymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::toPaymentResponse).toList();
    }

    private PaymentResponse toPaymentResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getStudentFee().getId(), p.getAmount(), p.getReceiptNo(),
                p.getRazorpayOrderId(), p.getRazorpayPaymentId(), p.getStatus().name(),
                p.getCreatedAt(), p.getPaidAt());
    }

    // ---------- Receipt ----------
    @Transactional(readOnly = true)
    public ReceiptDetail receipt(Long studentId, String receiptNo) {
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> p.getReceiptNo().equalsIgnoreCase(receiptNo))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Receipt not found"));
        if (!payment.getStudent().getId().equals(studentId)) {
            throw ApiException.forbidden("Not your receipt");
        }
        FeeStructureResponse structure = toStructureResponse(payment.getStudentFee().getFeeStructure());
        return new ReceiptDetail(payment.getReceiptNo(), payment.getStudent().getName(),
                payment.getStudent().getRollNumber(), structure.name(), payment.getAmount(),
                payment.getPaidAt(), structure.components(),
                payment.getRazorpayPaymentId() != null && !payment.getRazorpayPaymentId().startsWith("pay_local")
                        ? "Razorpay" : "Sandbox");
    }

    // ---------- Dues summary ----------
    @Transactional(readOnly = true)
    public DuesSummary duesSummary() {
        List<StudentFee> all = studentFeeRepository.findDues(null, null);
        long withDues = all.stream().filter(f -> f.getStatus() != StudentFee.FeeStatus.PAID).count();
        BigDecimal outstanding = all.stream()
                .filter(f -> f.getStatus() != StudentFee.FeeStatus.PAID)
                .map(f -> f.getAmount().subtract(f.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DuesSummary(withDues, outstanding);
    }

    private String futureReceiptNo() {
        return "RCP-" + LocalDate.now().toString().replace("-", "")
                + "-" + Long.toHexString(System.currentTimeMillis() & 0xFFFFFFL).toUpperCase();
    }
}
