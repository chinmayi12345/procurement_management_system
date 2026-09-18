package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.entity.ApprovalHierarchy;
import com.infosys.procurementsystem.entity.Department;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.exception.BadRequestException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.repository.ApprovalHierarchyRepository;
import com.infosys.procurementsystem.repository.DepartmentRepository;
import com.infosys.procurementsystem.repository.UserRepository;
import com.infosys.procurementsystem.service.ApprovalHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalHierarchyServiceImpl implements ApprovalHierarchyService {

    private final ApprovalHierarchyRepository approvalHierarchyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ApprovalHierarchy createApprovalHierarchy(ApprovalHierarchy hierarchy) {
        Department dept = departmentRepository.findById(hierarchy.getDepartment().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + hierarchy.getDepartment().getId()));

        User approver = userRepository.findById(hierarchy.getApprover().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Approver user not found with id: " + hierarchy.getApprover().getId()));

        approvalHierarchyRepository.findByDepartmentIdAndLevel(dept.getId(), hierarchy.getLevel()).ifPresent(h -> {
            throw new BadRequestException("Approval hierarchy level " + hierarchy.getLevel()
                    + " already exists for department " + dept.getName());
        });

        hierarchy.setDepartment(dept);
        hierarchy.setApprover(approver);
        return approvalHierarchyRepository.save(hierarchy);
    }

    @Override
    public ApprovalHierarchy getApprovalHierarchyById(Long id) {
        return approvalHierarchyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval hierarchy not found with id: " + id));
    }

    @Override
    public List<ApprovalHierarchy> getHierarchyByDepartment(Long departmentId) {
        return approvalHierarchyRepository.findByDepartmentIdOrderByLevelAsc(departmentId);
    }

    @Override
    public List<ApprovalHierarchy> getAllApprovalHierarchies() {
        return approvalHierarchyRepository.findAll();
    }

    @Override
    @Transactional
    public ApprovalHierarchy updateApprovalHierarchy(Long id, ApprovalHierarchy hierarchyDetails) {
        ApprovalHierarchy hierarchy = getApprovalHierarchyById(id);

        Department dept = departmentRepository.findById(hierarchyDetails.getDepartment().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + hierarchyDetails.getDepartment().getId()));

        User approver = userRepository.findById(hierarchyDetails.getApprover().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Approver user not found with id: " + hierarchyDetails.getApprover().getId()));

        approvalHierarchyRepository.findByDepartmentIdAndLevel(dept.getId(), hierarchyDetails.getLevel())
                .ifPresent(h -> {
                    if (!h.getId().equals(id)) {
                        throw new BadRequestException("Approval hierarchy level " + hierarchyDetails.getLevel()
                                + " already exists for department " + dept.getName());
                    }
                });

        hierarchy.setDepartment(dept);
        hierarchy.setLevel(hierarchyDetails.getLevel());
        hierarchy.setApprover(approver);
        hierarchy.setApprovalRole(hierarchyDetails.getApprovalRole());

        return approvalHierarchyRepository.save(hierarchy);
    }

    @Override
    @Transactional
    public void deleteApprovalHierarchy(Long id) {
        ApprovalHierarchy hierarchy = getApprovalHierarchyById(id);
        approvalHierarchyRepository.delete(hierarchy);
    }
}
