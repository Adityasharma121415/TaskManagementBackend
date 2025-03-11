package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.exceptions.SlaException;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlaServiceImpl implements com.cars24.taskmanagement.backend.service.SlaService {

    private final SlaDaoImpl slaDao;

    public SlaServiceImpl(SlaDaoImpl slaDao) {
        this.slaDao = slaDao;
    }

    public SlaResponse getSlaMetricsByChannel(String channel) {
        List<TaskExecutionTimeEntity> executions = slaDao.getTasksByChannel(channel);
        if (executions.isEmpty()) {
            throw new SlaException("No data found for channel: " + channel);
        }

        // Use LinkedHashMap to preserve insertion order if needed.
        Map<String, List<Long>> taskDurations = new LinkedHashMap<>();
        Map<String, List<Long>> taskSendbacks = new LinkedHashMap<>();
        // Use LinkedHashSet to preserve task order for each funnel.
        Map<String, Set<String>> funnelToTaskMapping = new LinkedHashMap<>();

        for (TaskExecutionTimeEntity execution : executions) {
            Map<String, List<SubTaskEntity>> funnels = Map.of(
                    "sourcing", execution.getSourcing(),
                    "credit", execution.getCredit(),
                    "conversion", execution.getConversion(),
                    "fulfillment", execution.getFulfillment()
            );

            funnels.forEach((funnelName, tasks) -> {
                for (SubTaskEntity task : tasks) {
                    taskDurations.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>()).add(task.getDuration());
                    taskSendbacks.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>()).add((long) task.getSendbacks());
                    // Use LinkedHashSet here to preserve the insertion order.
                    funnelToTaskMapping.computeIfAbsent(funnelName, k -> new LinkedHashSet<>()).add(task.getTaskId());
                }
            });
        }

        // Calculate average time per task using the detailed format.
        Map<String, String> avgTaskTimes = taskDurations.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> SlaResponse.formatDuration(
                                (long) entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))));

        // Calculate average time per funnel by summing the average durations of tasks in that funnel.
        Map<String, String> avgFunnelTimes = funnelToTaskMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> SlaResponse.formatDuration(
                                (long) entry.getValue().stream()
                                        .mapToDouble(taskId -> taskDurations.getOrDefault(taskId, List.of(0L))
                                                .stream().mapToLong(Long::longValue).average().orElse(0.0))
                                        .sum())));

        // Compute overall TAT from funnel durations.
        long totalTAT = (long) avgFunnelTimes.values().stream()
                .mapToDouble(time -> {
                    String[] parts = time.split(" ");
                    long totalMillis = 0;
                    for (int i = 0; i < parts.length; i += 2) {
                        long num = Long.parseLong(parts[i]);
                        switch (parts[i + 1]) {
                            case "days" -> totalMillis += num * 24 * 3600 * 1000;
                            case "hrs" -> totalMillis += num * 3600 * 1000;
                            case "min" -> totalMillis += num * 60 * 1000;
                            case "sec" -> totalMillis += num * 1000;
                        }
                    }
                    return totalMillis;
                }).sum();

        // Calculate average sendbacks per task.
        Map<String, Long> sendbackCounts = taskSendbacks.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Math.round(entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))));

        // Build nested "funnels" structure in the required order.
        Map<String, SlaResponse.Funnel> funnels = new LinkedHashMap<>();
        String[] funnelOrder = {"sourcing", "credit", "conversion", "fulfillment"};
        for (String funnelName : funnelOrder) {
            String funnelTime = avgFunnelTimes.getOrDefault(funnelName, SlaResponse.formatDuration(0));
            funnels.put(funnelName, new SlaResponse.Funnel(funnelTime, new LinkedHashMap<>()));
        }

        // Populate tasks for each funnel in the preserved order using the actual taskId.
        for (String funnelName : funnelOrder) {
            SlaResponse.Funnel funnel = funnels.get(funnelName);
            Set<String> tasksForFunnel = funnelToTaskMapping.get(funnelName);
            if (tasksForFunnel != null) {
                for (String taskId : tasksForFunnel) {
                    String taskTime = avgTaskTimes.get(taskId);
                    Long noOfSendbacks = sendbackCounts.get(taskId);
                    if (taskTime == null) {
                        taskTime = SlaResponse.formatDuration(0);
                    }
                    if (noOfSendbacks == null) {
                        noOfSendbacks = 0L;
                    }
                    // Use the actual taskId (e.g., "sourcing_task1") as the key.
                    funnel.getTasks().put(taskId, new SlaResponse.Task(taskTime, noOfSendbacks));
                }
            }
        }

        return new SlaResponse(funnels, SlaResponse.formatDuration(totalTAT));
    }
}
