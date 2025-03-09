package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlaServiceImpl {

    private final TaskExecutionTimeRepository repository;

    public SlaResponse getSlaMonitoring(String channel) {
        // Fetch aggregated task execution data
        List<Map<String, Object>> aggregatedData = repository.getAggregatedTaskExecutionByChannel(channel);

        // Process task data per funnel
        Map<String, List<SlaResponse.TaskData>> funnelTasks = new HashMap<>();
        double totalFunnelTime = 0.0;
        int funnelCount = 0;

        for (Map<String, Object> task : aggregatedData) {
            String taskName = (String) task.get("taskId");

            // Skip null tasks
            if (taskName == null) {
                continue;
            }

            // Handle potential null values for numeric fields with safe defaults
            double timeTaken = task.get("timeTaken") != null ? (Double) task.get("timeTaken") : 0.0;
            double sendbacks = task.get("sendbacks") != null ? (Double) task.get("sendbacks") : 0.0;

            String formattedTime = String.format("%.2f hrs", timeTaken);
            String formattedSendbacks = String.format("%.2f", sendbacks);

            // Determine funnel based on task name
            String funnelName = determineFunnelFromTask(taskName);

            funnelTasks.computeIfAbsent(funnelName, k -> new ArrayList<>())
                    .add(new SlaResponse.TaskData(taskName, formattedTime, formattedSendbacks));

            totalFunnelTime += timeTaken;
        }

        // Convert funnelTasks map to list of FunnelData
        List<SlaResponse.FunnelData> funnels = funnelTasks.entrySet().stream()
                .map(entry -> {
                    double funnelTime = entry.getValue().stream()
                            .mapToDouble(task -> {
                                try {
                                    return Double.parseDouble(task.getTimeTaken().split(" ")[0]);
                                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                                    return 0.0; // Safe default if parsing fails
                                }
                            })
                            .sum();
                    return new SlaResponse.FunnelData(entry.getKey(), String.format("%.2f hrs", funnelTime), entry.getValue());
                })
                .collect(Collectors.toList());

        funnelCount = funnels.size();

        // Calculate Average Turnaround Time (TAT)
        String averageTAT = funnelCount > 0 ? String.format("%.2f hrs", totalFunnelTime / funnelCount) : "0 hrs";

        return new SlaResponse(funnels, averageTAT);
    }

    // Fixed method to handle null task names
    private String determineFunnelFromTask(String taskName) {
        if (taskName == null) {
            return "Unknown"; // Default category for null task names
        }

        if (taskName.toLowerCase().contains("source")) {
            return "Sourcing";
        } else if (taskName.toLowerCase().contains("credit")) {
            return "Credit";
        } else if (taskName.toLowerCase().contains("conversion")) {
            return "Conversion";
        } else {
            return "Disbursement";
        }
    }
}