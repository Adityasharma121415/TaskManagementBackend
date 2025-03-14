package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.data.response.dto.FunnelResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ApplicationService {

    public Map<String, FunnelResponse> getTasksGroupedByFunnel(String applicationId);


    TasksResponse getTasksByApplicationId(String applicationId);
}