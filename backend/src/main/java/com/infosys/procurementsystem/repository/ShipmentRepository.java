package com.infosys.procurementsystem.repository;

import com.infosys.procurementsystem.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByPurchaseRequestId(Long purchaseRequestId);

    List<Shipment> findByUserId(Long userId);
}
