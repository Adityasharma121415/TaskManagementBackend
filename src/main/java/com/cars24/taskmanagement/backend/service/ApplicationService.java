package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.response.applicationDto.ListFunnelGroupResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface ApplicationService {

    Map<String, Object> getTasksGroupedByFunnel(String applicationId);

//    ListFunnelGroupResponse getTasksByApplicationId(String applicationId);
}