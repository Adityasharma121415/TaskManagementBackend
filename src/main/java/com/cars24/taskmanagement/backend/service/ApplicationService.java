package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ApplicationService {
    /**
     * Retrieves tasks grouped by funnel for a given application ID.
     * Each task includes duration and sendbacks information if available.
     *
     * @param applicationId The ID of the application
     * @return A map of funnel names to lists of task responses
     */
    Map<String, List<TaskResponse>> getTasksGroupedByFunnel(String applicationId);

    /**
     * Retrieves tasks for a given application ID, organized for graph display.
     *
     * @param applicationId The ID of the application
     * @return A TasksResponse containing funnel groups with task details
     */
    TasksResponse getTasksByApplicationId(String applicationId);
}