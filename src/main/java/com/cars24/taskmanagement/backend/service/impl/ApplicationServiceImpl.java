package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.dao.impl.SendbackConfigDao;
import com.cars24.taskmanagement.backend.data.entity.LoanDurationEntity;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import com.cars24.taskmanagement.backend.data.response.applicationDto.FunnelGroupResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.TaskDetailsResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.ListFunnelGroupResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.StatusLogResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.TaskResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final String UNKNOWN_FUNNEL = "Unknown Funnel";
    private final ApplicationDao taskExecutionDao;
    private final SendbackConfigDao sendbackConfigDao;

    //for graph view
    @Override
    public ListFunnelGroupResponse getTasksByApplicationId(String applicationId) {
        List<TaskExecutionLogEntity> sortedTasks = taskExecutionDao.findTasksByApplicationIdSortedByUpdatedAt(applicationId);
        log.info("[getTasksByApplicationId] Retrieved {} tasks for applicationId: {}", sortedTasks.size(), applicationId);

        List<TaskDetailsResponse> taskDetailsResponseList = sortedTasks.stream()
                .map(this::convertToTaskDetails)
                .collect(Collectors.toList());
        List<FunnelGroupResponse> funnelGroupResponses = groupTasksByFunnel(taskDetailsResponseList);
        return new ListFunnelGroupResponse(funnelGroupResponses);
    }

    private List<FunnelGroupResponse> groupTasksByFunnel(List<TaskDetailsResponse> sortedTasks) {
        log.info("[groupTasksByFunnel] Grouping {} tasks by funnel", sortedTasks.size());
        Map<String, FunnelGroupResponse> funnelMap = new LinkedHashMap<>();
        for (TaskDetailsResponse task : sortedTasks) {
            funnelMap.computeIfAbsent(task.getFunnel(), key -> new FunnelGroupResponse(task.getFunnel(), new ArrayList<>()))
                    .getTasks().add(task);
        }
        log.info("[groupTasksByFunnel] Grouped tasks into {} funnel(s)", funnelMap.size());
        return new ArrayList<>(funnelMap.values());
    }

    //for list view
    @Override
    public Map<String, Object> getTasksGroupedByFunnel(String applicationId) {
        log.info("[getTasksGroupedByFunnel] Fetching list view tasks for applicationId: {}", applicationId);

        Map<String, Object> data = taskExecutionDao.findTasksAndLoanDurationByApplicationId(applicationId);
        List<TaskExecutionLogEntity> tasks = (List<TaskExecutionLogEntity>) data.getOrDefault("tasks", Collections.emptyList());
        log.info("[getTasksGroupedByFunnel] Retrieved {} tasks for applicationId: {}", tasks.size(), applicationId);

        LoanDurationEntity loanDurationEntity = (LoanDurationEntity) data.get("loanDurationEntity");

        List<TaskExecutionLogEntity> sendbackTasks = tasks.stream()
                .filter(task -> "sendback".equalsIgnoreCase(task.getTaskId()))
                .toList();

        List<TaskExecutionLogEntity> regularTasks = tasks.stream()
                .filter(task -> !"sendback".equalsIgnoreCase(task.getTaskId()))
                .toList();
        log.info("[getTasksGroupedByFunnel] Separated {} sendback tasks and {} regular tasks", sendbackTasks.size(), regularTasks.size());

        Map<String, Integer> funnelMinOrders = regularTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse(UNKNOWN_FUNNEL),
                        Collectors.mapping(TaskExecutionLogEntity::getOrder, Collectors.minBy(Integer::compare))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().orElse(Integer.MAX_VALUE)
                ));
        log.info("[getTasksGroupedByFunnel] Identified {} unique funnels", funnelMinOrders.size());

        Map<String, LoanDurationEntity.Task> taskMetadata = Optional.ofNullable(loanDurationEntity)
                .map(ld -> Arrays.stream(ld.getClass().getDeclaredFields()) // Get all fields of LoanDurationEntity
                        .filter(field -> List.class.isAssignableFrom(field.getType())) // Filter only List fields
                        .map(field -> {
                            field.setAccessible(true);
                            try {
                                return (List<LoanDurationEntity.Task>) field.get(ld);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Failed to access field: " + field.getName(), e);
                            }
                        })
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                LoanDurationEntity.Task::getTaskId,
                                task -> task,
                                (a, b) -> a // Handle duplicate taskIds by keeping the first occurrence
                        ))
                ).orElse(Collections.emptyMap());

        Map<String, Map<String, List<TaskExecutionLogEntity>>> tasksByFunnelAndId = regularTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse(UNKNOWN_FUNNEL),
                        Collectors.groupingBy(task -> Optional.ofNullable(task.getTaskId()).orElse("UNKNOWN_TASK"))
                ));

        List<String> sortedFunnels = funnelMinOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        LinkedHashMap<String, Object> tasksGroupedByFunnel = new LinkedHashMap<>();

        for (String funnel : sortedFunnels) {
            List<TaskResponse> funnelTasks = tasksByFunnelAndId.getOrDefault(funnel, Collections.emptyMap())
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparing(list -> list.getFirst().getOrder())))
                    .map(entry -> createTaskResponse(entry.getValue(), taskMetadata, false))
                    .collect(Collectors.toList());

            long totalDuration = funnelTasks.stream().mapToLong(TaskResponse::getDuration).sum();
            log.info("[getTasksGroupedByFunnel] Calculated total duration {} for funnel {}", totalDuration, funnel);

            Map<String, Object> funnelData = new LinkedHashMap<>();
            funnelData.put("funnel", funnel);
            funnelData.put("funnelDuration", totalDuration);
            funnelData.put("tasks", funnelTasks);

            tasksGroupedByFunnel.put(funnel, funnelData);
        }
        response.put("tasksGroupedByFunnel", tasksGroupedByFunnel);

        Map<String, List<TaskResponse>> sendbackGroupedByRequestId = sendbackTasks.stream()
                .sorted(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getRequestId()).orElse("UNKNOWN_REQUEST"),
                        Collectors.mapping(applicationLog -> createTaskResponse(Collections.singletonList(applicationLog), taskMetadata, true), Collectors.toList())
                ));

        response.put("sendbackTasks", sendbackGroupedByRequestId);

        TaskExecutionLogEntity latestLog = tasks.stream()
                .max(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                .orElse(null);

        if (latestLog != null) {
            response.put("latestTaskState", Map.of(
                    "taskId", Optional.ofNullable(latestLog.getTaskId()).orElse("UNKNOWN_TASK"),
                    "order", latestLog.getOrder(),
                    "handledBy", latestLog.getHandledBy(),
                    "createdAt", latestLog.getCreatedAt(),
                    "status", latestLog.getStatus(),
                    "updatedAt", latestLog.getUpdatedAt(),
                    "duration", taskMetadata.getOrDefault(latestLog.getTaskId(), new LoanDurationEntity.Task()).getDuration(),
                    "sendbacks", taskMetadata.getOrDefault(latestLog.getTaskId(), new LoanDurationEntity.Task()).getSendbacks(),
                    "visited", taskMetadata.getOrDefault(latestLog.getTaskId(), new LoanDurationEntity.Task()).getVisited()
            ));
            log.info("[getTasksGroupedByFunnel] Latest task state recorded for applicationId {}", applicationId);
        } else {
            response.put("latestTaskState", null);
        }

        return response;
    }

    private TaskResponse createTaskResponse(List<TaskExecutionLogEntity> logs,
                                            Map<String, LoanDurationEntity.Task> taskMetadata,
                                            boolean isSendback) {
        TaskExecutionLogEntity firstLog = logs.getFirst();
        log.info("[createTaskResponse] Creating response for taskId: {} isSendback: {}", firstLog.getTaskId(), isSendback);

        List<StatusLogResponse> statusLogs = logs.stream()
                .sorted(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                .map(applicationLog -> new StatusLogResponse(applicationLog.getStatus(), applicationLog.getUpdatedAt()))
                .collect(Collectors.toList());

        LoanDurationEntity.Task metadata = taskMetadata.getOrDefault(firstLog.getTaskId(), null);
        long duration = metadata != null ? metadata.getDuration() : 0;
        int sendbacks = metadata != null ? metadata.getSendbacks() : 0;
        int visited = metadata != null ? metadata.getVisited() : 0;

        String targetTaskId = isSendback ? fetchTargetTaskId(firstLog) : null;
        String sourceLoanStage = isSendback ? fetchSourceModule(firstLog) : null;
        String sourceSubModule = isSendback ? fetchSubModule(firstLog) : null;

        return new TaskResponse(
                firstLog.getTaskId(),
                firstLog.getOrder(),
                firstLog.getHandledBy(),
                firstLog.getCreatedAt(),
                statusLogs,
                targetTaskId,
                duration,
                sendbacks,
                visited,
                sourceLoanStage,
                sourceSubModule
        );
    }

    private String fetchTargetTaskId(TaskExecutionLogEntity applicationLog) {
        log.info("[fetchTargetTaskId] Fetching target task ID for log: {}", applicationLog.getTaskId());
        Map<String, Object> sendbackMetadata = (Map<String, Object>) applicationLog.getSendbackMetadata();

        if (sendbackMetadata != null && sendbackMetadata.containsKey("key")) {
            String sendbackKey = (String) sendbackMetadata.get("key");
            return sendbackConfigDao.findBySendbackKey(sendbackKey)
                    .map(config -> {
                        if (!config.getSubReasonList().isEmpty()) {
                            return config.getSubReasonList().get(0).getTargetTaskId();
                        }
                        return null;
                    })
                    .orElse(null);
        }
        return null;
    }

    private String fetchSourceModule(TaskExecutionLogEntity applicationLog) {
        log.info("[fetchSourceModule] Fetching source module for log: {}", applicationLog.getTaskId());
        Map<String, Object> sendbackMetadata = (Map<String, Object>) applicationLog.getSendbackMetadata();
        return sendbackMetadata != null ? (String) sendbackMetadata.get("sourceLoanStage") : null;
    }

    private String fetchSubModule(TaskExecutionLogEntity applicationLog) {
        log.info("[fetchSubModule] Fetching sub module for log: {}", applicationLog.getTaskId());
        Map<String, Object> sendbackMetadata = (Map<String, Object>) applicationLog.getSendbackMetadata();
        return sendbackMetadata != null ? (String) sendbackMetadata.get("sourceSubModule") : null;
    }

    private TaskDetailsResponse convertToTaskDetails(TaskExecutionLogEntity applicationLog) {
        log.info("[convertToTaskDetails] Converting task details for taskId: {}", applicationLog.getTaskId());
        String targetTaskId = "sendback".equalsIgnoreCase(applicationLog.getTaskId()) ? fetchTargetTaskId(applicationLog) : null;

        return new TaskDetailsResponse(
                Optional.ofNullable(applicationLog.getFunnel()).orElse(UNKNOWN_FUNNEL),
                applicationLog.getActorId(),
                applicationLog.getStatus(),
                applicationLog.getUpdatedAt(),
                applicationLog.getTaskId(),
                targetTaskId,
                0,
                0,
                applicationLog.getMetadata()
        );
    }
}