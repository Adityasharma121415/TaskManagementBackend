package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applicationLog")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173/")
public class ApplicationController {


    private final ApplicationServiceImpl taskExecutionService;

    @GetMapping("/{applicationId}")
    public Map<String, List<Map<String, Object>>> getTasksByApplicationId(@PathVariable String applicationId) {
        return taskExecutionService.getTasksGroupedByFunnel(applicationId);
    }
}


