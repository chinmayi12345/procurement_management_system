package com.infosys.procurementsystem.dto.tracking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sent by the supplier to push a shipment status update for a purchase
 * request (ready to ship / shipped / in transit / delivered).
 */
@Data
public class UpdateTrackingRequest {

    @NotNull
    private Long supplierId;

    private String trackingNumber;

    private String currentLocation;

    private String note;
}
