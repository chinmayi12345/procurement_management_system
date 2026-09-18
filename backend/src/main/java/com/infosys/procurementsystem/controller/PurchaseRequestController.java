package com.infosys.procurementsystem.controller;

import com.infosys.procurementsystem.common.ApiResponse;
import com.infosys.procurementsystem.entity.PurchaseRequest;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.Role;
import com.infosys.procurementsystem.exception.ForbiddenException;
import com.infosys.procurementsystem.service.PurchaseRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseRequest>> createPurchaseRequest(
            @Valid @RequestBody PurchaseRequest request,
            @AuthenticationPrincipal User currentUser) {
        PurchaseRequest created = purchaseRequestService.createPurchaseRequest(request, currentUser.getId());
        return new ResponseEntity<>(new ApiResponse<>(true, "Purchase request created in DRAFT status", created), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<PurchaseRequest>> submitPurchaseRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        PurchaseRequest submitted = purchaseRequestService.submitPurchaseRequest(id, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase request submitted for approval", submitted));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PurchaseRequest>> approvePurchaseRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        PurchaseRequest approved = purchaseRequestService.approvePurchaseRequest(id, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase request approved", approved));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PurchaseRequest>> rejectPurchaseRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        PurchaseRequest rejected = purchaseRequestService.rejectPurchaseRequest(id, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase request rejected", rejected));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseRequest>> getPurchaseRequestById(@PathVariable Long id) {
        PurchaseRequest request = purchaseRequestService.getPurchaseRequestById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase request retrieved successfully", request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseRequest>>> getAllPurchaseRequests(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only an admin can view all purchase requests");
        }
        List<PurchaseRequest> requests = purchaseRequestService.getAllPurchaseRequests();
        return ResponseEntity.ok(new ApiResponse<>(true, "All purchase requests retrieved successfully", requests));
    }

    /**
     * Admin-only CSV containing every user's request, approval/rejection result,
     * and payment state. Approved requests with no PAID payment are reported as
     * PAYMENT_PENDING, so requests waiting for supplier payment details are also included.
     */
    @GetMapping("/export/admin-report")
    public ResponseEntity<byte[]> exportAdminProcurementReport(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only an admin can download the procurement report");
        }
        byte[] csv = purchaseRequestService.exportAdminProcurementReportAsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=procurement-admin-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PurchaseRequest>>> getMyPurchaseRequests(@AuthenticationPrincipal User currentUser) {
        List<PurchaseRequest> requests = purchaseRequestService.getPurchaseRequestsByRequester(currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Your purchase requests retrieved successfully", requests));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PurchaseRequest>>> getPendingMyApproval(@AuthenticationPrincipal User currentUser) {
        List<PurchaseRequest> requests = purchaseRequestService.getPurchaseRequestsByApprover(currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase requests pending your approval retrieved successfully", requests));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<PurchaseRequest>>> getPurchaseRequestsByDepartment(
            @PathVariable Long departmentId,
            @AuthenticationPrincipal User currentUser) {
        List<PurchaseRequest> requests = purchaseRequestService.getPurchaseRequestsByDepartment(departmentId, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Department purchase requests retrieved successfully", requests));
    }

    /**
     * Convenience endpoint for a department admin to fetch requests for their
     * own department without needing to know its id up front.
     */
    @GetMapping("/department/my")
    public ResponseEntity<ApiResponse<List<PurchaseRequest>>> getMyDepartmentPurchaseRequests(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.ok(new ApiResponse<>(true, "You are not assigned to a department", List.of()));
        }
        List<PurchaseRequest> requests = purchaseRequestService
                .getPurchaseRequestsByDepartment(currentUser.getDepartment().getId(), currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Your department's purchase requests retrieved successfully", requests));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseRequest>> updatePurchaseRequest(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequest requestDetails,
            @AuthenticationPrincipal User currentUser) {
        PurchaseRequest updated = purchaseRequestService.updatePurchaseRequest(id, requestDetails, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase request updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        purchaseRequestService.deletePurchaseRequest(id, currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Purchase request deleted successfully"));
    }
}
