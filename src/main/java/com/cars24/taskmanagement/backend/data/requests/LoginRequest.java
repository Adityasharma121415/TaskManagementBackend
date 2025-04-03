package com.cars24.taskmanagement.backend.data.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@cars24\\.com$", message = "Email must end with @cars24.com")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}