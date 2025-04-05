package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.dao.impl.SendbackConfigDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import com.cars24.taskmanagement.backend.data.response.applicationDto.FunnelGroupResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.ListFunnelGroupResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.TaskDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationGanntService {

    private static final String UNKNOWN_FUNNEL = "Unknown Funnel";
    private final ApplicationDao taskExecutionDao;
    private final SendbackConfigDao sendbackConfigDao;

    public ListFunnelGroupResponse getTasksByApplicationId(String applicationId) {
        log.info("[getTasksByApplicationId] Starting to fetch tasks for applicationId={}", applicationId);
        List<TaskExecutionLogEntity> sortedTasks = taskExecutionDao.findTasksByApplicationIdSortedByUpdatedAt(applicationId);
        List<TaskDetailsResponse> taskDetailsResponseList = sortedTasks.stream()
                .map(this::convertToTaskDetails)
                .collect(Collectors.toList());
        List<FunnelGroupResponse> funnelGroupResponses = groupTasksByFunnel(taskDetailsResponseList);
        log.info("[getTasksByApplicationId] Completed processing for applicationId={}", applicationId);
        return new ListFunnelGroupResponse(funnelGroupResponses);
    }


    private Map<String, String> fetchTargetTaskInfo(TaskExecutionLogEntity log) {
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getSendbackMetadata();
        Map<String, String> taskInfo = new HashMap<>();

        if (sendbackMetadata != null) {
            // Fetch sourceLoanStage and sourceSubModule
            String sourceLoanStage = (String) sendbackMetadata.get("sourceLoanStage");
            String sourceSubModule = (String) sendbackMetadata.get("sourceSubModule");

            // Fetch targetTaskId from sendbackConfigDao
            if (sendbackMetadata.containsKey("key")) {
                String sendbackKey = (String) sendbackMetadata.get("key");
                String targetTaskId = sendbackConfigDao.findBySendbackKey(sendbackKey)
                        .map(config -> config.getSubReasonList().stream()
                                .filter(subReason -> sendbackKey.equals(subReason.getSendbackKey()))
                                .findFirst()
                                .map(subReason -> subReason.getTargetTaskId())
                                .orElse(null))
                        .orElse(null);
                taskInfo.put("targetTaskId", targetTaskId);
                taskInfo.put("key",sendbackKey);
            }

            // Add sourceLoanStage and sourceSubModule if available
            if (sourceLoanStage != null && sourceSubModule != null) {
                taskInfo.put("sourceLoanStage", sourceLoanStage);
                taskInfo.put("sourceSubModule", sourceSubModule);

            }
        }

        return taskInfo; // Return the map containing all required fields
    } //gannt chart


    private TaskDetailsResponse convertToTaskDetails(TaskExecutionLogEntity log) {
        Map<String, String> taskInfo = "sendback".equalsIgnoreCase(log.getTaskId()) ? fetchTargetTaskInfo(log) : new HashMap<>();

        return new TaskDetailsResponse(
                Optional.ofNullable(log.getFunnel()).orElse(UNKNOWN_FUNNEL),
                log.getActorId(),
                log.getStatus(),
                log.getUpdatedAt(),
                log.getTaskId(),
                taskInfo.get("key"),
                taskInfo.get("targetTaskId"),  // Target Task ID
                taskInfo.get("sourceLoanStage"), // Source Loan Stage
                taskInfo.get("sourceSubModule"), // Source SubModule
                log.getMetadata()
        );
    } //gantt chart

    private List<FunnelGroupResponse> groupTasksByFunnel(List<TaskDetailsResponse> sortedTasks) {
        log.info("[groupTasksByFunnel] Grouping {} tasks by funnel", sortedTasks.size());
        Map<String, FunnelGroupResponse> funnelMap = new LinkedHashMap<>();
        for (TaskDetailsResponse task : sortedTasks) {
            funnelMap.computeIfAbsent(task.getFunnel(), key -> new FunnelGroupResponse(task.getFunnel(), new ArrayList<>()))
                    .getTasks().add(task);
        }
        return new ArrayList<>(funnelMap.values());
    }
}
