package com.cars24.taskmanagement.backend.data.response.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TaskGroupedResponse {
    public TaskGroupedResponse(Map<String, List<TaskResponse>> tasksByFunnel) {
        this.tasksByFunnel = tasksByFunnel;
    }

    public Map<String, List<TaskResponse>> getTasksByFunnel() {
        return tasksByFunnel;
    }

    public void setTasksByFunnel(Map<String, List<TaskResponse>> tasksByFunnel) {
        this.tasksByFunnel = tasksByFunnel;
    }

    private Map<String, List<TaskResponse>> tasksByFunnel;
}