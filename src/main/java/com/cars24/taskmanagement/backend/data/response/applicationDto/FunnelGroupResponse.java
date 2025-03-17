package com.cars24.taskmanagement.backend.data.response.applicationDto;


import lombok.Data;

import java.util.List;

@Data
public class FunnelGroupResponse {
        private String funnelName;
        private List<TaskDetailsResponse> tasks;



        public FunnelGroupResponse(String funnelName, List<TaskDetailsResponse> tasks) {
            this.funnelName = funnelName;
            this.tasks = tasks;
        }

    }
