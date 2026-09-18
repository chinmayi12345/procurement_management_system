package com.infosys.procurementsystem.controller;

import com.infosys.procurementsystem.common.ApiResponse;
import com.infosys.procurementsystem.dto.auth.AuthResponse;
import com.infosys.procurementsystem.dto.auth.LoginRequest;
import com.infosys.procurementsystem.dto.auth.RegisterRequest;
import com.infosys.procurementsystem.entity.Department;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.UserStatus;
import com.infosys.procurementsystem.security.JwtUtil;
import com.infosys.procurementsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.infosys.procurementsystem.enums.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setSupplierId(request.getSupplierId());

        if (request.getDepartmentId() != null) {
            Department department = new Department();
            department.setId(request.getDepartmentId());
            user.setDepartment(department);
        }

        // userService.createUser() already checks for duplicate username/email
        // and encodes the password before saving.
        User created = userService.createUser(user);

        String token = jwtUtil.generateToken(created);
        AuthResponse authResponse = new AuthResponse(token, created.getUsername(), created.getFullName(), created.getRole(), created.getSupplierId());

        return new ResponseEntity<>(new ApiResponse<>(true, "User registered successfully", authResponse), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            return new ResponseEntity<>(new ApiResponse<>(false, "Invalid username or password"), HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = userService.loadUserByUsername(request.getUsername());
        User user = (User) userDetails;

        // The login screen has separate USER / ADMIN / SUPPLIER options.
        // Make sure the selected account type matches the actual database role.
        if (request.getRole() != null && user.getRole() != request.getRole()) {
            String actualRole = user.getRole() == Role.EMPLOYEE ? "USER" : user.getRole().name();
            return new ResponseEntity<>(
                    new ApiResponse<>(false,
                            "Account type mismatch. This username is registered as " + actualRole + ". Please select " + actualRole + " and sign in again."),
                    HttpStatus.FORBIDDEN);
        }

        if (user.getRole() == Role.SUPPLIER && user.getSupplierId() == null) {
            return new ResponseEntity<>(
                    new ApiResponse<>(false,
                            "This supplier account is not linked to a supplier record. Please contact the administrator."),
                    HttpStatus.FORBIDDEN);
        }

        String token = jwtUtil.generateToken(userDetails);
        AuthResponse authResponse = new AuthResponse(token, user.getUsername(), user.getFullName(), user.getRole(), user.getSupplierId());

        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", authResponse));
    }
}
