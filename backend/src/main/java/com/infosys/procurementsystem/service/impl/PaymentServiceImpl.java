package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.dto.payment.SendPaymentDetailsRequest;
import com.infosys.procurementsystem.dto.payment.MarkPaymentRequest;
import com.infosys.procurementsystem.entity.Payment;
import com.infosys.procurementsystem.entity.PurchaseRequest;
import com.infosys.procurementsystem.entity.Supplier;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.PaymentStatus;
import com.infosys.procurementsystem.enums.RequestStatus;
import com.infosys.procurementsystem.exception.BadRequestException;
import com.infosys.procurementsystem.exception.ForbiddenException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.enums.NotificationType;
import com.infosys.procurementsystem.repository.PaymentRepository;
import com.infosys.procurementsystem.repository.PurchaseRequestRepository;
import com.infosys.procurementsystem.repository.SupplierRepository;
import com.infosys.procurementsystem.repository.UserRepository;
import com.infosys.procurementsystem.service.EmailService;
import com.infosys.procurementsystem.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    

    @Override
    @Transactional
    public Payment sendPaymentDetails(SendPaymentDetailsRequest dto) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(dto.getPurchaseRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase request not found with id: " + dto.getPurchaseRequestId()));

        if (purchaseRequest.getStatus() != RequestStatus.APPROVED) {
            throw new BadRequestException("Payment details can only be sent for an APPROVED purchase request");
        }

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + dto.getSupplierId()));

        Long expectedSupplierId = purchaseRequest.getItems().get(0).getProduct().getSupplier().getId();
        boolean mixedSuppliers = purchaseRequest.getItems().stream()
                .anyMatch(item -> !expectedSupplierId.equals(item.getProduct().getSupplier().getId()));
        if (mixedSuppliers || !expectedSupplierId.equals(supplier.getId())) {
            throw new BadRequestException("The supplier must match the supplier assigned to the requested product(s)");
        }

        Payment payment = paymentRepository.findByPurchaseRequestId(purchaseRequest.getId())
                .orElseGet(Payment::new);

        payment.setPurchaseRequest(purchaseRequest);
        payment.setSupplier(supplier);
        payment.setUser(purchaseRequest.getRequester());
        payment.setAmount(purchaseRequest.getTotalAmount());
        payment.setBankName(dto.getBankName());
        payment.setAccountNumber(dto.getAccountNumber());
        payment.setIfscCode(dto.getIfscCode());
        payment.setUpiId(dto.getUpiId() != null && !dto.getUpiId().isBlank() ? dto.getUpiId() : supplier.getUpiId());
        payment.setPaymentNotes(dto.getPaymentNotes());
        payment.setStatus(PaymentStatus.DETAILS_SENT);
        payment.setDetailsSentAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        emailService.sendPaymentDetailsEmailToRequester(saved);

        return saved;
    }

    @Override
    @Transactional
    public Payment markPaymentDone(Long purchaseRequestId, Long userId, MarkPaymentRequest dto) {
        Payment payment = paymentRepository.findByPurchaseRequestId(purchaseRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment record found for purchase request id: " + purchaseRequestId));

        if (!payment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the requester of this purchase request can mark the payment as done");
        }

        if (payment.getStatus() != PaymentStatus.DETAILS_SENT) {
            throw new BadRequestException("Payment can only be marked done after the supplier has sent payment details");
        }

        String method = dto != null && dto.getPaymentMethod() != null && !dto.getPaymentMethod().isBlank()
                ? dto.getPaymentMethod().trim().toUpperCase() : "UPI";
        if (!method.equals("UPI") && !method.equals("CARD")) {
            throw new BadRequestException("Payment method must be UPI or CARD");
        }
        if (method.equals("CARD")) {
            if (dto.getCardLast4() == null || !dto.getCardLast4().matches("\\d{4}")) {
                throw new BadRequestException("For card payments, only the last 4 digits are required");
            }
            payment.setCardLast4(dto.getCardLast4());
        }
        payment.setPaymentMethod(method);
        payment.setTransactionReference(dto != null ? dto.getTransactionReference() : null);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        Supplier supplier = payment.getSupplier();
        String title = "Payment Received - Request #" + payment.getPurchaseRequest().getId();
        String message = "The user " + payment.getUser().getFullName() + " has marked payment as done for purchase request #"
                + payment.getPurchaseRequest().getId() + " (" + payment.getPurchaseRequest().getTitle()
                + "), amount Rs. " + payment.getAmount() + ". You may proceed to prepare the shipment.";

        log.info("Notifying supplier {} that payment was completed for request #{}",
                supplier.getEmail(), payment.getPurchaseRequest().getId());
        emailService.sendNotificationEmail(supplier.getEmail(), supplier.getName(),
                NotificationType.PAYMENT_COMPLETED, title, message);

        return saved;
    }

    @Override
    public Payment getPaymentByPurchaseRequestId(Long purchaseRequestId) {
        return paymentRepository.findByPurchaseRequestId(purchaseRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment record found for purchase request id: " + purchaseRequestId));
    }

    @Override
    public List<Payment> getPaymentsForUser(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    @Override
    public List<Payment> getPaymentsForSupplier(Long supplierId) {
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        return paymentRepository.findBySupplierId(supplierId);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public byte[] exportPaymentsForUserAsCsv(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Payment> payments = paymentRepository.findByUserId(userId);
        return buildCsv(payments, false);
    }

    @Override
    public byte[] exportAllPaymentsAsCsv() {
        List<Payment> payments = paymentRepository.findAll();
        return buildCsv(payments, true);
    }

    @Override
    public byte[] exportPaymentsForSupplierAsCsv(Long supplierId) {
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
        return buildCsv(paymentRepository.findBySupplierId(supplierId), true);
    }

    private byte[] buildCsv(List<Payment> payments, boolean includeUserColumns) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {

            StringBuilder header = new StringBuilder();
            if (includeUserColumns) {
                header.append("User ID,User Name,User Email,");
            }
            header.append("Purchase Request ID,Title,Products,Supplier,Amount,Payment Status,")
                  .append("Bank Name,Account Number,IFSC Code,UPI ID,Payment Method,Card Last4,Transaction Reference,Details Sent At,Paid At");
            writer.println(header);

            for (Payment payment : payments) {
                PurchaseRequest pr = payment.getPurchaseRequest();
                StringBuilder row = new StringBuilder();

                if (includeUserColumns) {
                    row.append(csv(String.valueOf(payment.getUser().getId()))).append(",")
                       .append(csv(payment.getUser().getFullName())).append(",")
                       .append(csv(payment.getUser().getEmail())).append(",");
                }

                String products = pr.getItems().stream()
                        .map(item -> item.getProduct().getName() + " x" + item.getQuantity())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("");

                row.append(csv(String.valueOf(pr.getId()))).append(",")
                   .append(csv(pr.getTitle())).append(",")
                   .append(csv(products)).append(",")
                   .append(csv(payment.getSupplier() != null ? payment.getSupplier().getName() : "")).append(",")
                   .append(csv(String.valueOf(payment.getAmount()))).append(",")
                   .append(csv(payment.getStatus().name())).append(",")
                   .append(csv(payment.getBankName())).append(",")
                   .append(csv(payment.getAccountNumber())).append(",")
                   .append(csv(payment.getIfscCode())).append(",")
                   .append(csv(payment.getUpiId())).append(",")
                   .append(csv(payment.getPaymentMethod())).append(",")
                   .append(csv(payment.getCardLast4())).append(",")
                   .append(csv(payment.getTransactionReference())).append(",")
                   .append(csv(payment.getDetailsSentAt() != null ? payment.getDetailsSentAt().toString() : "")).append(",")
                   .append(csv(payment.getPaidAt() != null ? payment.getPaidAt().toString() : ""));

                writer.println(row);
            }
        }
        return out.toByteArray();
    }

    /**
     * Escapes a value for safe inclusion in a CSV cell.
     */
    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
