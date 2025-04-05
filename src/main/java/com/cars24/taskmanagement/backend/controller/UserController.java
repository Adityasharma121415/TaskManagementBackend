package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.requests.LoginRequest;
import com.cars24.taskmanagement.backend.data.requests.SignUpRequest;
import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.service.UserService;
//import com.cars24.taskmanagement.backend.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private static final String APPUSER = "UserController";
    private static final String TOKEN_KEY = "token";

  //  private final JwtUtil jwtUtil;
    private final UserService userService;


    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signUp(@Valid @RequestBody SignUpRequest user) {
        try{
            ApiResponse response = userService.registerUser(user);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            ApiResponse response = new ApiResponse(
                    HttpStatus.CONFLICT.value(),
                    e.getMessage(),
                    APPUSER,
                    false,
                    null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest user, HttpServletResponse httpServletResponse) {
        try {
            ApiResponse response = userService.login(user);

            Map<String, Object> dataMap = (Map<String, Object>) response.getData();
            String token = (String) dataMap.get(TOKEN_KEY);
            dataMap.remove(TOKEN_KEY);

            // Create an HttpOnly cookie to store the token.
            Cookie cookie = new Cookie(TOKEN_KEY, token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(86400); // Token expiration: 86400 seconds = 1 day
            httpServletResponse.addCookie(cookie);//Attaches the cookie to the HTTP response, sending it to the client.
            response.setData(dataMap);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            ApiResponse response = new ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(),
                    APPUSER,
                    false,
                    null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
