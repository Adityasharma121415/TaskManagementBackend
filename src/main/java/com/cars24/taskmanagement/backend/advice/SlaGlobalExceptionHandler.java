package com.cars24.taskmanagement.backend.advice;

import com.cars24.taskmanagement.backend.data.exception.SlaResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class SlaGlobalExceptionHandler {

    @ExceptionHandler(SlaResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(SlaResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
