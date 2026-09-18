package com.infosys.procurementsystem.repository;

import com.infosys.procurementsystem.entity.ApprovalHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalHierarchyRepository extends JpaRepository<ApprovalHierarchy, Long> {
    List<ApprovalHierarchy> findByDepartmentIdOrderByLevelAsc(Long departmentId);

    Optional<ApprovalHierarchy> findByDepartmentIdAndLevel(Long departmentId, Integer level);
}
