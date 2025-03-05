package com.cars24.taskmanagement.backend.data.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data



public class TasksResponse {
    private List<FunnelGroup> funnelGroups;

    // Default constructor
    public TasksResponse() {
    }

    // Constructor with parameters
    public TasksResponse(List<FunnelGroup> funnelGroups) {
        this.funnelGroups = funnelGroups;
    }

    // Getters and setters
    public List<FunnelGroup> getFunnelGroups() {
        return funnelGroups;
    }

    public void setFunnelGroups(List<FunnelGroup> funnelGroups) {
        this.funnelGroups = funnelGroups;
    }
}
