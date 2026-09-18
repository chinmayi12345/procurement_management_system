package com.infosys.procurementsystem.controller;

import com.infosys.procurementsystem.common.ApiResponse;
import com.infosys.procurementsystem.entity.Department;
import com.infosys.procurementsystem.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Department>> createDepartment(@Valid @RequestBody Department department) {
        Department created = departmentService.createDepartment(department);
        return new ResponseEntity<>(new ApiResponse<>(true, "Department created successfully", created), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> getDepartmentById(@PathVariable Long id) {
        Department dept = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department retrieved successfully", dept));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<Department>> getDepartmentByCode(@PathVariable String code) {
        Department dept = departmentService.getDepartmentByCode(code);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department retrieved successfully", dept));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Department>>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(new ApiResponse<>(true, "Departments retrieved successfully", departments));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> updateDepartment(@PathVariable Long id, @Valid @RequestBody Department departmentDetails) {
        Department updated = departmentService.updateDepartment(id, departmentDetails);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department deleted successfully"));
    }

    @PutMapping("/{id}/admin/{userId}")
    public ResponseEntity<ApiResponse<Department>> assignAdmin(@PathVariable Long id, @PathVariable Long userId) {
        Department updated = departmentService.assignAdmin(id, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department admin assigned successfully", updated));
    }

    @DeleteMapping("/{id}/admin")
    public ResponseEntity<ApiResponse<Department>> removeAdmin(@PathVariable Long id) {
        Department updated = departmentService.removeAdmin(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department admin removed successfully", updated));
    }
}
