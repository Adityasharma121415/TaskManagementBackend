package com.cars24.taskmanagement.backend.advice;

import com.cars24.taskmanagement.backend.data.response.RepeatedApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RepeatedApiResponse> handleRunTimeExceptions(RuntimeException exception){

        RepeatedApiResponse response = new RepeatedApiResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());;
        response.setSuccess(false);
        response.setMessage(exception.getMessage());

        response.setService("APPUSER"+HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(response);

    }
}