package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.dto.payment.SendPaymentDetailsRequest;
import com.infosys.procurementsystem.dto.payment.MarkPaymentRequest;
import com.infosys.procurementsystem.entity.Payment;

import java.util.List;

public interface PaymentService {

    /**
     * Supplier sends their payment/bank details for an approved purchase
     * request. Creates the Payment record on first call, updates it on
     * subsequent calls. Notifies the requesting user either way.
     */
    Payment sendPaymentDetails(SendPaymentDetailsRequest requestDto);

    /**
     * The paying user presses "Payment Done" - marks the payment as PAID
     * and notifies the supplier.
     */
    Payment markPaymentDone(Long purchaseRequestId, Long userId, MarkPaymentRequest request);

    Payment getPaymentByPurchaseRequestId(Long purchaseRequestId);

    List<Payment> getPaymentsForUser(Long userId);

    List<Payment> getPaymentsForSupplier(Long supplierId);

    List<Payment> getAllPayments();

    /**
     * CSV of a single user's own product + payment details.
     */
    byte[] exportPaymentsForUserAsCsv(Long userId);

    /**
     * CSV of every user's product + payment details, for admins.
     */
    byte[] exportAllPaymentsAsCsv();

    byte[] exportPaymentsForSupplierAsCsv(Long supplierId);
}
