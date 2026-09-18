package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.entity.Department;
import java.util.List;

public interface DepartmentService {
    Department createDepartment(Department department);
    Department getDepartmentById(Long id);
    Department getDepartmentByCode(String code);
    List<Department> getAllDepartments();
    Department updateDepartment(Long id, Department departmentDetails);
    void deleteDepartment(Long id);
    Department assignAdmin(Long departmentId, Long userId);
    Department removeAdmin(Long departmentId);
}
