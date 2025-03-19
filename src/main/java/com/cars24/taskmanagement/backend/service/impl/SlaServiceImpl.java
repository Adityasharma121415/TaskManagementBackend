package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.impl.SlaDaoImpl;
import com.cars24.taskmanagement.backend.data.entity.SubTaskEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.exceptions.SlaException;
import com.cars24.taskmanagement.backend.data.response.SlaResponse;
import com.cars24.taskmanagement.backend.service.SlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaDaoImpl slaDao;

    @Override
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

        // Compute overall dynamic TAT distribution across applications
        Map<String, SlaResponse.Distribution> tatDistribution = computeDynamicTatDistribution(executions);
        // Compute dynamic per-task distributions
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
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Math.round(entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0))
                ));
    }

    // Compute overall dynamic TAT distribution across applications
    private Map<String, SlaResponse.Distribution> computeDynamicTatDistribution(List<TaskExecutionTimeEntity> executions) {
        Map<String, SlaResponse.Distribution> distribution = new LinkedHashMap<>();
        long globalMin = Long.MAX_VALUE;
        long globalMax = Long.MIN_VALUE;
        Map<String, Long> appTatMap = new HashMap<>();
        for (TaskExecutionTimeEntity execution : executions) {
            long tat = sumDurations(execution.getSourcing())
                    + sumDurations(execution.getCredit())
                    + sumDurations(execution.getRisk())
                    + sumDurations(execution.getConversion())
                    + sumDurations(execution.getRto())
                    + sumDurations(execution.getFulfillment())
                    + sumDurations(execution.getDisbursal());
            appTatMap.put(execution.getApplicationId(), tat);
            globalMin = Math.min(globalMin, tat);
            globalMax = Math.max(globalMax, tat);
        }
        long totalRange = globalMax - globalMin;
        if (totalRange == 0) {
            String rangeKey = formatRange(globalMin, globalMax);
            SlaResponse.Distribution d = new SlaResponse.Distribution(0, new ArrayList<>());
            for (String appId : appTatMap.keySet()) {
                d.setCount(d.getCount() + 1);
                if (d.getApplicationIds().size() < 100) {
                    d.getApplicationIds().add(appId);
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
        distribution.put(lowerRangeKey, new SlaResponse.Distribution(0, new ArrayList<>()));
        distribution.put(middleRangeKey, new SlaResponse.Distribution(0, new ArrayList<>()));
        distribution.put(upperRangeKey, new SlaResponse.Distribution(0, new ArrayList<>()));
        for (Map.Entry<String, Long> entry : appTatMap.entrySet()) {
            String appId = entry.getKey();
            long tat = entry.getValue();
            if (tat <= lowerUpperBound) {
                SlaResponse.Distribution d = distribution.get(lowerRangeKey);
                d.setCount(d.getCount() + 1);
                d.getApplicationIds().add(appId);
            } else if (tat <= middleUpperBound) {
                SlaResponse.Distribution d = distribution.get(middleRangeKey);
                d.setCount(d.getCount() + 1);
                d.getApplicationIds().add(appId);
            } else {
                SlaResponse.Distribution d = distribution.get(upperRangeKey);
                d.setCount(d.getCount() + 1);
                d.getApplicationIds().add(appId);
            }
        }
        // Trim each bucket's applicationIds list to the most recent 100 entries (assuming order of insertion reflects recency)
        for (SlaResponse.Distribution d : distribution.values()) {
            List<String> appIds = d.getApplicationIds();
            if (appIds.size() > 100) {
                d.setApplicationIds(new ArrayList<>(appIds.subList(appIds.size() - 100, appIds.size())));
            }
        }
        return distribution;
    }

    // Compute dynamic distribution for each task.
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
                SlaResponse.Distribution dist = new SlaResponse.Distribution(0, new ArrayList<>());
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
                SlaResponse.Distribution dist1 = new SlaResponse.Distribution(0, new ArrayList<>());
                SlaResponse.Distribution dist2 = new SlaResponse.Distribution(0, new ArrayList<>());
                SlaResponse.Distribution dist3 = new SlaResponse.Distribution(0, new ArrayList<>());
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
            // Trim each bucket's applicationIds list to only the last 100 entries (most recent)
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

    // Helper inner class for task duration records.
    private static class TaskDurationRecord {
        String applicationId;
        long duration;
        public TaskDurationRecord(String applicationId, long duration) {
            this.applicationId = applicationId;
            this.duration = duration;
        }
    }
}
