package com.campusos.fees;

import com.campusos.common.PageResponse;
import com.campusos.fees.dto.FeeDtos.*;
import com.campusos.security.AuthPrincipal;
import com.campusos.student.Student;
import com.campusos.student.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
@Tag(name = "Fees")
public class FeeController {

    private final FeeService feeService;
    private final StudentService studentService;

    // ---- Admin ----
    @GetMapping("/structures")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "List fee structures")
    public List<FeeStructureResponse> structures() {
        return feeService.listStructures();
    }

    @PostMapping("/structures")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a fee structure with components")
    public FeeStructureResponse createStructure(@Valid @RequestBody FeeStructureRequest request) {
        return feeService.createStructure(request);
    }

    @PostMapping("/structures/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a fee structure to matching students (due date = today + 30 days if omitted)")
    public AssignResult assign(@PathVariable Long id,
                               @RequestParam(required = false) java.time.LocalDate dueDate) {
        return feeService.assignToStudents(new AssignRequest(id,
                dueDate != null ? dueDate : java.time.LocalDate.now().plusDays(30)));
    }

    @GetMapping("/dues")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Outstanding dues (paginated), optional status filter")
    public PageResponse<StudentFeeResponse> dues(@RequestParam(required = false) StudentFee.FeeStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Page<StudentFeeResponse> p = feeService.dues(status, page, size);
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(), p.isLast());
    }

    @GetMapping("/dues/summary")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Dues summary KPIs")
    public DuesSummary duesSummary() {
        return feeService.duesSummary();
    }

    // ---- Student ----
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Current student's fee records")
    public List<StudentFeeResponse> myFees(@AuthenticationPrincipal AuthPrincipal principal) {
        Student s = studentService.getByUserId(principal.id());
        return feeService.studentFees(s.getId());
    }

    @PostMapping("/payments")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create a payment order (Razorpay / local sandbox)")
    public PaymentCreateResponse createPayment(@Valid @RequestBody PaymentCreateRequest request,
                                               @AuthenticationPrincipal AuthPrincipal principal) {
        Student s = studentService.getByUserId(principal.id());
        return feeService.createPayment(s.getId(), request);
    }

    @PostMapping("/payments/verify")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Verify a payment and record the receipt")
    public PaymentResponse verifyPayment(@Valid @RequestBody PaymentVerifyRequest request,
                                         @AuthenticationPrincipal AuthPrincipal principal) {
        Student s = studentService.getByUserId(principal.id());
        return feeService.verifyPayment(s.getId(), request);
    }

    @GetMapping("/payments/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Current student's payment history")
    public List<PaymentResponse> myPayments(@AuthenticationPrincipal AuthPrincipal principal) {
        Student s = studentService.getByUserId(principal.id());
        return feeService.myPayments(s.getId());
    }

    @GetMapping("/receipts/{receiptNo}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Receipt detail for a payment")
    public ReceiptDetail receipt(@PathVariable String receiptNo,
                                 @AuthenticationPrincipal AuthPrincipal principal) {
        Student s = studentService.getByUserId(principal.id());
        return feeService.receipt(s.getId(), receiptNo);
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @Operation(summary = "Payments for a fee record (admin view)")
    public List<PaymentResponse> payments(@RequestParam Long studentFeeId) {
        return feeService.paymentsForFee(studentFeeId);
    }

    @GetMapping("/payments/sandbox-payment-id")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Local sandbox helper: generates a fake payment id for the verify step")
    public Map<String, String> sandboxPaymentId() {
        return Map.of("razorpayPaymentId", feeService.sandboxPaymentId());
    }
}
