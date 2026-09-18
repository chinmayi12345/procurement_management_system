package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.entity.Payment;
import com.infosys.procurementsystem.entity.PurchaseRequest;
import com.infosys.procurementsystem.entity.Supplier;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.NotificationType;
import com.infosys.procurementsystem.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =========================================================
    // SHARED BRANDED TEMPLATE
    //
    // Every single email sent by this service (request emails,
    // generic notifications, payment emails, tracking emails, ...)
    // is rendered through wrapEmail(...) so that every mail the
    // user receives shares the exact same "PROCUREMENT SYSTEM"
    // header, card layout and footer.
    // =========================================================

    private static final String STYLE = """
            body {
                margin: 0;
                padding: 0;
                background-color: #f4f6f8;
                font-family: Arial, Helvetica, sans-serif;
            }

            .container {
                width: 100%;
                padding: 40px 0;
            }

            .email {
                width: 600px;
                max-width: 90%;
                margin: auto;
                background-color: #ffffff;
                border-radius: 8px;
                overflow: hidden;
                box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            }

            .header {
                background-color: #4a90d9;
                padding: 30px;
                text-align: center;
                color: #ffffff;
            }

            .header h1 {
                margin: 0;
                font-size: 24px;
                letter-spacing: 1px;
            }

            .content {
                padding: 35px;
                color: #333333;
            }

            .content h2 {
                margin-top: 0;
                color: #222222;
            }

            .details {
                background-color: #f7f9fb;
                padding: 20px;
                border-radius: 6px;
                margin: 25px 0;
            }

            .details p {
                margin: 10px 0;
            }

            .pending { color: #e67e22; font-weight: bold; }
            .approved { color: #2e9d57; font-weight: bold; }
            .rejected { color: #d9534f; font-weight: bold; }
            .info { color: #4a90d9; font-weight: bold; }

            .footer {
                padding: 20px 35px;
                background-color: #f1f3f5;
                color: #777777;
                font-size: 12px;
                text-align: center;
            }
            """;

    /**
     * Wraps a fragment of content markup (heading, paragraphs, an optional
     * "details" box, closing regards, etc.) in the standard Procurement
     * System email shell: header banner + content + footer.
     */
    private String wrapEmail(String contentHtml) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                %s
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="email">
                            <div class="header">
                                <h1>PROCUREMENT SYSTEM</h1>
                            </div>
                            <div class="content">
                %s
                            </div>
                            <div class="footer">
                                <strong>Procurement System</strong>
                                <p>This is an automated email. Please do not reply to this email.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(STYLE, contentHtml);
    }

    /** Builds one row inside a ".details" box, e.g. "Request ID: #12". */
    private String detailRow(String label, String value) {
        return "<p><strong>" + label + ":</strong> " + value + "</p>";
    }

    /** Builds a status row with the colored badge span, e.g. Status: APPROVED. */
    private String statusRow(String cssClass, String label) {
        return "<p><strong>Status:</strong> <span class=\"" + cssClass + "\">" + label + "</span></p>";
    }

    // =========================================================
    // 1. REQUEST SUBMITTED -> REQUESTER
    // =========================================================

    @Override
    public void sendRequestRaisedEmailToRequester(PurchaseRequest request) {

        String subject = "Purchase Request Raised Successfully - #" + request.getId();

        String approverName = request.getCurrentApprover() != null
                ? request.getCurrentApprover().getFullName()
                : "your department admin";

        String details = detailRow("Request ID", "#" + request.getId())
                + detailRow("Title", request.getTitle())
                + detailRow("Total Amount", "\u20B9" + request.getTotalAmount())
                + detailRow("Approver", approverName)
                + statusRow("pending", "PENDING APPROVAL");

        String content = """
                <h2>Purchase Request Submitted</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your purchase request has been successfully submitted and is now waiting for approval.</p>
                <div class="details">
                %s
                </div>
                <p>You will receive another email once your request has been approved or rejected.</p>
                <p>Regards,<br><strong>Procurement System Team</strong></p>
                """.formatted(request.getRequester().getFullName(), details);

        sendHtml(request.getRequester().getEmail(), subject, wrapEmail(content));
    }


    // =========================================================
    // 2. REQUEST SUBMITTED -> ADMIN
    // =========================================================

    @Override
    public void sendRequestRaisedEmailToAdmin(PurchaseRequest request) {

        if (request.getCurrentApprover() == null) {
            log.warn("Cannot notify admin for purchase request #{} - no current approver assigned",
                    request.getId());
            return;
        }

        String subject = "New Purchase Request Pending Your Approval - #" + request.getId();

        String details = detailRow("Request ID", "#" + request.getId())
                + detailRow("Title", request.getTitle())
                + detailRow("Requested By", request.getRequester().getFullName())
                + detailRow("Department", request.getDepartment().getName())
                + detailRow("Total Amount", "\u20B9" + request.getTotalAmount())
                + statusRow("pending", "PENDING APPROVAL");

        String content = """
                <h2>New Purchase Request</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>A new purchase request has been submitted and requires your approval.</p>
                <div class="details">
                %s
                </div>
                <p>Please log in to the Procurement System to review and approve or reject this purchase request.</p>
                <p>Regards,<br><strong>Procurement System Team</strong></p>
                """.formatted(request.getCurrentApprover().getFullName(), details);

        sendHtml(request.getCurrentApprover().getEmail(), subject, wrapEmail(content));
    }


    // =========================================================
    // 3. REQUEST APPROVED -> REQUESTER
    // =========================================================

    @Override
    public void sendRequestApprovedEmail(PurchaseRequest request) {
        if (request == null || request.getRequester() == null) {
            log.warn("Cannot send approval email because the purchase requester is missing");
            return;
        }

        log.info("Sending purchase request approval email for request #{} to {}",
                request.getId(), request.getRequester().getEmail());

        String subject = "Your Purchase Request Was Approved - #" + request.getId();

        String details = detailRow("Request ID", "#" + request.getId())
                + detailRow("Title", request.getTitle())
                + detailRow("Total Amount", "\u20B9" + request.getTotalAmount())
                + statusRow("approved", "APPROVED");

        String content = """
                <h2>Purchase Request Approved</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Good news! Your purchase request has been approved successfully.</p>
                <div class="details">
                %s
                </div>
                <p>Your purchase request has successfully completed the approval process.</p>
                <p>Regards,<br><strong>Procurement System Team</strong></p>
                """.formatted(request.getRequester().getFullName(), details);

        sendHtml(request.getRequester().getEmail(), subject, wrapEmail(content));
    }


    // =========================================================
    // 4. REQUEST REJECTED -> REQUESTER
    // =========================================================

    @Override
    public void sendRequestRejectedEmail(PurchaseRequest request) {

        String subject = "Your Purchase Request Was Rejected - #" + request.getId();

        String adminName = request.getDepartment().getAdmin() != null
                ? request.getDepartment().getAdmin().getFullName()
                : "your department admin";

        String details = detailRow("Request ID", "#" + request.getId())
                + detailRow("Title", request.getTitle())
                + detailRow("Total Amount", "\u20B9" + request.getTotalAmount())
                + statusRow("rejected", "REJECTED");

        String content = """
                <h2>Purchase Request Rejected</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your purchase request has been rejected by <strong>%s</strong>.</p>
                <div class="details">
                %s
                </div>
                <p>Please contact your department admin for more information.</p>
                <p>Regards,<br><strong>Procurement System Team</strong></p>
                """.formatted(request.getRequester().getFullName(), adminName, details);

        sendHtml(request.getRequester().getEmail(), subject, wrapEmail(content));
    }

    @Override
    public void sendNotificationEmail(User user, NotificationType notificationType, String subject, String message) {
        if (user == null) {
            log.warn("Cannot send {} notification because the user is missing", notificationType);
            return;
        }

        sendNotificationEmail(user.getEmail(), user.getFullName(), notificationType, subject, message);
    }

    // =========================================================
    // GENERIC NOTIFICATION -> ANY RECIPIENT (e.g. suppliers, who
    // have no User/login record in this system)
    // =========================================================

    @Override
    public void sendNotificationEmail(String toEmail, String recipientName, NotificationType notificationType,
                                       String subject, String message) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send {} notification because the recipient email is missing", notificationType);
            return;
        }

        String content = """
                <h2>%s</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>%s</p>
                <p>Regards,<br><strong>Procurement System Team</strong></p>
                """.formatted(subject, recipientName != null ? recipientName : "there", message);

        sendHtml(toEmail, subject, wrapEmail(content));
    }

    // =========================================================
    // REQUEST APPROVED -> SUPPLIER(S)
    // =========================================================

    @Override
    public void sendRequestApprovedEmailToSuppliers(PurchaseRequest request) {
        if (request == null || request.getItems() == null) {
            return;
        }

        request.getItems().stream()
                .map(item -> item.getProduct() != null ? item.getProduct().getSupplier() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        Supplier::getId,
                        supplier -> supplier,
                        (a, b) -> a))
                .values()
                .forEach(supplier -> {
                    String subject = "Purchase Request #" + request.getId() + " Approved - Payment Details Needed";
                    String message = "Purchase request #" + request.getId() + " (" + request.getTitle()
                            + ") placed by " + request.getRequester().getFullName()
                            + " has been approved. Total amount: Rs. " + request.getTotalAmount()
                            + ". Please send your payment details for this order via the supplier payment API.";

                    log.info("Notifying supplier {} (#{}) that purchase request #{} was approved",
                            supplier.getName(), supplier.getId(), request.getId());

                    sendNotificationEmail(supplier.getEmail(), supplier.getName(),
                            NotificationType.REQUEST_APPROVED, subject, message);
                });
    }

    // =========================================================
    // SUPPLIER SENT PAYMENT DETAILS -> REQUESTER
    // =========================================================

    @Override
    public void sendPaymentDetailsEmailToRequester(Payment payment) {
        if (payment == null || payment.getUser() == null) {
            log.warn("Cannot send payment details email because the payment or requester is missing");
            return;
        }

        PurchaseRequest request = payment.getPurchaseRequest();
        Supplier supplier = payment.getSupplier();
        User requester = payment.getUser();

        String subject = "Payment Details Received - Request #"
                + (request != null ? request.getId() : payment.getId());

        StringBuilder details = new StringBuilder();
        if (request != null) {
            details.append(detailRow("Request ID", "#" + request.getId()));
            details.append(detailRow("Title", request.getTitle()));
        }
        details.append(detailRow("Supplier", supplier != null ? supplier.getName() : "N/A"));
        details.append(detailRow("Amount Due", "\u20B9" + payment.getAmount()));
        details.append(detailRow("Bank Name", nullSafe(payment.getBankName())));
        details.append(detailRow("Account Number", nullSafe(payment.getAccountNumber())));
        details.append(detailRow("IFSC Code", nullSafe(payment.getIfscCode())));
        details.append(detailRow("UPI ID", nullSafe(payment.getUpiId())));
        if (payment.getPaymentNotes() != null && !payment.getPaymentNotes().isBlank()) {
            details.append(detailRow("Payment Notes", payment.getPaymentNotes()));
        }
        details.append(statusRow("info", "DETAILS SENT"));

        String content = """
                <h2>Payment Details Received</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Supplier <strong>%s</strong> has sent their payment details for your approved purchase request. Please review the details below and complete the payment.</p>
                <div class="details">
                %s
                </div>
                <p>Once you have made the payment, please log in to the Procurement System and mark this payment as done.</p>
                <p>Regards,<br><strong>Procurement System Team</strong></p>
                """.formatted(
                        requester.getFullName(),
                        supplier != null ? supplier.getName() : "your supplier",
                        details.toString());

        log.info("Sending payment details email to {} for request #{}",
                requester.getEmail(), request != null ? request.getId() : payment.getId());

        sendHtml(requester.getEmail(), subject, wrapEmail(content));
    }

    private String nullSafe(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }


    // =========================================================
    // SEND HTML EMAIL
    // =========================================================

    private void sendHtml(
            String to,
            String subject,
            String body
    ) {

        if (!mailEnabled) {

            log.info(
                    "Email sending disabled. "
                            + "Skipped email to {} - subject: {}",
                    to,
                    subject
            );

            return;
        }

        if (to == null || to.isBlank()) {

            log.warn(
                    "Skipping email - recipient address is empty. "
                            + "Subject: {}",
                    subject
            );

            return;
        }

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);

            // true = HTML email
            helper.setText(body, true);

            mailSender.send(message);

            log.info(
                    "HTML email sent successfully to {} - subject: {}",
                    to,
                    subject
            );

        } catch (MessagingException | MailException ex) {

            log.error(
                    "Failed to send HTML email to {} - "
                            + "subject: {}. Reason: {}",
                    to,
                    subject,
                    ex.getMessage(),
                    ex
            );

            throw new IllegalStateException("Unable to send email notification to " + to, ex);
        }
    }
}
