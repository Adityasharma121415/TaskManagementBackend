package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.data.response.RepeatedApiResponse;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;

import com.cars24.taskmanagement.backend.data.response.dto.FunnelResponse;
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
    public ApiResponse getTasksByApplicationId(@PathVariable String applicationId) {
        // Fetch tasks grouped by funnel
        Map<String, FunnelResponse> tasksByFunnel = applicationService.getTasksGroupedByFunnel(applicationId);

        // Wrap in response object
        TaskGroupedResponse responseData = new TaskGroupedResponse(tasksByFunnel);

        // Create API response
        ApiResponse response = new ApiResponse();
        response.setData(responseData);
        response.setService("TaskManagementService");
        response.setMessage("Tasks fetched successfully");
        response.setSuccess(true);
        response.setStatusCode(HttpStatus.OK.value());

        return response;
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


//    @GetMapping("/duration/{applicationId}")
//    public ResponseEntity<?> getLoanDuration(@PathVariable String applicationId) {
//        Optional<LoanDuration> loanDuration = loanDurationService.fetchLoanDuration(applicationId);
//        return loanDuration.map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }
}