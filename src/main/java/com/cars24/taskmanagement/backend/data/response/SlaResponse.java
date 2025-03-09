package com.cars24.taskmanagement.backend.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class SlaResponse {
    private List<FunnelData> funnels;
    private String averageTAT;

    @Data
    @AllArgsConstructor
    public static class FunnelData {
        private String funnelName;
        private String timeTaken;
        private List<TaskData> tasks;
    }

    @Data
    @AllArgsConstructor
    public static class TaskData {
        private String taskName;
        private String timeTaken;
        private String sendbacks;
    }
}
