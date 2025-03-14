package com.cars24.taskmanagement.backend.data.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class TasksResponse {
    private List<FunnelGroup> funnelGroups;


    public TasksResponse() {
    }

    public TasksResponse(List<FunnelGroup> funnelGroups) {
        this.funnelGroups = funnelGroups;
    }
    
}
