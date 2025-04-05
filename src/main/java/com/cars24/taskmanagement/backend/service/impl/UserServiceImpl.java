package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.entity.UserEntity;
import com.cars24.taskmanagement.backend.data.repository.UserRepository;
import com.cars24.taskmanagement.backend.data.requests.LoginRequest;
import com.cars24.taskmanagement.backend.data.requests.SignUpRequest;
import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.service.UserService;
import com.cars24.taskmanagement.backend.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
   private final JwtUtil jwtUtil;

    @Override
    public ApiResponse registerUser(SignUpRequest user) {
        Optional<UserEntity> userExists = userRepository.findByEmail(user.getEmail());

        if (userExists.isPresent()) {
            throw new RuntimeException("User Already Exists!!");
        }
        String encodedPassword = passwordEncoder.encode(user.getPassword());

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(user.getEmail());
        userEntity.setPassword(encodedPassword);
        userEntity.setName(user.getName());


        userRepository.save(userEntity);

        return new ApiResponse(
                HttpStatus.OK.value(),
                "User registered successfully! Please Login",
                "APPUSER",
                true,
                null
        );
    }

    @Override
    public ApiResponse login(LoginRequest user) {
        Optional<UserEntity> userExists = userRepository.findByEmail(user.getEmail());

        if (userExists.isPresent()) {
            UserEntity userEntity = userExists.get();
            if (passwordEncoder.matches(user.getPassword(), userEntity.getPassword())) {
                String id = String.valueOf(userEntity.getId());
                String token = jwtUtil.generateToken(user.getEmail(), id);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("token", token);
                responseData.put("id", id);
                return new ApiResponse(
                        HttpStatus.OK.value(),
                        "User Logged in Successfully",
                        "APPUSER",
                        true,
                        responseData
                );
            } else {
                throw new RuntimeException("Invalid Password");
            }
        }
        throw new RuntimeException("User doesn't exist. Please SignUp");
    }
}