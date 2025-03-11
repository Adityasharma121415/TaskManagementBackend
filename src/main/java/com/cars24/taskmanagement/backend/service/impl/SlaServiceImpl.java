package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.exceptions.SlaException;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.service.SlaService;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlaServiceImpl implements SlaService {

    private final SlaDaoImpl slaDao;

    public SlaServiceImpl(SlaDaoImpl slaDao) {
        this.slaDao = slaDao;
    }

    public SlaResponse getSlaMetricsByChannel(String channel) {
        List<TaskExecutionTimeEntity> executions = slaDao.getTasksByChannel(channel);
        if (executions.isEmpty()) {
            throw new SlaException("No data found for channel: " + channel);
        }

        Map<String, List<Long>> taskDurations = new HashMap<>();
        Map<String, List<Long>> taskSendbacks = new HashMap<>();
        Map<String, Set<String>> funnelToTaskMapping = new HashMap<>();

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
                    funnelToTaskMapping.computeIfAbsent(funnelName, k -> new HashSet<>()).add(task.getTaskId());
                }
            });
        }


        Map<String, String> avgTaskTimes = taskDurations.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> SlaResponse.formatDuration(
                                (long) entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))));


        Map<String, String> avgFunnelTimes = funnelToTaskMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> SlaResponse.formatDuration(
                                (long) entry.getValue().stream()
                                        .mapToDouble(taskId -> taskDurations.getOrDefault(taskId, List.of(0L)).stream().mapToLong(Long::longValue).average().orElse(0.0))
                                        .sum())));


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


        Map<String, Long> sendbackCounts = taskSendbacks.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Math.round(entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))));

        return new SlaResponse(avgFunnelTimes, avgTaskTimes, sendbackCounts, SlaResponse.formatDuration(totalTAT));
    }
}
