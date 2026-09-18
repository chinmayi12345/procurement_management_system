package com.infosys.procurementsystem.repository;

import com.infosys.procurementsystem.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);

    Optional<Department> findByCode(String code);

    Optional<Department> findByAdminId(Long adminId);
}
