package com.cars24.taskmanagement.backend.data.response.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class TaskGroupedResponse {
    private Map<String, List<TaskResponse>> tasksByFunnel;
}