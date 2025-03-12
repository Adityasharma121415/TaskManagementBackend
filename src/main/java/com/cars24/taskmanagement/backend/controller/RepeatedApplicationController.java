package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.data.entity.LoanDuration;
import com.cars24.taskmanagement.backend.data.response.RepeatedApiResponse;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import com.cars24.taskmanagement.backend.service.LoanDurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/applicationLog")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5174/")
public class RepeatedApplicationController {
    @Autowired
    private ApplicationService applicationService;

    @GetMapping("/{applicationId}")
    public ResponseEntity<RepeatedApiResponse> getTasksByApplicationId(@PathVariable String applicationId) {
        TasksResponse tasksResponse = applicationService.getTasksByApplicationId(applicationId);

        RepeatedApiResponse response = new RepeatedApiResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setSuccess(true);
        response.setMessage("Tasks retrieved successfully");
        response.setService("APPUSER" + HttpStatus.OK.value());
        response.setData(tasksResponse);

        return ResponseEntity.ok(response);
    }
    @Autowired
    private LoanDurationService loanDurationService;
    @GetMapping("/duration/{applicationId}")


    public ResponseEntity<?> getLoanDuration(@PathVariable String applicationId) {
        Optional<LoanDuration> loanDuration = loanDurationService.fetchLoanDuration(applicationId);
        return loanDuration.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}


