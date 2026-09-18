package com.infosys.procurementsystem.controller;

import com.infosys.procurementsystem.common.ApiResponse;
import com.infosys.procurementsystem.entity.ApprovalHierarchy;
import com.infosys.procurementsystem.service.ApprovalHierarchyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval-hierarchies")
@RequiredArgsConstructor
public class ApprovalHierarchyController {

    private final ApprovalHierarchyService approvalHierarchyService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApprovalHierarchy>> createApprovalHierarchy(@Valid @RequestBody ApprovalHierarchy hierarchy) {
        ApprovalHierarchy created = approvalHierarchyService.createApprovalHierarchy(hierarchy);
        return new ResponseEntity<>(new ApiResponse<>(true, "Approval hierarchy level created successfully", created), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalHierarchy>> getApprovalHierarchyById(@PathVariable Long id) {
        ApprovalHierarchy hierarchy = approvalHierarchyService.getApprovalHierarchyById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Approval hierarchy level retrieved successfully", hierarchy));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<ApprovalHierarchy>>> getHierarchyByDepartment(@PathVariable Long departmentId) {
        List<ApprovalHierarchy> hierarchyList = approvalHierarchyService.getHierarchyByDepartment(departmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department approval hierarchy retrieved successfully", hierarchyList));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApprovalHierarchy>>> getAllApprovalHierarchies() {
        List<ApprovalHierarchy> hierarchies = approvalHierarchyService.getAllApprovalHierarchies();
        return ResponseEntity.ok(new ApiResponse<>(true, "All approval hierarchies retrieved successfully", hierarchies));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalHierarchy>> updateApprovalHierarchy(@PathVariable Long id, @Valid @RequestBody ApprovalHierarchy hierarchyDetails) {
        ApprovalHierarchy updated = approvalHierarchyService.updateApprovalHierarchy(id, hierarchyDetails);
        return ResponseEntity.ok(new ApiResponse<>(true, "Approval hierarchy level updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApprovalHierarchy(@PathVariable Long id) {
        approvalHierarchyService.deleteApprovalHierarchy(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Approval hierarchy level deleted successfully"));
    }
}
