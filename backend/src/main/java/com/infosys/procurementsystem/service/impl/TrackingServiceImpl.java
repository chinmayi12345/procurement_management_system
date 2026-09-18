package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.dto.tracking.SendTrackingDetailsRequest;
import com.infosys.procurementsystem.dto.tracking.UpdateTrackingRequest;
import com.infosys.procurementsystem.entity.Payment;
import com.infosys.procurementsystem.entity.PurchaseRequest;
import com.infosys.procurementsystem.entity.Shipment;
import com.infosys.procurementsystem.entity.Supplier;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.NotificationType;
import com.infosys.procurementsystem.enums.PaymentStatus;
import com.infosys.procurementsystem.enums.ShipmentStatus;
import com.infosys.procurementsystem.exception.BadRequestException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.repository.PaymentRepository;
import com.infosys.procurementsystem.repository.PurchaseRequestRepository;
import com.infosys.procurementsystem.repository.ShipmentRepository;
import com.infosys.procurementsystem.repository.SupplierRepository;
import com.infosys.procurementsystem.service.TrackingService;
import com.infosys.procurementsystem.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final ShipmentRepository shipmentRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public Shipment markReadyToShip(Long purchaseRequestId, UpdateTrackingRequest dto) {
        PurchaseRequest purchaseRequest = getPurchaseRequest(purchaseRequestId);

        Payment payment = paymentRepository.findByPurchaseRequestId(purchaseRequestId)
                .orElseThrow(() -> new BadRequestException("Payment must be completed before the product can be marked ready to ship"));
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Payment must be completed before the product can be marked ready to ship");
        }

        Shipment shipment = shipmentRepository.findByPurchaseRequestId(purchaseRequestId).orElseGet(Shipment::new);
        if (shipment.getId() != null) {
            throw new BadRequestException("This purchase request already has a shipment in progress");
        }

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + dto.getSupplierId()));

        shipment.setPurchaseRequest(purchaseRequest);
        shipment.setSupplier(supplier);
        shipment.setUser(purchaseRequest.getRequester());
        shipment.setStatus(ShipmentStatus.READY_TO_SHIP);
        shipment.setTrackingNumber(dto.getTrackingNumber());
        shipment.setReadyToShipAt(LocalDateTime.now());

        return applyEventAndNotify(shipment, ShipmentStatus.READY_TO_SHIP, dto.getNote(),
                NotificationType.PRODUCT_READY_TO_SHIP,
                "Your order is ready to ship - Request #" + purchaseRequestId,
                "Good news! Your order for purchase request #" + purchaseRequestId
                        + " is ready to ship" + (dto.getTrackingNumber() != null ? " (tracking #" + dto.getTrackingNumber() + ")" : "") + ".");
    }

    @Override
    @Transactional
    public Shipment sendTrackingDetails(Long purchaseRequestId, SendTrackingDetailsRequest dto) {
        Shipment shipment = requireExistingShipment(purchaseRequestId);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new BadRequestException("Shipment for request #" + purchaseRequestId
                    + " has already been delivered; tracking details can no longer be updated");
        }

        if (!shipment.getSupplier().getId().equals(dto.getSupplierId())) {
            throw new BadRequestException("Only the supplier assigned to this shipment can send tracking details");
        }

        shipment.setTrackingNumber(dto.getTrackingNumber());
        shipment.setCourierName(dto.getCourierName());
        shipment.setTrackingUrl(dto.getTrackingUrl());
        shipment.setTrackingDetailsSentAt(LocalDateTime.now());

        Shipment.TrackingEvent event = new Shipment.TrackingEvent();
        event.setStatus(shipment.getStatus());
        event.setNote(dto.getNote() != null && !dto.getNote().isBlank()
                ? dto.getNote()
                : "Tracking details sent: " + dto.getTrackingNumber());
        event.setEventTime(LocalDateTime.now());
        shipment.getHistory().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        String courierText = dto.getCourierName() != null && !dto.getCourierName().isBlank()
                ? " via " + dto.getCourierName() : "";
        String urlText = dto.getTrackingUrl() != null && !dto.getTrackingUrl().isBlank()
                ? " Track it here: " + dto.getTrackingUrl() : "";

        User user = saved.getUser();
        emailService.sendNotificationEmail(user, NotificationType.TRACKING_DETAILS_SENT,
                "Tracking Details Available - Request #" + purchaseRequestId,
                "Tracking details are available for your order (purchase request #" + purchaseRequestId
                        + "). Tracking number: " + dto.getTrackingNumber() + courierText + "." + urlText);

        return saved;
    }

    @Override
    @Transactional
    public Shipment markShipped(Long purchaseRequestId, UpdateTrackingRequest dto) {
        Shipment shipment = requireExistingShipment(purchaseRequestId);
        requireCurrentStatus(shipment, ShipmentStatus.READY_TO_SHIP);

        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());
        if (dto.getTrackingNumber() != null && !dto.getTrackingNumber().isBlank()) {
            shipment.setTrackingNumber(dto.getTrackingNumber());
        }

        return applyEventAndNotify(shipment, ShipmentStatus.SHIPPED, dto.getNote(),
                NotificationType.PRODUCT_SHIPPED,
                "Your order has shipped - Request #" + purchaseRequestId,
                "Your order for purchase request #" + purchaseRequestId + " has been shipped and is on its way.");
    }

    @Override
    @Transactional
    public Shipment markInTransit(Long purchaseRequestId, UpdateTrackingRequest dto) {
        Shipment shipment = requireExistingShipment(purchaseRequestId);
        requireCurrentStatus(shipment, ShipmentStatus.SHIPPED, ShipmentStatus.IN_TRANSIT);

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        if (dto.getCurrentLocation() != null && !dto.getCurrentLocation().isBlank()) {
            shipment.setCurrentLocation(dto.getCurrentLocation());
        }

        String location = dto.getCurrentLocation() != null && !dto.getCurrentLocation().isBlank()
                ? " Current location: " + dto.getCurrentLocation() + "." : "";

        return applyEventAndNotify(shipment, ShipmentStatus.IN_TRANSIT, dto.getNote(),
                NotificationType.PRODUCT_IN_TRANSIT,
                "Your order is in transit - Request #" + purchaseRequestId,
                "Your order for purchase request #" + purchaseRequestId + " is currently in transit." + location);
    }

    @Override
    @Transactional
    public Shipment markDelivered(Long purchaseRequestId, UpdateTrackingRequest dto) {
        Shipment shipment = requireExistingShipment(purchaseRequestId);
        requireCurrentStatus(shipment, ShipmentStatus.SHIPPED, ShipmentStatus.IN_TRANSIT);

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now());

        return applyEventAndNotify(shipment, ShipmentStatus.DELIVERED, dto.getNote(),
                NotificationType.PRODUCT_DELIVERED,
                "Your order has been delivered - Request #" + purchaseRequestId,
                "Your order for purchase request #" + purchaseRequestId + " has reached you. Thanks for using the Procurement System!");
    }

    @Override
    public Shipment getByPurchaseRequestId(Long purchaseRequestId) {
        return requireExistingShipment(purchaseRequestId);
    }

    @Override
    public List<Shipment> getShipmentsForUser(Long userId) {
        return shipmentRepository.findByUserId(userId);
    }

    private PurchaseRequest getPurchaseRequest(Long id) {
        return purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase request not found with id: " + id));
    }

    private Shipment requireExistingShipment(Long purchaseRequestId) {
        return shipmentRepository.findByPurchaseRequestId(purchaseRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("No shipment/tracking record found for purchase request id: " + purchaseRequestId));
    }

    private void requireCurrentStatus(Shipment shipment, ShipmentStatus... allowed) {
        for (ShipmentStatus status : allowed) {
            if (shipment.getStatus() == status) {
                return;
            }
        }
        throw new BadRequestException("Shipment for request #" + shipment.getPurchaseRequest().getId()
                + " is currently " + shipment.getStatus() + " and cannot move to this next status yet");
    }

    private Shipment applyEventAndNotify(Shipment shipment, ShipmentStatus status, String note,
                                          NotificationType notificationType, String title, String message) {
        Shipment.TrackingEvent event = new Shipment.TrackingEvent();
        event.setStatus(status);
        event.setNote(note);
        event.setEventTime(LocalDateTime.now());
        shipment.getHistory().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        User user = saved.getUser();
        emailService.sendNotificationEmail(user, notificationType, title, message);

        return saved;
    }
}
