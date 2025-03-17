package com.cars24.taskmanagement.backend.data.response.applicationDto;

import lombok.Data;

import java.util.List;

@Data
public class ListFunnelGroupResponse {
    private List<FunnelGroupResponse> funnelGroupResponses;

    public ListFunnelGroupResponse(List<FunnelGroupResponse> funnelGroupResponses) {
        this.funnelGroupResponses = funnelGroupResponses;
    }
    
}
