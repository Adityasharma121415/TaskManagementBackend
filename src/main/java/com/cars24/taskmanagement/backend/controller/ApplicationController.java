package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskGroupedResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskResponse;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applicationLog")
@CrossOrigin(origins = "http://localhost:5173")
public class ApplicationController {

    private final ApplicationServiceImpl applicationService;

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
}

