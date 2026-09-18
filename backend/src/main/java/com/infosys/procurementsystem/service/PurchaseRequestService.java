package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.entity.PurchaseRequest;
import java.util.List;

public interface PurchaseRequestService {
    PurchaseRequest createPurchaseRequest(PurchaseRequest request, Long requesterId);
    PurchaseRequest submitPurchaseRequest(Long id, Long requesterId);
    PurchaseRequest approvePurchaseRequest(Long id, Long approverId);
    PurchaseRequest rejectPurchaseRequest(Long id, Long approverId);
    PurchaseRequest getPurchaseRequestById(Long id);
    List<PurchaseRequest> getAllPurchaseRequests();
    List<PurchaseRequest> getPurchaseRequestsByRequester(Long requesterId);
    List<PurchaseRequest> getPurchaseRequestsByApprover(Long approverId);
    /**
     * Returns purchase requests for the given department, but only if the
     * requesting user is that department's designated admin.
     */
    List<PurchaseRequest> getPurchaseRequestsByDepartment(Long departmentId, Long requestingUserId);
    PurchaseRequest updatePurchaseRequest(Long id, PurchaseRequest requestDetails, Long userId);
    void deletePurchaseRequest(Long id, Long userId);

    /** CSV report of all users' purchase requests, approval and payment status, for admins. */
    byte[] exportAdminProcurementReportAsCsv();
}
