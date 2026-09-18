package com.infosys.procurementsystem.repository;

import com.infosys.procurementsystem.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findByRequesterId(Long requesterId);

    List<PurchaseRequest> findByCurrentApproverId(Long currentApproverId);

    List<PurchaseRequest> findByDepartmentId(Long departmentId);
}
