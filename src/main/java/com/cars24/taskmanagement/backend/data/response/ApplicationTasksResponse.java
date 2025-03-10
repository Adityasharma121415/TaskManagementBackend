package com.cars24.taskmanagement.backend.data.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data

public class ApplicationTasksResponse {
    private List<FunnelGroup> funnels;
    private boolean success;
    private String error;

    // Constructor for success case
    public ApplicationTasksResponse(List<FunnelGroup> funnels) {
        this.funnels = funnels;
        this.success = true;
    }

    // Constructor for error case
    public ApplicationTasksResponse(String errorMessage) {
        this.error = errorMessage;
        this.success = false;
    }
}

