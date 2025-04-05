package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.constants.SubModuleTaskMapping;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionLogRepository;
import com.cars24.taskmanagement.backend.data.dao.impl.SendbackConfigDao;
import com.cars24.taskmanagement.backend.data.response.applicationDto.FunnelGroupResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.ListFunnelGroupResponse;
import com.cars24.taskmanagement.backend.data.response.applicationDto.TaskDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationGanntService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationGanntService.class);
    private static final String UNKNOWN_FUNNEL = "Unknown Funnel";

    private final TaskExecutionLogRepository taskExecutionLogRepository;
    private final SendbackConfigDao sendbackConfigDao;

    public ListFunnelGroupResponse getTasksByApplicationId(String applicationId) {
        logger.info("[getTasksByApplicationId] Starting to fetch tasks for applicationId={}", applicationId);

        // Fetch tasks sorted by updatedAt
        List<TaskExecutionLogEntity> sortedTasks = taskExecutionLogRepository.findTasksByApplicationIdSortedByUpdatedAt(applicationId);
        logger.info("[getTasksByApplicationId] Fetched {} tasks for applicationId={}", sortedTasks.size(), applicationId);

        // Map to track source tasks by their unique ObjectId
        Map<String, TaskDetailsResponse> sourceTaskMap = new HashMap<>();

        // Map to track pending updates for source tasks
        Map<String, Map<String, String>> pendingUpdatesMap = new HashMap<>();

        // Process tasks
        List<TaskDetailsResponse> taskDetailsResponseList = new ArrayList<>();
        for (TaskExecutionLogEntity log : sortedTasks) {
            TaskDetailsResponse response = processTask(log, sourceTaskMap, pendingUpdatesMap);
            if (response != null) {
                taskDetailsResponseList.add(response);
            }
        }

        // Group tasks by funnel
        List<FunnelGroupResponse> funnelGroupResponses = groupTasksByFunnel(taskDetailsResponseList);
        logger.info("[getTasksByApplicationId] Grouped tasks into {} funnel groups for applicationId={}", funnelGroupResponses.size(), applicationId);

        logger.info("[getTasksByApplicationId] Completed processing for applicationId={}", applicationId);
        return new ListFunnelGroupResponse(funnelGroupResponses);
    }

    private TaskDetailsResponse processTask(TaskExecutionLogEntity log, Map<String, TaskDetailsResponse> sourceTaskMap, Map<String, Map<String, String>> pendingUpdatesMap) {
        logger.info("[processTask] Processing taskId={} with id={}", log.getTaskId(), log.getId());

        // Process tasks with taskId: sendback
        if ("sendback".equalsIgnoreCase(log.getTaskId())) {
            logger.info("[processTask] Processing sendback task with taskId: sendback");

            // Fetch additional task info for sendback tasks
            Map<String, String> taskInfo = fetchTargetTaskInfo(log);

            // Get sourceTaskId and targetTaskId from the taskInfo
            String sourceTaskId = taskInfo.get("sourceTaskId");
            String targetTaskId = taskInfo.get("targetTaskId");

            // Log the mapping of targetTaskId to sourceTaskId
            logger.info("[processTask] Mapped targetTaskId={} to sourceTaskId={}", targetTaskId, sourceTaskId);

            // Check if the source task is already in the map
            if (sourceTaskId != null && sourceTaskMap.containsKey(sourceTaskId)) {
                TaskDetailsResponse sourceTaskResponse = sourceTaskMap.get(sourceTaskId);
                sourceTaskResponse.setTargetTaskId(targetTaskId);
                sourceTaskResponse.setSourceLoanStage(taskInfo.get("sourceLoanStage"));
                sourceTaskResponse.setSourceSubModule(taskInfo.get("sourceSubModule"));
                logger.info("[processTask] Updated source task with id={} in the response", sourceTaskId);
            } else {
                // If the source task is not yet available, store the update in the pendingUpdatesMap
                logger.warn("[processTask] Source task with id={} not found in the response map. Storing update in pendingUpdatesMap.", sourceTaskId);
                pendingUpdatesMap.put(sourceTaskId, taskInfo);
            }

            // Skip adding sendback tasks to the response
            logger.info("[processTask] Skipping sendback task from the response");
            return null;
        }

        // Process non-sendback tasks
        Map<String, String> taskInfo = fetchTargetTaskInfo(log);

        // Create TaskDetailsResponse for non-sendback tasks
        TaskDetailsResponse response = new TaskDetailsResponse(
                Optional.ofNullable(log.getFunnel()).orElse(UNKNOWN_FUNNEL),
                log.getActorId(),
                log.getStatus(),
                log.getUpdatedAt(),
                log.getTaskId(),
                taskInfo.get("key"), // Reason for sendback (if applicable)
                taskInfo.get("targetTaskId"), // Target Task ID assigned to the source task
                taskInfo.get("sourceLoanStage"), // Source Loan Stage
                taskInfo.get("sourceSubModule"), // Source SubModule
                log.getMetadata() // Original metadata
        );

        // Add the response to the sourceTaskMap using ObjectId
        sourceTaskMap.put(log.getId(), response);
        logger.info("[processTask] Added taskId={} with id={} to the response map", log.getTaskId(), log.getId());

        // Check if there are any pending updates for this task
        if (pendingUpdatesMap.containsKey(log.getId())) {
            Map<String, String> pendingUpdates = pendingUpdatesMap.get(log.getId());
            response.setTargetTaskId(pendingUpdates.get("targetTaskId"));
            response.setSourceLoanStage(pendingUpdates.get("sourceLoanStage"));
            response.setSourceSubModule(pendingUpdates.get("sourceSubModule"));
            logger.info("[processTask] Applied pending updates to task with id={}", log.getId());

            // Remove the updates from the pendingUpdatesMap
            pendingUpdatesMap.remove(log.getId());
        }

        return response;
    }

    private Map<String, String> fetchTargetTaskInfo(TaskExecutionLogEntity log) {
        logger.info("[fetchTargetTaskInfo] Processing taskId={} for sendback metadata", log.getTaskId());
        Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getSendbackMetadata();
        Map<String, String> taskInfo = new HashMap<>();

        if (sendbackMetadata != null) {
            logger.info("[fetchTargetTaskInfo] Sendback Metadata: {}", sendbackMetadata);

            // Extract metadata
            String sourceLoanStage = (String) sendbackMetadata.get("sourceLoanStage");
            String sourceSubModule = (String) sendbackMetadata.get("sourceSubModule");
            String key = (String) sendbackMetadata.get("key");
            Date initiatedAt = (Date) sendbackMetadata.get("initiatedAt");

            logger.info("[fetchTargetTaskInfo] Extracted Metadata - SourceLoanStage: {}, SourceSubModule: {}, Key: {}, InitiatedAt: {}",
                    sourceLoanStage, sourceSubModule, key, initiatedAt);

            // Get source task IDs from SubModuleTaskMapping
            Set<String> sourceTaskIds = SubModuleTaskMapping.SUBMODULE_TASK_MAP.get(sourceSubModule);
            logger.info("[fetchTargetTaskInfo] Source Task IDs for SubModule {}: {}", sourceSubModule, sourceTaskIds);

            if (sourceTaskIds != null && !sourceTaskIds.isEmpty()) {
                // Query TaskExecutionLog for source tasks with status: sendback
                List<TaskExecutionLogEntity> sourceTasks = taskExecutionLogRepository.findTasksByTaskIdsAndStatusAfterTime(
                        sourceTaskIds, "SENDBACK", initiatedAt
                );
                logger.info("[fetchTargetTaskInfo] Found {} source tasks with status 'sendback' after initiatedAt={}", sourceTasks.size(), initiatedAt);

                // Find the source task with the latest updatedAt
                TaskExecutionLogEntity sourceTask = sourceTasks.stream()
                        .max(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                        .orElse(null);

                if (sourceTask != null) {
                    logger.info("[fetchTargetTaskInfo] Latest Source Task ID: {}", sourceTask.getTaskId());

                    // Query SendbackConfig for target task IDs
                    List<String> targetTaskIds = sendbackConfigDao.findBySendbackKey(key)
                            .map(config -> config.getSubReasonList().stream()
                                    .filter(subReason -> key.equals(subReason.getSendbackKey()))
                                    .findFirst()
                                    .map(subReason -> subReason.getTargetTaskIds())
                                    .orElse(Collections.emptyList()))
                            .orElse(Collections.emptyList());

                    logger.info("[fetchTargetTaskInfo] Target Task IDs for key {}: {}", key, targetTaskIds);

                    // Pick a target task ID (random for now)
                    String targetTaskId = targetTaskIds.isEmpty() ? null : targetTaskIds.get(0);
                    logger.info("[fetchTargetTaskInfo] Selected Target Task ID: {}", targetTaskId);

                    // Populate taskInfo
                    taskInfo.put("sourceTaskId", sourceTask.getId());
                    taskInfo.put("targetTaskId", targetTaskId);
                    taskInfo.put("sourceLoanStage", sourceLoanStage);
                    taskInfo.put("sourceSubModule", sourceSubModule);
                } else {
                    logger.warn("[fetchTargetTaskInfo] No source task found for SubModule {} and key {}", sourceSubModule, key);
                }
            } else {
                logger.warn("[fetchTargetTaskInfo] No source task IDs found for SubModule {}", sourceSubModule);
            }
        } else {
            logger.warn("[fetchTargetTaskInfo] No sendback metadata found for taskId={}", log.getTaskId());
        }

        return taskInfo;
    }

    private List<FunnelGroupResponse> groupTasksByFunnel(List<TaskDetailsResponse> sortedTasks) {
        logger.info("[groupTasksByFunnel] Grouping {} tasks by funnel", sortedTasks.size());
        Map<String, FunnelGroupResponse> funnelMap = new LinkedHashMap<>();
        for (TaskDetailsResponse task : sortedTasks) {
            funnelMap.computeIfAbsent(task.getFunnel(), key -> new FunnelGroupResponse(task.getFunnel(), new ArrayList<>()))
                    .getTasks().add(task);
        }
        logger.info("[groupTasksByFunnel] Grouped tasks into {} funnels", funnelMap.size());
        return new ArrayList<>(funnelMap.values());
    }
}