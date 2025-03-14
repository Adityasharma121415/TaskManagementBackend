package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.data.response.RepeatedApiResponse;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;

import com.cars24.taskmanagement.backend.data.response.dto.TaskGroupedResponse;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
//@RequiredArgsConstructor
@RequestMapping("/applicationLog")
@CrossOrigin(origins = "http://localhost:5173")
public class ApplicationController {

    @Autowired
    ApplicationServiceImpl applicationService;

    //private LoanDurationService loanDurationService;


    @GetMapping("/{applicationId}")
    public Map<String, Object> getTasksByApplicationId(@PathVariable String applicationId) {

        return applicationService.getTasksGroupedByFunnel(applicationId);
    }

    @GetMapping("/graph/{applicationId}")
    public ResponseEntity<RepeatedApiResponse> getTasksByApplicationIdDC(@PathVariable String applicationId) {
        TasksResponse tasksResponse = applicationService.getTasksByApplicationId(applicationId);
        RepeatedApiResponse response = new RepeatedApiResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setSuccess(true);
        response.setMessage("Tasks retrieved successfully");
        response.setService("APPUSER" + HttpStatus.OK.value());
        response.setData(tasksResponse);

        return ResponseEntity.ok(response);
    }
}