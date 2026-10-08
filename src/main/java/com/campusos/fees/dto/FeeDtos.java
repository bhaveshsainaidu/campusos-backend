package com.campusos.fees.dto;

import com.campusos.fees.StudentFee;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FeeDtos {
    private FeeDtos() {}

    public record FeeComponent(@NotBlank String name, @NotNull BigDecimal amount) {}

    public record FeeStructureRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull Long semesterId,
            Long departmentId,
            @NotNull @DecimalMin("0") BigDecimal totalAmount,
            @NotEmpty List<FeeComponent> components) {}

    public record FeeStructureResponse(Long id, String name, Long semesterId, String semesterName,
                                       Long departmentId, BigDecimal totalAmount,
                                       List<FeeComponent> components) {}

    public record AssignRequest(@NotNull Long feeStructureId, @NotNull LocalDate dueDate) {}

    public record AssignResult(int assigned, int skipped) {}

    public record PaymentCreateResponse(Long paymentId, BigDecimal amount, String receiptNo,
                                        String razorpayOrderId, boolean live, String checkoutMode) {}

    public record StudentFeeResponse(Long id, Long studentId, String rollNumber, String studentName,
                                     Long feeStructureId, String feeName, BigDecimal amount,
                                     BigDecimal paidAmount, BigDecimal dueAmount, LocalDate dueDate,
                                     StudentFee.FeeStatus status) {}

    public record PaymentCreateRequest(@NotNull Long studentFeeId,
                                       @DecimalMin("1") BigDecimal amount) {}

    public record PaymentVerifyRequest(@NotNull Long paymentId,
                                       @NotBlank String razorpayPaymentId,
                                       @NotBlank String razorpaySignature) {}

    public record PaymentResponse(Long id, Long studentFeeId, BigDecimal amount, String receiptNo,
                                  String razorpayOrderId, String razorpayPaymentId,
                                  String status, Instant createdAt, Instant paidAt) {}

    public record ReceiptDetail(String receiptNo, String studentName, String rollNumber,
                                String feeName, BigDecimal amount, Instant paidAt,
                                List<FeeComponent> components, String paymentMode) {}

    public record DuesSummary(long studentsWithDues, java.math.BigDecimal totalOutstanding) {}
}
