package com.cars24.taskmanagement.backend.controller;


import com.cars24.taskmanagement.backend.data.response.ApplicationTasksResponse;
import com.cars24.taskmanagement.backend.data.response.FunnelGroup;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applicationLog")
@CrossOrigin(origins = "http://localhost:5174")
public class ApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);
    private final ApplicationService applicationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping(value = "/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<ApplicationTasksResponse> getTasksByApplicationId(
            @PathVariable String applicationId) {
        logger.info("Received request to fetch tasks for application ID: {}", applicationId);
        return applicationService.getTasksGroupedByFunnel(applicationId);
    }
}
