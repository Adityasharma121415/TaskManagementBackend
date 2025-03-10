package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
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
            throw new RuntimeException("No data found for channel: " + channel);
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

        // Compute average task times
        Map<String, Double> avgTaskTimes = taskDurations.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0)));

        // Compute funnel times by summing average task times
        Map<String, Double> avgFunnelTimes = funnelToTaskMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .mapToDouble(taskId -> avgTaskTimes.getOrDefault(taskId, 0.0))
                                .sum()));

        // Compute `averageTAT` as the sum of all funnel times
        double averageTAT = avgFunnelTimes.values().stream().mapToDouble(Double::doubleValue).sum();

        // Compute average sendbacks and round to the nearest integer
        Map<String, Long> sendbackCounts = taskSendbacks.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Math.round(entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))));

        return new SlaResponse(avgFunnelTimes, avgTaskTimes, sendbackCounts, averageTAT);
    }
}
