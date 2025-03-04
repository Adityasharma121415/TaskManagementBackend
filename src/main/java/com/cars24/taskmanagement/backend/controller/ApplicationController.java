package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;

    @GetMapping("/{applicationId}/tasks")
    public List<Application> getSortedApplications(@PathVariable String applicationId) {
        return applicationService.getSortedApplications(applicationId);
    }
}
