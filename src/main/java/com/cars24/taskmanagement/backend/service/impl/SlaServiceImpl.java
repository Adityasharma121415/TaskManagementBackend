package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.exceptions.SlaException;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.service.SlaService;
import com.cars24.taskmanagement.backend.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaDaoImpl slaDao;

    // Existing method for backward compatibility.
    @Override
    public SlaResponse getSlaMetricsByChannel(String channel, Integer days, String appStatusFilter) {
        return getSlaMetricsByChannel(channel, days, appStatusFilter, "ALL");
    }

    // New overloaded method that applies filtering for task status as well.
    public SlaResponse getSlaMetricsByChannel(String channel, Integer days, String appStatusFilter, String taskStatusFilter) {
        List<TaskExecutionTimeEntity> executions = slaDao.getTasksByChannel(channel);

        if (days != null && days > 0) {
            Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
            executions = executions.stream()
                    .filter(e -> e.getRecordDate() != null && e.getRecordDate().isAfter(cutoff))
                    .collect(Collectors.toList());
            log.info("Filtered {} records for channel: {} within last {} days", executions.size(), channel, days);
        }

        if (appStatusFilter != null && !appStatusFilter.trim().isEmpty()) {
            executions = executions.stream()
                    .filter(e -> determineApplicationStatus(e, taskStatusFilter).equalsIgnoreCase(appStatusFilter))
                    .collect(Collectors.toList());
            log.info("Filtered {} records for channel: {} with overall status: {}", executions.size(), channel, appStatusFilter);
        }

        if (executions.isEmpty()) {
            throw new SlaException("No data found for channel: " + channel +
                    (days != null && days > 0 ? " in the past " + days + " days" : "") +
                    (!appStatusFilter.trim().isEmpty() ? " with status " + appStatusFilter : ""));
        }

        log.info("Processing {} execution records for channel: {}", executions.size(), channel);

        Map<String, List<Long>> taskDurations = new LinkedHashMap<>();
        Map<String, List<Long>> taskSendbacks = new LinkedHashMap<>();
        Map<String, Set<String>> funnelToTaskMapping = new LinkedHashMap<>();

        processExecutions(executions, taskDurations, taskSendbacks, funnelToTaskMapping, taskStatusFilter);

        Map<String, String> avgTaskTimes = calculateAverageTimes(taskDurations);
        Map<String, String> avgFunnelTimes = calculateAverageFunnelTimes(funnelToTaskMapping, taskDurations);
        long totalTAT = calculateTotalTAT(avgFunnelTimes);
        Map<String, Long> sendbackCounts = calculateSendbackCounts(taskSendbacks);

        Map<String, SlaResponse.Distribution> tatDistribution = computeDynamicTatDistribution(executions, taskStatusFilter);
        Map<String, Map<String, SlaResponse.Distribution>> taskDistributions = computeTaskDistributions(executions, taskStatusFilter);

        return new SlaResponse(
                initializeFunnels(avgFunnelTimes, funnelToTaskMapping, avgTaskTimes, sendbackCounts),
                SlaResponse.formatDuration(totalTAT),
                tatDistribution,
                taskDistributions
        );
    }

    // Helper method to get funnels filtered by taskStatusFilter if it is not "ALL".
    private Map<String, List<SubTaskEntity>> getFilteredFunnels(TaskExecutionTimeEntity execution, String taskStatusFilter) {
        Map<String, List<SubTaskEntity>> funnels = getFunnels(execution);
        if (taskStatusFilter == null || "ALL".equalsIgnoreCase(taskStatusFilter.trim())) {
            return funnels;
        }
        Map<String, List<SubTaskEntity>> filteredFunnels = new LinkedHashMap<>();
        for (Map.Entry<String, List<SubTaskEntity>> entry : funnels.entrySet()) {
            List<SubTaskEntity> filteredList = entry.getValue().stream()
                    .filter(task -> taskStatusFilter.equalsIgnoreCase(task.getStatusoftask()))
                    .collect(Collectors.toList());
            filteredFunnels.put(entry.getKey(), filteredList);
        }
        return filteredFunnels;
    }

    private void processExecutions(List<TaskExecutionTimeEntity> executions,
                                   Map<String, List<Long>> taskDurations,
                                   Map<String, List<Long>> taskSendbacks,
                                   Map<String, Set<String>> funnelToTaskMapping,
                                   String taskStatusFilter) {
        for (TaskExecutionTimeEntity execution : executions) {
            Map<String, List<SubTaskEntity>> funnels = getFilteredFunnels(execution, taskStatusFilter);
            funnels.forEach((funnelName, tasks) -> {
                for (SubTaskEntity task : tasks) {
                    taskDurations.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>()).add(task.getDuration());
                    if (task.getSendbacks() >= 0) {
                        taskSendbacks.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>()).add((long) task.getSendbacks());
                    }
                    funnelToTaskMapping.computeIfAbsent(funnelName, k -> new LinkedHashSet<>()).add(task.getTaskId());
                }
            });
        }
        log.info("Processed {} execution records", executions.size());
    }

    private Map<String, List<SubTaskEntity>> getFunnels(TaskExecutionTimeEntity execution) {
        return Map.of(
                "sourcing", execution.getSourcing() == null ? new ArrayList<>() : execution.getSourcing(),
                "credit", execution.getCredit() == null ? new ArrayList<>() : execution.getCredit(),
                "risk", execution.getRisk() == null ? new ArrayList<>() : execution.getRisk(),
                "conversion", execution.getConversion() == null ? new ArrayList<>() : execution.getConversion(),
                "rto", execution.getRto() == null ? new ArrayList<>() : execution.getRto(),
                "fulfillment", execution.getFulfillment() == null ? new ArrayList<>() : execution.getFulfillment(),
                "disbursal", execution.getDisbursal() == null ? new ArrayList<>() : execution.getDisbursal()
        );
    }

    private Map<String, String> calculateAverageTimes(Map<String, List<Long>> taskDurations) {
        return taskDurations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> SlaResponse.formatDuration(
                                (long) entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0)
                        )
                ));
    }

    private Map<String, String> calculateAverageFunnelTimes(Map<String, Set<String>> funnelToTaskMapping,
                                                            Map<String, List<Long>> taskDurations) {
        return funnelToTaskMapping.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> SlaResponse.formatDuration(
                                (long) entry.getValue().stream()
                                        .mapToDouble(taskId -> taskDurations.getOrDefault(taskId, List.of(0L))
                                                .stream().mapToLong(Long::longValue).average().orElse(0.0)
                                        ).sum()
                        )
                ));
    }

    private long calculateTotalTAT(Map<String, String> avgFunnelTimes) {
        return (long) avgFunnelTimes.values().stream()
                .mapToDouble(TimeUtils::convertFormattedTimeToMillis)
                .sum();
    }

    private Map<String, Long> calculateSendbackCounts(Map<String, List<Long>> taskSendbacks) {
        return taskSendbacks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Math.round(entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))
                ));
    }

    private Map<String, SlaResponse.Distribution> computeDynamicTatDistribution(List<TaskExecutionTimeEntity> executions, String taskStatusFilter) {
        Map<String, SlaResponse.Distribution> distribution = new LinkedHashMap<>();
        long globalMin = Long.MAX_VALUE;
        long globalMax = Long.MIN_VALUE;
        Map<String, Long> appTatMap = new HashMap<>();
        Map<String, String> appStatusMap = new HashMap<>();

        for (TaskExecutionTimeEntity execution : executions) {
            Map<String, List<SubTaskEntity>> funnels = getFilteredFunnels(execution, taskStatusFilter);
            long tat = sumDurations(funnels.get("sourcing"))
                    + sumDurations(funnels.get("credit"))
                    + sumDurations(funnels.get("risk"))
                    + sumDurations(funnels.get("conversion"))
                    + sumDurations(funnels.get("rto"))
                    + sumDurations(funnels.get("fulfillment"))
                    + sumDurations(funnels.get("disbursal"));
            String appId = execution.getApplicationId();
            appTatMap.put(appId, tat);
            globalMin = Math.min(globalMin, tat);
            globalMax = Math.max(globalMax, tat);
            String overallStatus = determineApplicationStatus(execution, taskStatusFilter);
            appStatusMap.put(appId, overallStatus);
        }

        List<Long> tatValues = new ArrayList<>(appTatMap.values());
        Collections.sort(tatValues);

        if (globalMin == globalMax) {
            String rangeKey = formatRange(globalMin, globalMax);
            SlaResponse.Distribution d = new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>());
            for (String appId : appTatMap.keySet()) {
                d.setCount(d.getCount() + 1);
                if (d.getApplicationIds().size() < 100) {
                    d.getApplicationIds().add(appId);
                    d.getApplicationStatusMap().put(appId, appStatusMap.get(appId));
                }
            }
            distribution.put(rangeKey, d);
            return distribution;
        }

        long p30 = getPercentile(tatValues, 30);
        long p70 = getPercentile(tatValues, 70);
        double tolLower = (p30 - globalMin) * 0.1;
        double tolUpper = (globalMax - p70) * 0.1;
        String lowerRangeKey = formatRange(globalMin, p30);
        String middleRangeKey = formatRange(p30, p70);
        String upperRangeKey = formatRange(p70, globalMax);

        distribution.put(lowerRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));
        distribution.put(middleRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));
        distribution.put(upperRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));

        for (Map.Entry<String, Long> entry : appTatMap.entrySet()) {
            String appId = entry.getKey();
            long tat = entry.getValue();
            if (tat <= p30 || (tat > p30 && tat - p30 <= tolLower)) {
                SlaResponse.Distribution d = distribution.get(lowerRangeKey);
                d.setCount(d.getCount() + 1);
                d.getApplicationIds().add(appId);
                d.getApplicationStatusMap().put(appId, appStatusMap.get(appId));
            } else if (tat < p70 && (p70 - tat <= tolUpper)) {
                SlaResponse.Distribution d = distribution.get(middleRangeKey);
                d.setCount(d.getCount() + 1);
                d.getApplicationIds().add(appId);
                d.getApplicationStatusMap().put(appId, appStatusMap.get(appId));
            } else if (tat < p70) {
                SlaResponse.Distribution d = distribution.get(middleRangeKey);
                d.setCount(d.getCount() + 1);
                d.getApplicationIds().add(appId);
                d.getApplicationStatusMap().put(appId, appStatusMap.get(appId));
            } else {
                SlaResponse.Distribution d = distribution.get(upperRangeKey);
                d.setCount(d.getCount() + 1);
                d.getApplicationIds().add(appId);
                d.getApplicationStatusMap().put(appId, appStatusMap.get(appId));
            }
        }

        for (SlaResponse.Distribution d : distribution.values()) {
            List<String> appIds = d.getApplicationIds();
            if (appIds.size() > 100) {
                d.setApplicationIds(new ArrayList<>(appIds.subList(appIds.size() - 100, appIds.size())));
            }
        }
        return distribution;
    }

    private Map<String, Map<String, SlaResponse.Distribution>> computeTaskDistributions(List<TaskExecutionTimeEntity> executions, String taskStatusFilter) {
        Map<String, List<TaskDurationRecord>> taskRecords = new HashMap<>();

        for (TaskExecutionTimeEntity execution : executions) {
            String appId = execution.getApplicationId();
            Map<String, List<SubTaskEntity>> funnels = getFilteredFunnels(execution, taskStatusFilter);

            for (Map.Entry<String, List<SubTaskEntity>> entry : funnels.entrySet()) {
                for (SubTaskEntity task : entry.getValue()) {
                    taskRecords.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>())
                            .add(new TaskDurationRecord(appId, task.getDuration(), task.getStatusoftask()));
                }
            }
        }

        Map<String, Map<String, SlaResponse.Distribution>> result = new HashMap<>();

        for (Map.Entry<String, List<TaskDurationRecord>> entry : taskRecords.entrySet()) {
            String taskId = entry.getKey();
            List<TaskDurationRecord> records = entry.getValue();
            List<Long> durations = records.stream()
                    .map(r -> r.duration)
                    .sorted()
                    .collect(Collectors.toList());

            long globalMin = durations.get(0);
            long globalMax = durations.get(durations.size() - 1);
            Map<String, SlaResponse.Distribution> buckets = new LinkedHashMap<>();

            if (globalMin == globalMax) {
                String rangeKey = formatRange(globalMin, globalMax);
                SlaResponse.Distribution dist = new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>());
                for (TaskDurationRecord rec : records) {
                    dist.setCount(dist.getCount() + 1);
                    dist.getApplicationIds().add(rec.applicationId);
                    dist.getApplicationStatusMap().put(rec.applicationId, rec.statusOfTask);
                }
                buckets.put(rangeKey, dist);
            } else {
                long p30 = getPercentile(durations, 30);
                long p70 = getPercentile(durations, 70);
                double tolLower = (p30 - globalMin) * 0.1;
                double tolUpper = (globalMax - p70) * 0.1;
                String lowerRangeKey = formatRange(globalMin, p30);
                String middleRangeKey = formatRange(p30, p70);
                String upperRangeKey = formatRange(p70, globalMax);

                buckets.put(lowerRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));
                buckets.put(middleRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));
                buckets.put(upperRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));

                for (TaskDurationRecord rec : records) {
                    if (rec.duration <= p30 || (rec.duration > p30 && rec.duration - p30 <= tolLower)) {
                        SlaResponse.Distribution d = buckets.get(lowerRangeKey);
                        d.setCount(d.getCount() + 1);
                        d.getApplicationIds().add(rec.applicationId);
                        d.getApplicationStatusMap().put(rec.applicationId, rec.statusOfTask);
                    } else if (rec.duration < p70 && (p70 - rec.duration <= tolUpper)) {
                        SlaResponse.Distribution d = buckets.get(middleRangeKey);
                        d.setCount(d.getCount() + 1);
                        d.getApplicationIds().add(rec.applicationId);
                        d.getApplicationStatusMap().put(rec.applicationId, rec.statusOfTask);
                    } else if (rec.duration < p70) {
                        SlaResponse.Distribution d = buckets.get(middleRangeKey);
                        d.setCount(d.getCount() + 1);
                        d.getApplicationIds().add(rec.applicationId);
                        d.getApplicationStatusMap().put(rec.applicationId, rec.statusOfTask);
                    } else {
                        SlaResponse.Distribution d = buckets.get(upperRangeKey);
                        d.setCount(d.getCount() + 1);
                        d.getApplicationIds().add(rec.applicationId);
                        d.getApplicationStatusMap().put(rec.applicationId, rec.statusOfTask);
                    }
                }
            }

            for (SlaResponse.Distribution distribution : buckets.values()) {
                List<String> appIds = distribution.getApplicationIds();
                if (appIds.size() > 100) {
                    distribution.setApplicationIds(new ArrayList<>(appIds.subList(appIds.size() - 100, appIds.size())));
                }
            }

            result.put(taskId, buckets);
        }

        return result;
    }

    private String formatRange(long startMillis, long endMillis) {
        return SlaResponse.formatDuration(startMillis) + " - " + SlaResponse.formatDuration(endMillis);
    }

    private long sumDurations(List<SubTaskEntity> tasks) {
        if (tasks == null) return 0;
        return tasks.stream().mapToLong(SubTaskEntity::getDuration).sum();
    }

    private String determineApplicationStatus(TaskExecutionTimeEntity execution, String taskStatusFilter) {
        boolean hasTasks = false;
        boolean allCompletedOrSkipped = true;
        boolean anyPending = false;
        boolean anyRejected = false;

        Map<String, List<SubTaskEntity>> funnels = getFilteredFunnels(execution, taskStatusFilter);
        for (List<SubTaskEntity> tasks : funnels.values()) {
            for (SubTaskEntity task : tasks) {
                hasTasks = true;
                String status = task.getStatusoftask();
                if (status == null || (!status.equalsIgnoreCase("COMPLETED")
                        && !status.equalsIgnoreCase("SKIPPED")
                        && !status.equalsIgnoreCase("NEW"))) {
                    allCompletedOrSkipped = false;
                }
                if (status != null && (status.equalsIgnoreCase("IN_PROGRESS") || status.equalsIgnoreCase("TODO"))) {
                    anyPending = true;
                }
                if (status != null && status.equalsIgnoreCase("REJECTED")) {
                    anyRejected = true;
                }
            }
        }
        if (!hasTasks) return "Pending";
        if (anyPending) return "Pending";
        if (allCompletedOrSkipped) return "Approved";
        return "Rejected";
    }

    // Original buildSlaResponse method retained for backward compatibility.
    private SlaResponse buildSlaResponse(Map<String, String> avgTaskTimes,
                                         Map<String, String> avgFunnelTimes,
                                         Map<String, Long> sendbackCounts,
                                         long totalTAT,
                                         Map<String, Set<String>> funnelToTaskMapping) {
        Map<String, SlaResponse.Funnel> funnels = initializeFunnels(avgFunnelTimes, funnelToTaskMapping, avgTaskTimes, sendbackCounts);
        Map<String, SlaResponse.Distribution> distribution = computeDynamicTatDistribution(slaDao.getTasksByChannel("D2C"), "ALL");
        Map<String, Map<String, SlaResponse.Distribution>> taskDistributions = computeTaskDistributions(slaDao.getTasksByChannel("D2C"), "ALL");
        return new SlaResponse(funnels, SlaResponse.formatDuration(totalTAT), distribution, taskDistributions);
    }

    private Map<String, SlaResponse.Funnel> initializeFunnels(Map<String, String> avgFunnelTimes, Map<String, Set<String>> funnelToTaskMapping,
                                                              Map<String, String> avgTaskTimes, Map<String, Long> sendbackCounts) {
        Map<String, SlaResponse.Funnel> funnels = new LinkedHashMap<>();
        String[] funnelOrder = {"sourcing", "credit", "risk", "conversion", "rto", "fulfillment", "disbursal"};
        for (String funnelName : funnelOrder) {
            String funnelTime = avgFunnelTimes.getOrDefault(funnelName, SlaResponse.formatDuration(0));
            funnels.put(funnelName, new SlaResponse.Funnel(funnelTime, new LinkedHashMap<>()));
        }
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
        return funnels;
    }

    // Helper inner class for task duration records.
    private static class TaskDurationRecord {
        String applicationId;
        long duration;
        String statusOfTask;

        public TaskDurationRecord(String applicationId, long duration, String statusOfTask) {
            this.applicationId = applicationId;
            this.duration = duration;
            this.statusOfTask = statusOfTask;
        }
    }

    private long getPercentile(List<Long> sortedList, double percentile) {
        if (sortedList.isEmpty()) return 0L;
        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        index = Math.max(0, index);
        return sortedList.get(index);
    }
}
