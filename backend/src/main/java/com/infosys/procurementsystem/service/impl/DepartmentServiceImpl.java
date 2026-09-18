package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.entity.Department;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.Role;
import com.infosys.procurementsystem.exception.BadRequestException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.repository.DepartmentRepository;
import com.infosys.procurementsystem.repository.UserRepository;
import com.infosys.procurementsystem.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Department createDepartment(Department department) {
        if (departmentRepository.findByName(department.getName()).isPresent()) {
            throw new BadRequestException("Department name already exists");
        }
        if (departmentRepository.findByCode(department.getCode()).isPresent()) {
            throw new BadRequestException("Department code already exists");
        }
        // Admin is assigned separately via assignAdmin() so it can be validated properly.
        department.setAdmin(null);
        return departmentRepository.save(department);
    }

    @Override
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    @Override
    public Department getDepartmentByCode(String code) {
        return departmentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with code: " + code));
    }

    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    @Transactional
    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = getDepartmentById(id);
        
        departmentRepository.findByName(departmentDetails.getName()).ifPresent(d -> {
            if (!d.getId().equals(id)) {
                throw new BadRequestException("Department name already exists");
            }
        });
        
        departmentRepository.findByCode(departmentDetails.getCode()).ifPresent(d -> {
            if (!d.getId().equals(id)) {
                throw new BadRequestException("Department code already exists");
            }
        });

        department.setName(departmentDetails.getName());
        department.setCode(departmentDetails.getCode());
        // Admin changes go through assignAdmin()/removeAdmin(), not a plain update.
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = getDepartmentById(id);
        departmentRepository.delete(department);
    }

    @Override
    @Transactional
    public Department assignAdmin(Long departmentId, Long userId) {
        Department department = getDepartmentById(departmentId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only a user with role ADMIN can be assigned as a department admin");
        }

        departmentRepository.findByAdminId(userId).ifPresent(existing -> {
            if (!existing.getId().equals(departmentId)) {
                throw new BadRequestException(
                        "User is already the admin of another department: " + existing.getName());
            }
        });

        // The department admin must belong to the department they administer.
        if (user.getDepartment() == null || !user.getDepartment().getId().equals(departmentId)) {
            user.setDepartment(department);
            userRepository.save(user);
        }

        department.setAdmin(user);
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public Department removeAdmin(Long departmentId) {
        Department department = getDepartmentById(departmentId);
        department.setAdmin(null);
        return departmentRepository.save(department);
    }
}
