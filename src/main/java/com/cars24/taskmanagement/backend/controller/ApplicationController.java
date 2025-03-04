package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ApplicationServiceImpl taskExecutionService;

    @GetMapping("/byApplicationId/{applicationId}")
    public Map<String, List<Map<String, Object>>> getTasksByApplicationId(@PathVariable String applicationId) {
        return taskExecutionService.getTasksGroupedByFunnel(applicationId);
    }
}


