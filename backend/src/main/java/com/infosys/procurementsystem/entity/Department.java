package com.infosys.procurementsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "departments")
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    /**
     * The single user who administers this department. Only this user may
     * approve/reject purchase requests raised in this department, and only
     * this user can view the department's requests as an admin.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_id", unique = true)
    @JsonIgnoreProperties({ "department", "hibernateLazyInitializer", "handler" })
    private User admin;
}
