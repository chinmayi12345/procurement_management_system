package com.infosys.procurementsystem.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sent by the supplier (no login required - suppliers act through this
 * endpoint directly) to share their payment / bank details with the user
 * whose purchase request was approved.
 */
@Data
public class SendPaymentDetailsRequest {

    @NotNull
    private Long purchaseRequestId;

    @NotNull
    private Long supplierId;

    private String bankName;

    private String accountNumber;

    private String ifscCode;

    private String upiId;

    private String paymentNotes;
}
