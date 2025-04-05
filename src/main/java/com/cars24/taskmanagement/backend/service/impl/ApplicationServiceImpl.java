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


    @Override
    public Map<String, Object> getTasksGroupedByFunnel(String applicationId) {
        log.info("[getTasksGroupedByFunnel] Starting to process tasks for applicationId={}", applicationId);
        Map<String, Object> data = taskExecutionDao.findTasksAndLoanDurationByApplicationId(applicationId);
        List<TaskExecutionLogEntity> tasks = (List<TaskExecutionLogEntity>) data.getOrDefault("tasks", Collections.emptyList());
        LoanDurationEntity loanDurationEntity = (LoanDurationEntity) data.get("loanDurationEntity");

        List<TaskExecutionLogEntity> sendbackTasks = tasks.stream()
                .filter(task -> "sendback".equalsIgnoreCase(task.getTaskId()))
                .toList();

        List<TaskExecutionLogEntity> regularTasks = tasks.stream()
                .filter(task -> !"sendback".equalsIgnoreCase(task.getTaskId()))
                .toList();

        Map<String, Integer> funnelMinOrders = calculateFunnelMinOrders(regularTasks);
        Map<String, Map<String, List<TaskExecutionLogEntity>>> tasksByFunnelAndId = groupTasksByFunnelAndId(regularTasks);
        Map<String, LoanDurationEntity.Task> taskMetadata = extractTaskMetadata(loanDurationEntity);

        List<String> sortedFunnels = getSortedFunnels(funnelMinOrders);
        LinkedHashMap<String, Object> tasksGroupedByFunnel = createTasksGroupedByFunnel(sortedFunnels, tasksByFunnelAndId, taskMetadata);

        Map<String, Map<String, Map<String, TaskResponse>>> sendbackGrouped = processSendbackTasks(sendbackTasks, taskMetadata);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tasksGroupedByFunnel", tasksGroupedByFunnel);
        response.put("sendbackTasks", sendbackGrouped);
        response.put("latestTaskState", createLatestTaskState(tasks, taskMetadata));

        log.info("[getTasksGroupedByFunnel] Completed processing for applicationId={}", applicationId);
        return response;
    }

    private Map<String, Integer> calculateFunnelMinOrders(List<TaskExecutionLogEntity> regularTasks) {
        log.info("[calculateFunnelMinOrders] Calculating minimum orders for funnels");
        return regularTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse(UNKNOWN_FUNNEL),
                        Collectors.mapping(TaskExecutionLogEntity::getOrder, Collectors.minBy(Integer::compare))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().orElse(Integer.MAX_VALUE)
                ));
    }

    private Map<String, Map<String, List<TaskExecutionLogEntity>>> groupTasksByFunnelAndId(List<TaskExecutionLogEntity> regularTasks) {
        log.info("[groupTasksByFunnelAndId] Grouping tasks by funnel and ID");
        return regularTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse(UNKNOWN_FUNNEL),
                        Collectors.groupingBy(task -> Optional.ofNullable(task.getTaskId()).orElse("UNKNOWN_TASK"))
                ));
    }

    private Map<String, LoanDurationEntity.Task> extractTaskMetadata(LoanDurationEntity loanDurationEntity) {
        log.info("[extractTaskMetadata] Extracting task metadata");
        return Optional.ofNullable(loanDurationEntity)
                .map(ld -> Stream.of(ld.getSourcing(), ld.getCredit(), ld.getConversion(), ld.getFulfillment())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                task -> task.getTaskId(),
                                task -> task,
                                (a, b) -> a
                        ))
                ).orElse(Collections.emptyMap());
    }

    private List<String> getSortedFunnels(Map<String, Integer> funnelMinOrders) {
        log.info("[getSortedFunnels] Sorting funnels by order");
        return funnelMinOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    private LinkedHashMap<String, Object> createTasksGroupedByFunnel(
            List<String> sortedFunnels,
            Map<String, Map<String, List<TaskExecutionLogEntity>>> tasksByFunnelAndId,
            Map<String, LoanDurationEntity.Task> taskMetadata) {
        log.info("[createTasksGroupedByFunnel] Creating tasks grouped by funnel");
        LinkedHashMap<String, Object> tasksGroupedByFunnel = new LinkedHashMap<>();

        for (String funnel : sortedFunnels) {
            List<TaskResponse> funnelTasks = tasksByFunnelAndId.getOrDefault(funnel, Collections.emptyMap())
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparing(list -> list.getFirst().getOrder())))
                    .map(entry -> createTaskResponse(entry.getValue(), taskMetadata, false))
                    .collect(Collectors.toList());

            long totalDuration = funnelTasks.stream().mapToLong(TaskResponse::getDuration).sum();

            Map<String, Object> funnelData = new LinkedHashMap<>();
            funnelData.put("funnel", funnel);
            funnelData.put("funnelDuration", totalDuration);
            funnelData.put("tasks", funnelTasks);

            tasksGroupedByFunnel.put(funnel, funnelData);
        }
        return tasksGroupedByFunnel;
    }

    private Map<String, Map<String, Map<String, TaskResponse>>> processSendbackTasks(
            List<TaskExecutionLogEntity> sendbackTasks,
            Map<String, LoanDurationEntity.Task> taskMetadata) {
        log.info("[processSendbackTasks] Processing sendback tasks with new grouping structure");

        return sendbackTasks.stream()
                .sorted(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                .collect(Collectors.groupingBy(
                        this::getSendbackKey,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                this::getSourceFunnelKey,
                                LinkedHashMap::new,
                                Collectors.groupingBy(
                                        task -> Optional.ofNullable(fetchTargetTaskId(task)).orElse("UNKNOWN_TARGET"),
                                        LinkedHashMap::new,
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                tasks -> createTaskResponse(tasks, taskMetadata, true)
                                        )
                                )
                        )
                ));
    }

    private String getSendbackKey(TaskExecutionLogEntity task) {
        log.info("[getSendbackKey] Extracting sendback key for taskId={}", task.getTaskId());
        Map<String, Object> sendbackMetadata = (Map<String, Object>) task.getSendbackMetadata();
        return Optional.ofNullable(sendbackMetadata)
                .map(metadata -> (String) metadata.get("key"))
                .orElse("UNKNOWN_KEY");
    }

    private String getSourceFunnelKey(TaskExecutionLogEntity task) {
        log.info("[getSourceFunnelKey] Creating source funnel key for taskId={}", task.getTaskId());
        String sourceLoanStage = fetchSourceModule(task);
        String sourceSubModule = fetchSubModule(task);

        if (sourceLoanStage != null && sourceSubModule != null) {
            return sourceLoanStage + "_" + sourceSubModule;
        }
        return "UNKNOWN_SOURCE";
    }

    private Map<String, Object> createLatestTaskState(List<TaskExecutionLogEntity> tasks, Map<String, LoanDurationEntity.Task> taskMetadata) {
        log.info("[createLatestTaskState] Creating latest task state");
        TaskExecutionLogEntity latestLog = tasks.stream()
                .max(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                .orElse(null);

        if (latestLog != null) {
            return Map.of(
                    "taskId", Optional.ofNullable(latestLog.getTaskId()).orElse("UNKNOWN_TASK"),
                    "order", latestLog.getOrder(),
                    "handledBy", latestLog.getHandledBy(),
                    "createdAt", latestLog.getCreatedAt(),
                    "status", latestLog.getStatus(),
                    "updatedAt", latestLog.getUpdatedAt(),
                    "duration", taskMetadata.getOrDefault(latestLog.getTaskId(), new LoanDurationEntity.Task()).getDuration(),
                    "sendbacks", taskMetadata.getOrDefault(latestLog.getTaskId(), new LoanDurationEntity.Task()).getSendbacks(),
                    "visited", taskMetadata.getOrDefault(latestLog.getTaskId(), new LoanDurationEntity.Task()).getRevisit()
            );
        }
        return null;
    }

    private TaskResponse createTaskResponse(
            List<TaskExecutionLogEntity> logs,
            Map<String, LoanDurationEntity.Task> taskMetadata,
            boolean isSendback) {
        log.info("[createTaskResponse] Creating task response for taskId={}, isSendback={}",
                logs.getFirst().getTaskId(), isSendback);

        TaskExecutionLogEntity firstLog = logs.getFirst();

        List<StatusLogResponse> statusLogs = logs.stream()
                .sorted(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                .map(log -> new StatusLogResponse(log.getStatus(), log.getUpdatedAt()))
                .collect(Collectors.toList());

        LoanDurationEntity.Task metadata = taskMetadata.getOrDefault(firstLog.getTaskId(), null);
        long duration = metadata != null ? metadata.getDuration() : 0;
        int sendbacks = metadata != null ? metadata.getSendbacks() : 0;
        int visited = metadata != null ? metadata.getRevisit() : 0;

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

    private String fetchTargetTaskId(TaskExecutionLogEntity log) {
        //log.info("[fetchTargetTaskId] Fetching target task ID for taskId={}", log.getTaskId());
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getSendbackMetadata();

        if (sendbackMetadata != null && sendbackMetadata.containsKey("key")) {
            String sendbackKey = (String) sendbackMetadata.get("key");
            return sendbackConfigDao.findBySendbackKey(sendbackKey)
                    .map(config -> {
                        if (!config.getSubReasonList().isEmpty()) {
                            return config.getSubReasonList().get(0).getTargetTaskIds().getFirst();  //to be changed
                        }
                        return null;
                    })
                    .orElse(null);
        }
        return null;
    }



    private String fetchSourceModule(TaskExecutionLogEntity log) {
        //log.info("[fetchSourceModule] Fetching source module for taskId={}", log.getTaskId());
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getSendbackMetadata();
        return sendbackMetadata != null ? (String) sendbackMetadata.get("sourceLoanStage") : null;
    }

    private String fetchSubModule(TaskExecutionLogEntity log) {
        //log.info("[fetchSubModule] Fetching sub module for taskId={}", log.getTaskId());
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getSendbackMetadata();
        return sendbackMetadata != null ? (String) sendbackMetadata.get("sourceSubModule") : null;
    }






}