package com.infosys.procurementsystem.repository;

import com.infosys.procurementsystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPurchaseRequestId(Long purchaseRequestId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findBySupplierId(Long supplierId);
}
