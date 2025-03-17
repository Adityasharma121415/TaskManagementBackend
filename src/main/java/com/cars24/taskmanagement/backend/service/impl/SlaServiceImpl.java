package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.exceptions.SlaException;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements com.cars24.taskmanagement.backend.service.SlaService {

    private final SlaDaoImpl slaDao;



    public SlaResponse getSlaMetricsByChannel(String channel) {
        List<TaskExecutionTimeEntity> executions = slaDao.getTasksByChannel(channel);
        if (executions.isEmpty()) {
            throw new SlaException("No data found for channel: " + channel);
        }

        log.info("Received request for SLA service of channel: {}", channel);

        Map<String, List<Long>> taskDurations = new LinkedHashMap<>();
        Map<String, List<Long>> taskSendbacks = new LinkedHashMap<>();
        Map<String, Set<String>> funnelToTaskMapping = new LinkedHashMap<>();

        processExecutions(executions, taskDurations, taskSendbacks, funnelToTaskMapping);

        Map<String, String> avgTaskTimes = calculateAverageTimes(taskDurations);
        Map<String, String> avgFunnelTimes = calculateAverageFunnelTimes(funnelToTaskMapping, taskDurations);
        long totalTAT = calculateTotalTAT(avgFunnelTimes);
        Map<String, Long> sendbackCounts = calculateSendbackCounts(taskSendbacks);

        return buildSlaResponse(avgTaskTimes, avgFunnelTimes, sendbackCounts, totalTAT, funnelToTaskMapping);
    }

    private void processExecutions(List<TaskExecutionTimeEntity> executions,
                                   Map<String, List<Long>> taskDurations,
                                   Map<String, List<Long>> taskSendbacks,
                                   Map<String, Set<String>> funnelToTaskMapping) {
        for (TaskExecutionTimeEntity execution : executions) {
            Map<String, List<SubTaskEntity>> funnels = getFunnels(execution);
            funnels.forEach((funnelName, tasks) -> processTasks(tasks, funnelName, taskDurations, taskSendbacks, funnelToTaskMapping));
        }
    }

    private Map<String, List<SubTaskEntity>> getFunnels(TaskExecutionTimeEntity execution) {
        return Map.of(
                "sourcing", execution.getSourcing(),
                "credit", execution.getCredit(),
                "risk", execution.getRisk(),
                "conversion", execution.getConversion(),
                "rto", execution.getRto(),
                "fulfillment", execution.getFulfillment(),
                "disbursal", execution.getDisbursal()
        );
    }

    private void processTasks(List<SubTaskEntity> tasks, String funnelName,
                              Map<String, List<Long>> taskDurations,
                              Map<String, List<Long>> taskSendbacks,
                              Map<String, Set<String>> funnelToTaskMapping) {
        for (SubTaskEntity task : tasks) {
            taskDurations.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>()).add(task.getDuration());
            if (task.getSendbacks() >= 0) {
                taskSendbacks.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>()).add((long) task.getSendbacks());
            }
            funnelToTaskMapping.computeIfAbsent(funnelName, k -> new LinkedHashSet<>()).add(task.getTaskId());
        }
    }

    private Map<String, String> calculateAverageTimes(Map<String, List<Long>> taskDurations) {
        return taskDurations.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> SlaResponse.formatDuration(
                                (long) entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))));
    }

    private Map<String, String> calculateAverageFunnelTimes(Map<String, Set<String>> funnelToTaskMapping,
                                                            Map<String, List<Long>> taskDurations) {
        return funnelToTaskMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> SlaResponse.formatDuration(
                                (long) entry.getValue().stream()
                                        .mapToDouble(taskId -> taskDurations.getOrDefault(taskId, List.of(0L))
                                                .stream().mapToLong(Long::longValue).average().orElse(0.0))
                                        .sum())));
    }

    private long calculateTotalTAT(Map<String, String> avgFunnelTimes) {
        return (long) avgFunnelTimes.values().stream()
                .mapToDouble(this::convertFormattedTimeToMillis)
                .sum();
    }

    private long convertFormattedTimeToMillis(String time) {
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
    }

    private Map<String, Long> calculateSendbackCounts(Map<String, List<Long>> taskSendbacks) {
        return taskSendbacks.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Math.round(entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))));
    }

    private SlaResponse buildSlaResponse(Map<String, String> avgTaskTimes,
                                         Map<String, String> avgFunnelTimes,
                                         Map<String, Long> sendbackCounts,
                                         long totalTAT,
                                         Map<String, Set<String>> funnelToTaskMapping) {

        Map<String, SlaResponse.Funnel> funnels = initializeFunnels(avgFunnelTimes);
        populateFunnels(funnels, funnelToTaskMapping, avgTaskTimes, sendbackCounts);
        return new SlaResponse(funnels, SlaResponse.formatDuration(totalTAT));
    }

    private Map<String, SlaResponse.Funnel> initializeFunnels(Map<String, String> avgFunnelTimes) {
        Map<String, SlaResponse.Funnel> funnels = new LinkedHashMap<>();
        String[] funnelOrder = {"sourcing", "credit", "risk", "conversion", "rto", "fulfillment", "disbursal"};

        for (String funnelName : funnelOrder) {
            String funnelTime = avgFunnelTimes.getOrDefault(funnelName, SlaResponse.formatDuration(0));
            funnels.put(funnelName, new SlaResponse.Funnel(funnelTime, new LinkedHashMap<>()));
        }
        return funnels;
    }

    private void populateFunnels(Map<String, SlaResponse.Funnel> funnels,
                                 Map<String, Set<String>> funnelToTaskMapping,
                                 Map<String, String> avgTaskTimes,
                                 Map<String, Long> sendbackCounts) {
        String[] funnelOrder = {"sourcing", "credit", "risk", "conversion", "rto", "fulfillment", "disbursal"};

        for (String funnelName : funnelOrder) {
            SlaResponse.Funnel funnel = funnels.get(funnelName);
            Set<String> tasksForFunnel = funnelToTaskMapping.get(funnelName);
            if (tasksForFunnel != null) {
                for (String taskId : tasksForFunnel) {
                    String taskTime = avgTaskTimes.getOrDefault(taskId, SlaResponse.formatDuration(0));
                    Long noOfSendbacks = sendbackCounts.getOrDefault(taskId, 0L);
                    funnel.getTasks().put(taskId, new SlaResponse.Task(taskTime, noOfSendbacks));
                }
            }
        }
    }
}