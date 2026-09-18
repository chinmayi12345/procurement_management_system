package com.infosys.procurementsystem.dto.auth;

import com.infosys.procurementsystem.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String fullname;
    private Role role;
    private Long supplierId;
}
