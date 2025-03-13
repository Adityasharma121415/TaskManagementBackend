package com.cars24.taskmanagement.backend.data.response.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TaskGroupedResponse {
    private Map<String, FunnelResponse> tasksByFunnel;

    public TaskGroupedResponse(Map<String, FunnelResponse> tasksByFunnel) {
        this.tasksByFunnel = tasksByFunnel;
    }

    public Map<String, FunnelResponse> getTasksByFunnel() {
        return tasksByFunnel;
    }

    public void setTasksByFunnel(Map<String, FunnelResponse> tasksByFunnel) {
        this.tasksByFunnel = tasksByFunnel;
    }
}
