package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.requests.LoginRequest;
import com.cars24.taskmanagement.backend.data.requests.SignUpRequest;
import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    ApiResponse registerUser(SignUpRequest user);
    ApiResponse login(LoginRequest user);
}
