package com.cars24.taskmanagement.backend.data.response.applicationDto;

import lombok.Data;

import java.util.List;

@Data
public class ListFunnelGroupResponse {
    private List<FunnelGroupResponse> funnelGroupResponses;

    public List<FunnelGroupResponse> getFunnelGroupResponses() {
        return funnelGroupResponses;
    }

    public void setFunnelGroupResponses(List<FunnelGroupResponse> funnelGroupResponses) {
        this.funnelGroupResponses = funnelGroupResponses;
    }

    public ListFunnelGroupResponse(List<FunnelGroupResponse> funnelGroupResponses) {
        this.funnelGroupResponses = funnelGroupResponses;
    }


}
