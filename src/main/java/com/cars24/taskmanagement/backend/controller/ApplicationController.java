package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.data.entity.LoanDuration;
import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.data.response.RepeatedApiResponse;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskGroupedResponse;
import com.cars24.taskmanagement.backend.service.LoanDurationService;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/applicationLog")
@CrossOrigin(origins = "http://localhost:5173")
public class ApplicationController {

    private final ApplicationServiceImpl applicationService;

    private LoanDurationService loanDurationService;

    @Autowired
    public ApplicationController(ApplicationServiceImpl applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{applicationId}")
    public ApiResponse getTasksByApplicationId(@PathVariable String applicationId) {
        TaskGroupedResponse responseData = new TaskGroupedResponse(applicationService.getTasksGroupedByFunnel(applicationId));
        return new ApiResponse(
                HttpStatus.OK.value(),
                "Tasks fetched successfully",
                "TaskManagementService",
                true,
                responseData
        );
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


    @GetMapping("/duration/{applicationId}")
    public ResponseEntity<?> getLoanDuration(@PathVariable String applicationId) {
        Optional<LoanDuration> loanDuration = loanDurationService.fetchLoanDuration(applicationId);
        return loanDuration.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

