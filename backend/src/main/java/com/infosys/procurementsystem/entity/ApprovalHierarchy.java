package com.infosys.procurementsystem.entity;

import com.infosys.procurementsystem.enums.ApprovalRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "approval_hierarchies", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"department_id", "level"})
})
public class ApprovalHierarchy extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private Integer level; // e.g., 1 for Level 1 approver, 2 for Level 2 approver

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_role", nullable = false)
    private ApprovalRole approvalRole;
}
