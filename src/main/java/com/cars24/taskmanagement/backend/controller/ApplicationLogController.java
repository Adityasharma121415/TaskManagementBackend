package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.entity.ApplicationLog;
import com.cars24.taskmanagement.backend.service.ApplicationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApplicationLogController {

    @Autowired
    private ApplicationLogService applicationLogService;

    @GetMapping("/applicationLog/{applicationId}")
    public ResponseEntity<List<ApplicationLog>> getApplicationLogs(@PathVariable String applicationId) {
        List<ApplicationLog> logs = applicationLogService.getLogsByApplicationId(applicationId);
        return ResponseEntity.ok(logs);
    }
}