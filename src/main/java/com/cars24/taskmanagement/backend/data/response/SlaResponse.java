package com.cars24.taskmanagement.backend.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SlaResponse {
    private Map<String, Funnel> funnels;
    private String averageTAT;
    private Map<String, Distribution> tatDistribution;
    private Map<String, Map<String, Distribution>> taskDistributions;

    @Data
    @AllArgsConstructor
    public static class Funnel {
        private String timeTaken;
        private Map<String, Task> tasks;
    }

    @Data
    @AllArgsConstructor
    public static class Task {
        private String timeTaken;
        private long noOfSendbacks;
    }

    @Data
    @AllArgsConstructor
    public static class Distribution {
        private long count;
        private List<String> applicationIds;
        private Map<String, String> applicationStatusMap;
    }

    // Updated formatDuration method: always returns a non-empty string.
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0 sec";
        }
        long seconds = millis / 1000;
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) {
            formattedTime.append(days).append(" days ");
        }
        if (hours > 0) {
            formattedTime.append(hours).append(" hrs ");
        }
        if (minutes > 0) {
            formattedTime.append(minutes).append(" min ");
        }
        if (seconds > 0) {
            formattedTime.append(seconds).append(" sec");
        }
        return formattedTime.toString().trim();
    }
}
