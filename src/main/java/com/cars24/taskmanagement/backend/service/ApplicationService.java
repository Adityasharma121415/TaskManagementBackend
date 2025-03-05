package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.response.TasksResponse;

public interface ApplicationService {
    TasksResponse getTasksByApplicationId(String applicationId);
}