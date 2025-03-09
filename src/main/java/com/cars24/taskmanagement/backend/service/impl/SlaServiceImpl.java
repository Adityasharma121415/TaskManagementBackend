package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlaServiceImpl {

    private final SlaDaoImpl slaDao;

    public SlaServiceImpl(SlaDaoImpl slaDao) {
        this.slaDao = slaDao;
    }

    public SlaResponse getSlaMetricsByChannel(String channel) {
        List<TaskExecutionTimeEntity> executions = slaDao.getTasksByChannel(channel);
        if (executions.isEmpty()) {
            throw new RuntimeException("No data found for channel: " + channel);
        }

        Map<String, List<Long>> funnelDurations = new HashMap<>();
        Map<String, List<Long>> taskDurations = new HashMap<>();
        Map<String, Long> sendbackCounts = new HashMap<>();

        for (TaskExecutionTimeEntity execution : executions) {
            Map<String, List<SubTaskEntity>> funnels = Map.of(
                    "sourcing", execution.getSourcing(),
                    "credit", execution.getCredit(),
                    "conversion", execution.getConversion(),
                    "fulfillment", execution.getFulfillment()
            );

            funnels.forEach((funnelName, tasks) -> {
                long totalDuration = tasks.stream().mapToLong(SubTaskEntity::getDuration).sum();
                funnelDurations.computeIfAbsent(funnelName, k -> new ArrayList<>()).add(totalDuration);

                for (SubTaskEntity task : tasks) {
                    taskDurations.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>()).add(task.getDuration());
                    sendbackCounts.merge(task.getTaskId(), (long) task.getSendbacks(), Long::sum);
                }
            });
        }

        Map<String, Double> avgFunnelTimes = funnelDurations.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0)));

        Map<String, Double> avgTaskTimes = taskDurations.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0)));

        return new SlaResponse(avgFunnelTimes, avgTaskTimes, sendbackCounts);
    }
}

