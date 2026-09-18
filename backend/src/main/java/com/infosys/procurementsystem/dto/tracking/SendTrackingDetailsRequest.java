package com.infosys.procurementsystem.dto.tracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sent by the supplier as its own step (independent of the shipped /
 * in-transit / delivered status pushes) to share or update the tracking
 * number / courier / tracking link for a shipment already in progress.
 * Triggers a dedicated "tracking details" email to the requesting user.
 */
@Data
public class SendTrackingDetailsRequest {

    @NotNull
    private Long supplierId;

    @NotBlank
    private String trackingNumber;

    private String courierName;

    private String trackingUrl;

    private String note;
}
