package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class ApplicationController {
    @Autowired
    private  ApplicationService applicationService;

    @GetMapping("/byApplicationId/{applicationId}")
    public ResponseEntity<ApiResponse> getTasksByApplicationId(@PathVariable String applicationId) {
        TasksResponse tasksResponse = applicationService.getTasksByApplicationId(applicationId);

        ApiResponse response = new ApiResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setSuccess(true);
        response.setMessage("Tasks retrieved successfully");
        response.setService("APPUSER" + HttpStatus.OK.value());
        response.setData(tasksResponse);

        return ResponseEntity.ok(response);
    }
}


