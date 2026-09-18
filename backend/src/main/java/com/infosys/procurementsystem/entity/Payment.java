package com.infosys.procurementsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.infosys.procurementsystem.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks the payment lifecycle for an approved purchase request:
 * the supplier sends bank/payment details to the requesting user,
 * and the user marks the payment as done once it is completed.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_request_id", nullable = false, unique = true)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "department", "hibernateLazyInitializer", "handler" })
    private User user;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "payment_method")
    private String paymentMethod;

    // Only store the last 4 digits; never store the full card number or CVV.
    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Column(name = "payment_notes", length = 1000)
    private String paymentNotes;

    @Column(name = "details_sent_at")
    private LocalDateTime detailsSentAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
