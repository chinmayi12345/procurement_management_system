package com.infosys.procurementsystem.controller;

import com.infosys.procurementsystem.common.ApiResponse;
import com.infosys.procurementsystem.dto.tracking.SendTrackingDetailsRequest;
import com.infosys.procurementsystem.dto.tracking.UpdateTrackingRequest;
import com.infosys.procurementsystem.entity.Shipment;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    // ---- Supplier-pushed status updates (suppliers have no login here) ----

    @PostMapping("/supplier/{purchaseRequestId}/ready-to-ship")
    public ResponseEntity<ApiResponse<Shipment>> markReadyToShip(
            @PathVariable Long purchaseRequestId,
            @Valid @RequestBody UpdateTrackingRequest requestDto) {
        Shipment shipment = trackingService.markReadyToShip(purchaseRequestId, requestDto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product marked ready to ship and user notified", shipment));
    }

    @PostMapping("/supplier/{purchaseRequestId}/shipped")
    public ResponseEntity<ApiResponse<Shipment>> markShipped(
            @PathVariable Long purchaseRequestId,
            @Valid @RequestBody UpdateTrackingRequest requestDto) {
        Shipment shipment = trackingService.markShipped(purchaseRequestId, requestDto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product marked shipped and user notified", shipment));
    }

    /**
     * Standalone step: supplier sends/updates tracking number, courier, and
     * tracking link for a shipment already in progress. Separate from the
     * shipped / in-transit status pushes; sends its own tracking email.
     */
    @PostMapping("/supplier/{purchaseRequestId}/tracking-details")
    public ResponseEntity<ApiResponse<Shipment>> sendTrackingDetails(
            @PathVariable Long purchaseRequestId,
            @Valid @RequestBody SendTrackingDetailsRequest requestDto) {
        Shipment shipment = trackingService.sendTrackingDetails(purchaseRequestId, requestDto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tracking details sent and user notified", shipment));
    }

    @PostMapping("/supplier/{purchaseRequestId}/in-transit")
    public ResponseEntity<ApiResponse<Shipment>> markInTransit(
            @PathVariable Long purchaseRequestId,
            @Valid @RequestBody UpdateTrackingRequest requestDto) {
        Shipment shipment = trackingService.markInTransit(purchaseRequestId, requestDto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product marked in transit and user notified", shipment));
    }

    @PostMapping("/supplier/{purchaseRequestId}/delivered")
    public ResponseEntity<ApiResponse<Shipment>> markDelivered(
            @PathVariable Long purchaseRequestId,
            @Valid @RequestBody UpdateTrackingRequest requestDto) {
        Shipment shipment = trackingService.markDelivered(purchaseRequestId, requestDto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product marked delivered and user notified", shipment));
    }

    // ---- User-facing read endpoints ----

    @GetMapping("/{purchaseRequestId}")
    public ResponseEntity<ApiResponse<Shipment>> getTracking(@PathVariable Long purchaseRequestId) {
        Shipment shipment = trackingService.getByPurchaseRequestId(purchaseRequestId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tracking details retrieved successfully", shipment));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Shipment>>> getMyTracking(@AuthenticationPrincipal User currentUser) {
        List<Shipment> shipments = trackingService.getShipmentsForUser(currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Your tracking details retrieved successfully", shipments));
    }
}
