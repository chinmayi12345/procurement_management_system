package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.dto.tracking.SendTrackingDetailsRequest;
import com.infosys.procurementsystem.dto.tracking.UpdateTrackingRequest;
import com.infosys.procurementsystem.entity.Shipment;

import java.util.List;

public interface TrackingService {

    Shipment markReadyToShip(Long purchaseRequestId, UpdateTrackingRequest requestDto);

    /**
     * Standalone step: supplier sends/updates the tracking number, courier,
     * and tracking link for a shipment already in progress. Independent of
     * the shipped / in-transit / delivered status pushes; sends its own
     * dedicated "tracking details" email to the user.
     */
    Shipment sendTrackingDetails(Long purchaseRequestId, SendTrackingDetailsRequest requestDto);

    Shipment markShipped(Long purchaseRequestId, UpdateTrackingRequest requestDto);

    Shipment markInTransit(Long purchaseRequestId, UpdateTrackingRequest requestDto);

    Shipment markDelivered(Long purchaseRequestId, UpdateTrackingRequest requestDto);

    Shipment getByPurchaseRequestId(Long purchaseRequestId);

    List<Shipment> getShipmentsForUser(Long userId);
}
