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
        List<TaskDetailsResponse> taskDetailsResponseList = sortedTasks.stream()
                .map(this::convertToTaskDetails)
                .collect(Collectors.toList());
        List<FunnelGroupResponse> funnelGroupResponses = groupTasksByFunnel(taskDetailsResponseList);
        return new ListFunnelGroupResponse(funnelGroupResponses);
    }


    //for list view
    @Override
    public Map<String, Object> getTasksGroupedByFunnel(String applicationId) {
        Map<String, Object> data = taskExecutionDao.findTasksAndLoanDurationByApplicationId(applicationId);
        List<TaskExecutionLogEntity> tasks = (List<TaskExecutionLogEntity>) data.getOrDefault("tasks", Collections.emptyList());
        LoanDurationEntity loanDurationEntity = (LoanDurationEntity) data.get("loanDurationEntity");

        List<TaskExecutionLogEntity> sendbackTasks = tasks.stream()
                .filter(task -> "sendback".equalsIgnoreCase(task.getTaskId()))
                .toList();

        List<TaskExecutionLogEntity> regularTasks = tasks.stream()
                .filter(task -> !"sendback".equalsIgnoreCase(task.getTaskId()))
                .toList();

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

        Map<String, LoanDurationEntity.Task> taskMetadata = Optional.ofNullable(loanDurationEntity)
                .map(ld -> Stream.of(ld.getSourcing(), ld.getCredit(), ld.getConversion(), ld.getFulfillment())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                task -> task.getTaskId(),
                                task -> task,
                                (a, b) -> a
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

            // Calculate total duration for the funnel
            long totalDuration = funnelTasks.stream().mapToLong(TaskResponse::getDuration).sum();

            // Create funnel data with total duration
            Map<String, Object> funnelData = new LinkedHashMap<>();
            funnelData.put("funnel", funnel);
            funnelData.put("funnelDuration", totalDuration);
            funnelData.put("tasks", funnelTasks);

            tasksGroupedByFunnel.put(funnel, funnelData);
        }
        response.put("tasksGroupedByFunnel", tasksGroupedByFunnel);

        Map<String, List<TaskResponse>> sendbackGroupedByRequestId = sendbackTasks.stream()
                .sorted(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt)) // Sorting before grouping
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getRequestId()).orElse("UNKNOWN_REQUEST"),
                        Collectors.mapping(log -> createTaskResponse(Collections.singletonList(log), taskMetadata, true), Collectors.toList())
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
        } else {
            response.put("latestTaskState", null);
        }

        return response;
    }

    private TaskResponse createTaskResponse(List<TaskExecutionLogEntity> logs,
                                            Map<String, LoanDurationEntity.Task> taskMetadata,
                                            boolean isSendback) {
        TaskExecutionLogEntity firstLog = logs.getFirst();

        List<StatusLogResponse> statusLogs = logs.stream()
                .sorted(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                .map(log -> new StatusLogResponse(log.getStatus(), log.getUpdatedAt()))
                .collect(Collectors.toList());

        LoanDurationEntity.Task metadata = taskMetadata.getOrDefault(firstLog.getTaskId(), null);
        long duration = metadata != null ? metadata.getDuration() : 0;
        int sendbacks = metadata != null ? metadata.getSendbacks() : 0;
        int visited = metadata != null ? metadata.getVisited() : 0;

        // Fetch targetTaskId only if it's a sendback task
        String targetTaskId = isSendback ? fetchTargetTaskId(firstLog) : null;

        // Extract sourceModule and subModule for sendback tasks
        String sourceLoanStage = isSendback ? fetchSourceModule(firstLog) : null;
        String sourceSubModule = isSendback ? fetchSubModule(firstLog) : null;

        return new TaskResponse(
                firstLog.getTaskId(),
                firstLog.getOrder(),
                firstLog.getHandledBy(),
                firstLog.getCreatedAt(),
                statusLogs,
                targetTaskId,
                duration, // Duration will be 0 for sendback tasks
                sendbacks,
                visited,
                sourceLoanStage, // Add sourceModule
                sourceSubModule // Add subModule
        );
    }

    private String fetchTargetTaskId(TaskExecutionLogEntity log) {
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getSendbackMetadata();

        if (sendbackMetadata != null && sendbackMetadata.containsKey("key")) {
            String sendbackKey = (String) sendbackMetadata.get("key");
            return sendbackConfigDao.findBySendbackKey(sendbackKey)
                    .map(config -> {
                        if (!config.getSubReasonList().isEmpty()) {
                            return config.getSubReasonList().get(0).getTargetTaskId();
                        }
                        return null; // Return null if the list is empty
                    })
                    .orElse(null); // Return null if no configuration is found
        }
        return null; // Return null if the key is not found in the metadata
    }

    private String fetchSourceModule(TaskExecutionLogEntity log) {
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getSendbackMetadata();
        return sendbackMetadata != null ? (String) sendbackMetadata.get("sourceLoanStage") : null;
    }

    private String fetchSubModule(TaskExecutionLogEntity log) {
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getSendbackMetadata();
        return sendbackMetadata != null ? (String) sendbackMetadata.get("sourceSubModule") : null;
    }

    private List<FunnelGroupResponse> groupTasksByFunnel(List<TaskDetailsResponse> sortedTasks) {
        Map<String, FunnelGroupResponse> funnelMap = new LinkedHashMap<>();
        for (TaskDetailsResponse task : sortedTasks) {
            funnelMap.computeIfAbsent(task.getFunnel(), key -> new FunnelGroupResponse(task.getFunnel(), new ArrayList<>()))
                    .getTasks().add(task);
        }
        return new ArrayList<>(funnelMap.values());
    }
    private TaskDetailsResponse convertToTaskDetails(TaskExecutionLogEntity log) {
        String targetTaskId = "sendback".equalsIgnoreCase(log.getTaskId()) ? fetchTargetTaskId(log) : null;

        return new TaskDetailsResponse(
                Optional.ofNullable(log.getFunnel()).orElse(UNKNOWN_FUNNEL), // 1: Funnel
                log.getActorId(),
                log.getStatus(),
                log.getUpdatedAt(),
                log.getTaskId(),
                targetTaskId,
                0,
                0,
                log.getMetadata()
        );
    }

}