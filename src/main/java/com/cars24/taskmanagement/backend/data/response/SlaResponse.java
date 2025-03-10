package com.cars24.taskmanagement.backend.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class SlaResponse {
    private Map<String, String> averageTimePerFunnel;
    private Map<String, String> averageTimePerTask;
    private Map<String, Long> totalSendbacksPerTask;
    private String averageTAT;


    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) formattedTime.append(days).append(" days ");
        if (hours > 0) formattedTime.append(hours).append(" hrs ");
        if (minutes > 0) formattedTime.append(minutes).append(" min ");
        if (seconds > 0) formattedTime.append(seconds).append(" sec");

        return formattedTime.toString().trim();
    }
}
