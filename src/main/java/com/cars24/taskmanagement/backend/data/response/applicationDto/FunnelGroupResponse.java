package com.cars24.taskmanagement.backend.data.response.applicationDto;


import lombok.Data;

import java.util.List;

@Data
public class FunnelGroupResponse {
    private String funnelName;
    private List<TaskDetailsResponse> tasks;
    private long funnelDuration; // Add this field

    public FunnelGroupResponse(String funnelName, List<TaskDetailsResponse> tasks) {
        this.funnelName = funnelName;
        this.tasks = tasks;
        this.funnelDuration = 0; // Default value
    }

    // Add constructor with duration
    public FunnelGroupResponse(String funnelName, List<TaskDetailsResponse> tasks, long funnelDuration) {
        this.funnelName = funnelName;
        this.tasks = tasks;
        this.funnelDuration = funnelDuration;
    }
}