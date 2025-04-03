package com.cars24.taskmanagement.backend.controller;

import com.cars24.taskmanagement.backend.data.response.ApiResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.ListFunnelGroupResponse;
import com.cars24.taskmanagement.backend.service.impl.ApplicationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/applicationLog")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationServiceImpl applicationService;

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse> getTasksByApplicationId(@PathVariable String applicationId) {
        Map<String, Object> tasksByApplicationId= applicationService.getTasksGroupedByFunnel(applicationId);
        ApiResponse response = new ApiResponse();
        response.setStatusCode(HttpStatus.OK.value());
        response.setSuccess(true);
        response.setMessage("Tasks retrieved successfully");
        response.setService("APPUSER" + HttpStatus.OK.value());
        response.setData(tasksByApplicationId);
        return ResponseEntity.ok().body(response);
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

        return ResponseEntity.ok().body(response);
    }
}