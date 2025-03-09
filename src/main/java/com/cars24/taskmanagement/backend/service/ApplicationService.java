package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.response.ApplicationTasksResponse;
import com.cars24.taskmanagement.backend.data.response.FunnelGroup;
import org.springframework.http.ResponseEntity;

import java.util.List;
public interface ApplicationService {
    /**
     * Retrieves tasks grouped by funnel for a given application ID
     *
     * @param applicationId the application identifier
     * @return ResponseEntity containing the funnel groups or error response
     */
    ResponseEntity<ApplicationTasksResponse> getTasksGroupedByFunnel(String applicationId);
}