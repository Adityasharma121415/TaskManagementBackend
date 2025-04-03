package com.cars24.taskmanagement.backend.data.requests;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SignUpRequest {

    private String name;
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@cars24\\.com$", message = "Email must end with @cars24.com")
    private String email;
    private String password;
}