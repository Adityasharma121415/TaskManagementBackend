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

        // Group tasks by funnel and determine the minimum updatedAt for each funnel
        Map<String, FunnelGroupWithMinUpdatedAt> funnelMap = new LinkedHashMap<>();
        for (TaskDetailsResponse task : taskDetailsResponseList) {
            funnelMap.computeIfAbsent(task.getFunnel(), key -> new FunnelGroupWithMinUpdatedAt(task.getFunnel(), new ArrayList<>(), task.getUpdatedAt()))
                    .update(task);
        }
        logger.info("[getTasksByApplicationId] Grouped tasks into {} funnels", funnelMap.size());

        // Define desired funnel order
        List<String> funnelOrder = Arrays.asList("SOURCING", "CREDIT", "CONVERSION", "RISK", "FULFILMENT", "RTO", "DISBURSAL");

        // Sort funnels based on the hardcoded order
        List<FunnelGroupResponse> funnelGroupResponses = funnelMap.values().stream()
                .sorted(Comparator.comparingInt(fg -> {
                    int index = funnelOrder.indexOf(fg.funnel.toUpperCase());
                    return index == -1 ? Integer.MAX_VALUE : index;
                }))
                .map(FunnelGroupWithMinUpdatedAt::toFunnelGroupResponse)
                .collect(Collectors.toList());

        logger.info("[getTasksByApplicationId] Grouped and sorted tasks into {} funnel groups for applicationId={}", funnelGroupResponses.size(), applicationId);
        logger.info("[getTasksByApplicationId] Completed processing for applicationId={}", applicationId);

        return new ListFunnelGroupResponse(funnelGroupResponses);
    }

    private TaskDetailsResponse processTask(TaskExecutionLogEntity log, Map<String, TaskDetailsResponse> sourceTaskMap, Map<String, Map<String, String>> pendingUpdatesMap) {
        logger.info("[processTask] Processing taskId={} with id={}", log.getTaskId(), log.getId());

        if ("sendback".equalsIgnoreCase(log.getTaskId())) {
            logger.info("[processTask] Processing sendback task with taskId: sendback");
            Map<String, String> taskInfo = fetchTargetTaskInfo(log);
            String sourceTaskId = taskInfo.get("sourceTaskId");
            String targetTaskId = taskInfo.get("targetTaskId");
            logger.info("[processTask] Mapped targetTaskId={} to sourceTaskId={}", targetTaskId, sourceTaskId);

            if (sourceTaskId != null && sourceTaskMap.containsKey(sourceTaskId)) {
                TaskDetailsResponse sourceTaskResponse = sourceTaskMap.get(sourceTaskId);
                sourceTaskResponse.setTargetTaskId(targetTaskId);
                sourceTaskResponse.setSourceLoanStage(taskInfo.get("sourceLoanStage"));
                sourceTaskResponse.setSourceSubModule(taskInfo.get("sourceSubModule"));
                logger.info("[processTask] Updated source task with id={} in the response", sourceTaskId);
            } else if (sourceTaskId != null) {
                logger.warn("[processTask] Source task with id={} not found in the response map. Storing update in pendingUpdatesMap.", sourceTaskId);
                pendingUpdatesMap.put(sourceTaskId, taskInfo);
            }
            logger.info("[processTask] Skipping sendback task from the response");
            return null;
        }

        Map<String, String> taskInfo = fetchTargetTaskInfo(log);
        TaskDetailsResponse response = new TaskDetailsResponse(
                Optional.ofNullable(log.getFunnel()).orElse(UNKNOWN_FUNNEL),
                log.getActorId(),
                log.getStatus(),
                log.getUpdatedAt(),
                log.getTaskId(),
                taskInfo.get("key"),
                taskInfo.get("targetTaskId"),
                taskInfo.get("sourceLoanStage"),
                taskInfo.get("sourceSubModule"),
                log.getMetadata()
        );

        sourceTaskMap.put(log.getId(), response);
        logger.info("[processTask] Added taskId={} with id={} to the response map", log.getTaskId(), log.getId());

        if (pendingUpdatesMap.containsKey(log.getId())) {
            Map<String, String> pendingUpdates = pendingUpdatesMap.get(log.getId());
            response.setTargetTaskId(pendingUpdates.get("targetTaskId"));
            response.setSourceLoanStage(pendingUpdates.get("sourceLoanStage"));
            response.setSourceSubModule(pendingUpdates.get("sourceSubModule"));
            logger.info("[processTask] Applied pending updates to task with id={}", log.getId());
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
            String sourceLoanStage = (String) sendbackMetadata.get("sourceLoanStage");
            String sourceSubModule = (String) sendbackMetadata.get("sourceSubModule");
            String key = (String) sendbackMetadata.get("key");
            Date initiatedAt = (Date) sendbackMetadata.get("initiatedAt");
            logger.info("[fetchTargetTaskInfo] Extracted Metadata - SourceLoanStage: {}, SourceSubModule: {}, Key: {}, InitiatedAt: {}",
                    sourceLoanStage, sourceSubModule, key, initiatedAt);

            // Fetch potential source task IDs
            Set<String> sourceTaskIds = SubModuleTaskMapping.SUBMODULE_TASK_MAP.get(sourceSubModule);
            logger.info("[fetchTargetTaskInfo] Source Task IDs for SubModule {}: {}", sourceSubModule, sourceTaskIds);

            if (sourceTaskIds != null && !sourceTaskIds.isEmpty()) {
                // Query the database for actual source tasks
                List<TaskExecutionLogEntity> sourceTasks = taskExecutionLogRepository.findTasksByTaskIdsAndStatusAndApplicationIdAfterTime(
                        sourceTaskIds, "SENDBACK", log.getApplicationId(), initiatedAt
                );
                logger.info("[fetchTargetTaskInfo] Found {} source tasks with status 'sendback' after initiatedAt={}", sourceTasks.size(), initiatedAt);

                // Select the most recent source task based on updatedAt
                TaskExecutionLogEntity latestSourceTask = sourceTasks.stream()
                        .max(Comparator.comparing(TaskExecutionLogEntity::getUpdatedAt))
                        .orElse(null);

                if (latestSourceTask != null) {
                    logger.info("[fetchTargetTaskInfo] Latest Source Task ID: {}", latestSourceTask.getTaskId());

                    // Fetch target task IDs using the key
                    List<String> targetTaskIds = sendbackConfigDao.findBySendbackKey(key)
                            .map(config -> config.getSubReasonList().stream()
                                    .filter(subReason -> key.equals(subReason.getSendbackKey()))
                                    .findFirst()
                                    .map(subReason -> subReason.getTargetTaskIds())
                                    .orElse(Collections.emptyList()))
                            .orElse(Collections.emptyList());
                    logger.info("[fetchTargetTaskInfo] Target Task IDs for key {}: {}", key, targetTaskIds);

                    String targetTaskId = targetTaskIds.isEmpty() ? null : targetTaskIds.get(0);
                    logger.info("[fetchTargetTaskInfo] Selected Target Task ID: {}", targetTaskId);

                    // Populate the taskInfo map
                    taskInfo.put("sourceTaskId", latestSourceTask.getId());
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

    private static class FunnelGroupWithMinUpdatedAt {
        private final String funnel;
        private final List<TaskDetailsResponse> tasks;
        private Date minUpdatedAt;

        public FunnelGroupWithMinUpdatedAt(String funnel, List<TaskDetailsResponse> tasks, Date initialUpdatedAt) {
            this.funnel = funnel;
            this.tasks = tasks;
            this.minUpdatedAt = initialUpdatedAt;
        }

        public void update(TaskDetailsResponse task) {
            this.tasks.add(task);
            if (this.minUpdatedAt == null || (task.getUpdatedAt() != null && task.getUpdatedAt().before(this.minUpdatedAt))) {
                this.minUpdatedAt = task.getUpdatedAt();
            }
        }

        public FunnelGroupResponse toFunnelGroupResponse() {
            return new FunnelGroupResponse(this.funnel, this.tasks);
        }
    }
}