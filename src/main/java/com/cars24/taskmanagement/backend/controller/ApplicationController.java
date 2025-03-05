package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applicationLog")
@CrossOrigin(origins = "http://localhost:5173")
public class ApplicationController {

    private ApplicationServiceImpl applicationService;

    // Add explicit constructor for dependency injection
    @Autowired
    public ApplicationController(ApplicationServiceImpl applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{applicationId}")
    public Map<String, List<Map<String, Object>>> getTasksByApplicationId(@PathVariable String applicationId) {
        return applicationService.getTasksGroupedByFunnel(applicationId);
    }
}

