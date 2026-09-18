package com.infosys.procurementsystem.dto.auth;

import jakarta.validation.constraints.NotBlank;
import com.infosys.procurementsystem.enums.Role;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    // Selected account type from the login screen. Optional for backward compatibility
    // with existing Postman/API clients.
    private Role role;
}
