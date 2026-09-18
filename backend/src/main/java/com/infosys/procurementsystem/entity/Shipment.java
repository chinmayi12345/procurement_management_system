package com.infosys.procurementsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.infosys.procurementsystem.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks a shipment for an approved (and paid) purchase request, from the
 * supplier marking the product ready to ship through to delivery at the
 * user's end. Every status change is appended to {@link #history} so the
 * user can see the full tracking trail.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shipments")
public class Shipment extends BaseEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "courier_name")
    private String courierName;

    @Column(name = "tracking_url")
    private String trackingUrl;

    @Column(name = "tracking_details_sent_at")
    private LocalDateTime trackingDetailsSentAt;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "ready_to_ship_at")
    private LocalDateTime readyToShipAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shipment_history", joinColumns = @JoinColumn(name = "shipment_id"))
    @OrderColumn(name = "event_order")
    private List<TrackingEvent> history = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class TrackingEvent {

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        private ShipmentStatus status;

        @Column(name = "note")
        private String note;

        @Column(name = "event_time", nullable = false)
        private LocalDateTime eventTime;
    }
}
