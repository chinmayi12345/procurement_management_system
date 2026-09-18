package com.infosys.procurementsystem.controller;

import com.infosys.procurementsystem.common.ApiResponse;
import com.infosys.procurementsystem.dto.payment.SendPaymentDetailsRequest;
import com.infosys.procurementsystem.dto.payment.MarkPaymentRequest;
import com.infosys.procurementsystem.entity.Payment;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.Role;
import com.infosys.procurementsystem.exception.ForbiddenException;
import com.infosys.procurementsystem.service.PaymentService;
import com.infosys.procurementsystem.repository.ShipmentRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ShipmentRepository shipmentRepository;

    /**
     * Called by the supplier (no supplier login in this system) once a
     * request has been approved, to share their bank/payment details with
     * the requester.
     */
    @PostMapping("/supplier/send-details")
    public ResponseEntity<ApiResponse<Payment>> sendPaymentDetails(@Valid @RequestBody SendPaymentDetailsRequest requestDto) {
        Payment payment = paymentService.sendPaymentDetails(requestDto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment details sent to the requester", payment));
    }

    /**
     * The "Payment Done" button for the requesting user.
     */
    @PostMapping("/{purchaseRequestId}/mark-paid")
    public ResponseEntity<ApiResponse<Payment>> markPaymentDone(
            @PathVariable Long purchaseRequestId,
            @RequestBody(required = false) MarkPaymentRequest request,
            @AuthenticationPrincipal User currentUser) {
        Payment payment = paymentService.markPaymentDone(
                purchaseRequestId, currentUser.getId(), request != null ? request : new MarkPaymentRequest());
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment marked as done and supplier notified", payment));
    }

    @GetMapping("/{purchaseRequestId}")
    public ResponseEntity<ApiResponse<Payment>> getPayment(@PathVariable Long purchaseRequestId) {
        Payment payment = paymentService.getPaymentByPurchaseRequestId(purchaseRequestId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment retrieved successfully", payment));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Payment>>> getMyPayments(@AuthenticationPrincipal User currentUser) {
        List<Payment> payments = paymentService.getPaymentsForUser(currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Your payments retrieved successfully", payments));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments(@AuthenticationPrincipal User currentUser) {
        requireAdmin(currentUser);
        List<Payment> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(new ApiResponse<>(true, "All payments retrieved successfully", payments));
    }

    /**
     * CSV of the logged-in user's own product + payment details.
     */
    @GetMapping("/export/my")
    public ResponseEntity<byte[]> exportMyPaymentsCsv(@AuthenticationPrincipal User currentUser) {
        byte[] csv = paymentService.exportPaymentsForUserAsCsv(currentUser.getId());
        return csvResponse(csv, "my-payments.csv");
    }

    /**
     * CSV of every user's product + payment details, for admins.
     */
    @GetMapping("/export/all")
    public ResponseEntity<byte[]> exportAllPaymentsCsv(@AuthenticationPrincipal User currentUser) {
        requireAdmin(currentUser);
        byte[] csv = paymentService.exportAllPaymentsAsCsv();
        return csvResponse(csv, "all-payments.csv");
    }


    /**
     * Supplier-specific payment history CSV. Supplier accounts are intentionally
     * not part of the login workflow, so the supplier ID is selected in the
     * supplier console and the backend filters strictly by that supplier.
     */
    /**
     * Supplier waiting list: PAID requests belonging to the selected supplier
     * that are not yet delivered. A user appears here only after payment is
     * completed, so the supplier knows exactly which paid orders are waiting
     * to be shipped/delivered.
     */
    @GetMapping("/supplier/{supplierId}/tracking-status")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSupplierTrackingStatus(@PathVariable Long supplierId) {
        List<Payment> payments = paymentService.getPaymentsForSupplier(supplierId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Payment payment : payments) {
            if (payment.getStatus() == null || !"PAID".equals(payment.getStatus().name())) continue;
            Long requestId = payment.getPurchaseRequest() != null ? payment.getPurchaseRequest().getId() : null;
            if (requestId == null) continue;

            var shipmentOpt = shipmentRepository.findByPurchaseRequestId(requestId);
            // Supplier tracking status is a pending-delivery view only.
            // Once an order is delivered, remove it from this response.
            if (shipmentOpt.isPresent() && shipmentOpt.get().getStatus() != null
                    && "DELIVERED".equals(shipmentOpt.get().getStatus().name())) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("purchaseRequestId", requestId);
            row.put("userName", payment.getUser() != null ? payment.getUser().getFullName() : "");
            row.put("userEmail", payment.getUser() != null ? payment.getUser().getEmail() : "");
            row.put("title", payment.getPurchaseRequest().getTitle());
            row.put("amount", payment.getAmount());
            row.put("paymentStatus", payment.getStatus().name());
            row.put("paidAt", payment.getPaidAt());
            row.put("paymentMethod", payment.getPaymentMethod());
            row.put("transactionReference", payment.getTransactionReference());
            row.put("shipmentStatus", shipmentOpt.map(s -> s.getStatus().name()).orElse("NOT_SHIPPED"));
            row.put("trackingNumber", shipmentOpt.map(s -> s.getTrackingNumber()).orElse(""));
            row.put("courierName", shipmentOpt.map(s -> s.getCourierName()).orElse(""));
            row.put("currentLocation", shipmentOpt.map(s -> s.getCurrentLocation()).orElse(""));
            row.put("trackingUrl", shipmentOpt.map(s -> s.getTrackingUrl()).orElse(""));
            row.put("lastUpdated", shipmentOpt.map(s -> {
                if (s.getDeliveredAt() != null) return s.getDeliveredAt();
                if (s.getShippedAt() != null) return s.getShippedAt();
                if (s.getReadyToShipAt() != null) return s.getReadyToShipAt();
                return s.getTrackingDetailsSentAt();
            }).orElse(null));
            row.put("history", shipmentOpt.map(s -> s.getHistory().stream().map(event -> {
                Map<String, Object> h = new HashMap<>();
                h.put("status", event.getStatus() != null ? event.getStatus().name() : null);
                h.put("note", event.getNote());
                h.put("eventTime", event.getEventTime());
                return h;
            }).toList()).orElse(List.of()));
            result.add(row);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Supplier tracking status retrieved successfully", result));
    }

    @GetMapping("/supplier/{supplierId}/waiting-list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSupplierWaitingList(@PathVariable Long supplierId) {
        List<Payment> payments = paymentService.getPaymentsForSupplier(supplierId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Payment payment : payments) {
            // A supplier waiting-list entry should appear only after the user has
            // actually completed payment. Payment details being sent is not enough.
            if (payment.getStatus() == null || !"PAID".equals(payment.getStatus().name())) continue;

            Long requestId = payment.getPurchaseRequest() != null ? payment.getPurchaseRequest().getId() : null;
            if (requestId == null) continue;

            String shipmentStatus = shipmentRepository.findByPurchaseRequestId(requestId)
                    .map(s -> s.getStatus().name())
                    .orElse("NOT_SHIPPED");

            if ("DELIVERED".equals(shipmentStatus)) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("purchaseRequestId", requestId);
            row.put("userId", payment.getUser() != null ? payment.getUser().getId() : null);
            row.put("userName", payment.getUser() != null ? payment.getUser().getFullName() : "");
            row.put("userEmail", payment.getUser() != null ? payment.getUser().getEmail() : "");
            row.put("title", payment.getPurchaseRequest().getTitle());
            row.put("amount", payment.getAmount());
            row.put("paymentStatus", payment.getStatus().name());
            row.put("shipmentStatus", shipmentStatus);
            row.put("paidAt", payment.getPaidAt());
            result.add(row);
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Supplier waiting list retrieved successfully", result));
    }

    @GetMapping("/export/supplier/{supplierId}")
    public ResponseEntity<byte[]> exportSupplierPaymentsCsv(@PathVariable Long supplierId) {
        byte[] csv = paymentService.exportPaymentsForSupplierAsCsv(supplierId);
        return csvResponse(csv, "supplier-" + supplierId + "-payments.csv");
    }

    private void requireAdmin(User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only an admin can access this resource");
        }
    }

    private ResponseEntity<byte[]> csvResponse(byte[] csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
