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

    @Override
    public SlaResponse getSlaMetricsByChannel(String channel) {
        // Default: no days filtering and no application status filtering.
        return getSlaMetricsByChannel(channel, null, "");
    }

    @Override
    public SlaResponse getSlaMetricsByChannel(String channel, Integer days) {
        return null;
    }

    /**
     * Overloaded method supporting filtering by days and overall application status.
     * @param channel the channel (e.g., "D2C")
     * @param days if provided (> 0), only records with recordDate within the last 'days' are used.
     * @param appStatusFilter if provided (e.g., "Pending", "Approved", "Rejected"),
     *                        only executions whose overall status matches are processed.
     */
    @Override
    public SlaResponse getSlaMetricsByChannel(String channel, Integer days, String appStatusFilter) {
        List<TaskExecutionTimeEntity> executions = slaDao.getTasksByChannel(channel);

        // Filter by date if days parameter is provided.
        if (days != null && days > 0) {
            Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
            executions = executions.stream()
                    .filter(e -> e.getRecordDate() != null && e.getRecordDate().isAfter(cutoff))
                    .collect(Collectors.toList());
            log.info("Filtered {} records for channel: {} within last {} days", executions.size(), channel, days);
        }

        // Filter by overall application status if provided.
        if (appStatusFilter != null && !appStatusFilter.trim().isEmpty()) {
            executions = executions.stream()
                    .filter(e -> determineApplicationStatus(e).equalsIgnoreCase(appStatusFilter))
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

        processExecutions(executions, taskDurations, taskSendbacks, funnelToTaskMapping);

        Map<String, String> avgTaskTimes = calculateAverageTimes(taskDurations);
        Map<String, String> avgFunnelTimes = calculateAverageFunnelTimes(funnelToTaskMapping, taskDurations);
        long totalTAT = calculateTotalTAT(avgFunnelTimes);
        Map<String, Long> sendbackCounts = calculateSendbackCounts(taskSendbacks);

        // Compute overall dynamic TAT distribution (with application statuses).
        Map<String, SlaResponse.Distribution> tatDistribution = computeDynamicTatDistribution(executions);
        // Compute dynamic per-task distributions.
        Map<String, Map<String, SlaResponse.Distribution>> taskDistributions = computeTaskDistributions(executions);

        return new SlaResponse(
                initializeFunnels(avgFunnelTimes, funnelToTaskMapping, avgTaskTimes, sendbackCounts),
                SlaResponse.formatDuration(totalTAT),
                tatDistribution,
                taskDistributions
        );
    }

    private void processExecutions(List<TaskExecutionTimeEntity> executions,
                                   Map<String, List<Long>> taskDurations,
                                   Map<String, List<Long>> taskSendbacks,
                                   Map<String, Set<String>> funnelToTaskMapping) {
        for (TaskExecutionTimeEntity execution : executions) {
            Map<String, List<SubTaskEntity>> funnels = getFunnels(execution);
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

    // Compute overall dynamic TAT distribution across applications, including overall status mapping.
    private Map<String, SlaResponse.Distribution> computeDynamicTatDistribution(List<TaskExecutionTimeEntity> executions) {
        Map<String, SlaResponse.Distribution> distribution = new LinkedHashMap<>();
        long globalMin = Long.MAX_VALUE;
        long globalMax = Long.MIN_VALUE;
        Map<String, Long> appTatMap = new HashMap<>();
        Map<String, String> appStatusMap = new HashMap<>();
        for (TaskExecutionTimeEntity execution : executions) {
            long tat = sumDurations(execution.getSourcing())
                    + sumDurations(execution.getCredit())
                    + sumDurations(execution.getRisk())
                    + sumDurations(execution.getConversion())
                    + sumDurations(execution.getRto())
                    + sumDurations(execution.getFulfillment())
                    + sumDurations(execution.getDisbursal());
            String appId = execution.getApplicationId();
            appTatMap.put(appId, tat);
            globalMin = Math.min(globalMin, tat);
            globalMax = Math.max(globalMax, tat);
            // Determine overall status for the application.
            String overallStatus = determineApplicationStatus(execution);
            appStatusMap.put(appId, overallStatus);
        }
        long totalRange = globalMax - globalMin;
        if (totalRange == 0) {
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
        long lowerUpperBound = globalMin + (long)(0.3 * totalRange);
        long middleUpperBound = globalMin + (long)(0.7 * totalRange);
        String lowerRangeKey = formatRange(globalMin, lowerUpperBound);
        String middleRangeKey = formatRange(lowerUpperBound, middleUpperBound);
        String upperRangeKey = formatRange(middleUpperBound, globalMax);
        distribution.put(lowerRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));
        distribution.put(middleRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));
        distribution.put(upperRangeKey, new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>()));
        for (Map.Entry<String, Long> entry : appTatMap.entrySet()) {
            String appId = entry.getKey();
            long tat = entry.getValue();
            if (tat <= lowerUpperBound) {
                SlaResponse.Distribution d = distribution.get(lowerRangeKey);
                d.setCount(d.getCount() + 1);
                d.getApplicationIds().add(appId);
                d.getApplicationStatusMap().put(appId, appStatusMap.get(appId));
            } else if (tat <= middleUpperBound) {
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
        // Trim each bucket's applicationIds list to the most recent 100 entries.
        for (SlaResponse.Distribution d : distribution.values()) {
            List<String> appIds = d.getApplicationIds();
            if (appIds.size() > 100) {
                d.setApplicationIds(new ArrayList<>(appIds.subList(appIds.size() - 100, appIds.size())));
            }
        }
        return distribution;
    }


    private Map<String, Map<String, SlaResponse.Distribution>> computeTaskDistributions(List<TaskExecutionTimeEntity> executions) {
        Map<String, List<TaskDurationRecord>> taskRecords = new HashMap<>();
        for (TaskExecutionTimeEntity execution : executions) {
            String appId = execution.getApplicationId();
            Map<String, List<SubTaskEntity>> funnels = getFunnels(execution);
            for (Map.Entry<String, List<SubTaskEntity>> entry : funnels.entrySet()) {
                for (SubTaskEntity task : entry.getValue()) {
                    taskRecords.computeIfAbsent(task.getTaskId(), k -> new ArrayList<>())
                            .add(new TaskDurationRecord(appId, task.getDuration()));
                }
            }
        }
        Map<String, Map<String, SlaResponse.Distribution>> result = new HashMap<>();
        for (Map.Entry<String, List<TaskDurationRecord>> entry : taskRecords.entrySet()) {
            String taskId = entry.getKey();
            List<TaskDurationRecord> records = entry.getValue();
            long min = records.stream().mapToLong(r -> r.duration).min().orElse(0);
            long max = records.stream().mapToLong(r -> r.duration).max().orElse(0);
            long range = max - min;
            Map<String, SlaResponse.Distribution> buckets = new LinkedHashMap<>();
            if (range == 0) {
                String rangeKey = formatRange(min, max);
                SlaResponse.Distribution dist = new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>());
                for (TaskDurationRecord rec : records) {
                    dist.setCount(dist.getCount() + 1);
                    dist.getApplicationIds().add(rec.applicationId);
                }
                buckets.put(rangeKey, dist);
            } else {
                long lowerUpper = min + (long)(0.3 * range);
                long middleUpper = min + (long)(0.7 * range);
                String bucket1 = formatRange(min, lowerUpper);
                String bucket2 = formatRange(lowerUpper, middleUpper);
                String bucket3 = formatRange(middleUpper, max);
                SlaResponse.Distribution dist1 = new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>());
                SlaResponse.Distribution dist2 = new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>());
                SlaResponse.Distribution dist3 = new SlaResponse.Distribution(0, new ArrayList<>(), new LinkedHashMap<>());
                for (TaskDurationRecord rec : records) {
                    if (rec.duration <= lowerUpper) {
                        dist1.setCount(dist1.getCount() + 1);
                        dist1.getApplicationIds().add(rec.applicationId);
                    } else if (rec.duration <= middleUpper) {
                        dist2.setCount(dist2.getCount() + 1);
                        dist2.getApplicationIds().add(rec.applicationId);
                    } else {
                        dist3.setCount(dist3.getCount() + 1);
                        dist3.getApplicationIds().add(rec.applicationId);
                    }
                }
                buckets.put(bucket1, dist1);
                buckets.put(bucket2, dist2);
                buckets.put(bucket3, dist3);
            }
            // Trim each bucket's applicationIds list to only the last 100 entries.
            for (Map.Entry<String, SlaResponse.Distribution> bucketEntry : buckets.entrySet()) {
                List<String> appIds = bucketEntry.getValue().getApplicationIds();
                if (appIds.size() > 100) {
                    bucketEntry.getValue().setApplicationIds(new ArrayList<>(appIds.subList(appIds.size() - 100, appIds.size())));
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

    private SlaResponse buildSlaResponse(Map<String, String> avgTaskTimes,
                                         Map<String, String> avgFunnelTimes,
                                         Map<String, Long> sendbackCounts,
                                         long totalTAT,
                                         Map<String, Set<String>> funnelToTaskMapping) {
        Map<String, SlaResponse.Funnel> funnels = initializeFunnels(avgFunnelTimes, funnelToTaskMapping, avgTaskTimes, sendbackCounts);
        Map<String, SlaResponse.Distribution> distribution = computeDynamicTatDistribution(slaDao.getTasksByChannel("D2C"));
        Map<String, Map<String, SlaResponse.Distribution>> taskDistributions = computeTaskDistributions(slaDao.getTasksByChannel("D2C"));
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


    private static class TaskDurationRecord {
        String applicationId;
        long duration;
        public TaskDurationRecord(String applicationId, long duration) {
            this.applicationId = applicationId;
            this.duration = duration;
        }
    }

    private String determineApplicationStatus(TaskExecutionTimeEntity execution) {
        boolean hasTasks = false;
        boolean allCompletedOrSkipped = true;
        boolean anyPending = false;
        boolean anyRejected = false;

        for (List<SubTaskEntity> tasks : getFunnels(execution).values()) {
            for (SubTaskEntity task : tasks) {
                hasTasks = true;
                String status = task.getStatusoftask();

                if (status == null || !(status.equalsIgnoreCase("COMPLETED") || status.equalsIgnoreCase("SKIPPED"))) {
                    allCompletedOrSkipped = false;
                }

                if (status != null && (status.equalsIgnoreCase("NEW") || status.equalsIgnoreCase("TODO"))) {
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
}