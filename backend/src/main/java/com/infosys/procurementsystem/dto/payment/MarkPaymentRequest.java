package com.infosys.procurementsystem.dto.payment;

import lombok.Data;

@Data
public class MarkPaymentRequest {
    private String paymentMethod; // UPI or CARD
    private String cardLast4;
    private String transactionReference;
}
