package com.cars24.taskmanagement.backend.data.response.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data

public class FunnelResponse {
    private String funnel;
    private int funnelDuration;  // Added this field
    private List<TaskResponse> tasks;

    // Constructor
    public FunnelResponse(String funnel, int funnelDuration, List<TaskResponse> tasks) {
        this.funnel = funnel;
        this.funnelDuration = funnelDuration;
        this.tasks = tasks;
    }

    // Getters and Setters
    public String getFunnel() {
        return funnel;
    }

    public void setFunnel(String funnel) {
        this.funnel = funnel;
    }

    public int getFunnelDuration() {
        return funnelDuration;
    }

    public void setFunnelDuration(int funnelDuration) {
        this.funnelDuration = funnelDuration;
    }

    public List<TaskResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskResponse> tasks) {
        this.tasks = tasks;
    }
}
