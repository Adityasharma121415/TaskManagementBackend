package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.ListFunnelGroupResponse;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/applicationLog")
@CrossOrigin(origins = "http://localhost:5173")
public class ApplicationController {

    @Autowired
    ApplicationServiceImpl applicationService;


    @GetMapping("/{applicationId}")
    public Map<String, Object> getTasksByApplicationId(@PathVariable String applicationId) {

        return applicationService.getTasksGroupedByFunnel(applicationId);
    }

    @GetMapping("/graph/{applicationId}")
    public ResponseEntity<ApiResponse> getTasksByApplicationIdDC(@PathVariable String applicationId) {
        ListFunnelGroupResponse listFunnelGroupResponse = applicationService.getTasksByApplicationId(applicationId);
        ApiResponse response = new ApiResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setSuccess(true);
        response.setMessage("Tasks retrieved successfully");
        response.setService("APPUSER" + HttpStatus.OK.value());
        response.setData(listFunnelGroupResponse);

        return ResponseEntity.ok(response);
    }
}