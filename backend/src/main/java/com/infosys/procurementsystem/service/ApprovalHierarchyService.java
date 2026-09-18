package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.entity.ApprovalHierarchy;
import java.util.List;

public interface ApprovalHierarchyService {
    ApprovalHierarchy createApprovalHierarchy(ApprovalHierarchy hierarchy);
    ApprovalHierarchy getApprovalHierarchyById(Long id);
    List<ApprovalHierarchy> getHierarchyByDepartment(Long departmentId);
    List<ApprovalHierarchy> getAllApprovalHierarchies();
    ApprovalHierarchy updateApprovalHierarchy(Long id, ApprovalHierarchy hierarchyDetails);
    void deleteApprovalHierarchy(Long id);
}
