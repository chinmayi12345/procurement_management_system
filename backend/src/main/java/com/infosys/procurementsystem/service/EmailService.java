package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.entity.Payment;
import com.infosys.procurementsystem.entity.PurchaseRequest;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.NotificationType;

public interface EmailService {

    /**
     * Notifies the requester that their purchase request has been raised
     * (submitted) successfully and is now pending approval.
     */
    void sendRequestRaisedEmailToRequester(PurchaseRequest request);

    /**
     * Notifies the department admin that a new purchase request has been
     * raised and is awaiting their approval.
     */
    void sendRequestRaisedEmailToAdmin(PurchaseRequest request);

    /**
     * Notifies the requester that their purchase request was approved.
     */
    void sendRequestApprovedEmail(PurchaseRequest request);

    /**
     * Notifies the requester that their purchase request was rejected.
     */
    void sendRequestRejectedEmail(PurchaseRequest request);

    /**
     * Sends a generic application notification to a user's email address.
     */
    void sendNotificationEmail(User user, NotificationType notificationType, String subject, String message);

    /**
     * Sends a generic application notification to any recipient by raw email
     * address (used for suppliers, who are not application {@link User}s and
     * therefore have no login/User record to notify through).
     */
    void sendNotificationEmail(String toEmail, String recipientName, NotificationType notificationType,
                                String subject, String message);

    /**
     * Notifies every supplier whose product(s) appear on the purchase request
     * that it has been approved, so they can send payment details next.
     */
    void sendRequestApprovedEmailToSuppliers(PurchaseRequest request);

    /**
     * Notifies the requester that a supplier has sent payment details for
     * their approved purchase request. The email includes the full set of
     * payment details (bank name, account number, IFSC code, UPI ID, notes)
     * so the requester has everything they need to make the payment.
     */
    void sendPaymentDetailsEmailToRequester(Payment payment);
}
