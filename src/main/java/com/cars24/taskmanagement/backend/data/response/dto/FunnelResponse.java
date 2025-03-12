package com.cars24.taskmanagement.backend.data.response.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FunnelResponse {
    private String funnel;
    private List<TaskResponse> tasks;
}
