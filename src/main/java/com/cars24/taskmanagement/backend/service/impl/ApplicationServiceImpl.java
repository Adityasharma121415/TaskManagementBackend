package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.dao.impl.SendbackConfigDao;
import com.cars24.taskmanagement.backend.data.entity.SendbackConfig;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.response.FunnelGroup;
import com.cars24.taskmanagement.backend.data.response.TaskDetails;
import com.cars24.taskmanagement.backend.data.response.TasksResponse;
import com.cars24.taskmanagement.backend.data.response.dto.StatusLogResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final String UNKNOWN_FUNNEL = "Unknown Funnel";
    private final ApplicationDao taskExecutionDao;
    private final SendbackConfigDao sendbackConfigDao;

    @Override
    public TasksResponse getTasksByApplicationId(String applicationId) {
        List<TaskExecutionLog> sortedTasks = taskExecutionDao.findTasksByApplicationIdSortedByUpdatedAt(applicationId);
        List<TaskDetails> taskDetailsList = sortedTasks.stream()
                .map(this::convertToTaskDetails)
                .collect(Collectors.toList());
        List<FunnelGroup> funnelGroups = groupTasksByFunnel(taskDetailsList);
        TasksResponse response = new TasksResponse();
        response.setFunnelGroups(funnelGroups);
        return response;
    }

    public Map<String, List<TaskResponse>> getTasksGroupedByFunnel(String applicationId) {
        List<TaskExecutionLog> tasks = taskExecutionDao.findByApplicationId(applicationId);

        Map<String, Integer> funnelMinOrders = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse("UNKNOWN"),
                        Collectors.mapping(TaskExecutionLog::getOrder, Collectors.minBy(Integer::compare))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().orElse(Integer.MAX_VALUE)
                ));

        Map<String, Map<String, List<TaskExecutionLog>>> tasksByFunnelAndId = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse("UNKNOWN"),
                        Collectors.groupingBy(TaskExecutionLog::getTaskId)
                ));

        LinkedHashMap<String, List<TaskResponse>> result = new LinkedHashMap<>();
        funnelMinOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(funnelEntry -> {
                    String funnel = funnelEntry.getKey();
                    Map<String, List<TaskExecutionLog>> tasksByIdInFunnel = tasksByFunnelAndId.get(funnel);
                    List<TaskResponse> funnelTasks = new ArrayList<>();
                    if (tasksByIdInFunnel != null) {
                        Map<String, Integer> taskOrders = tasksByIdInFunnel.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().get(0).getOrder()
                                ));
                        taskOrders.entrySet().stream()
                                .sorted(Map.Entry.comparingByValue())
                                .forEach(taskEntry -> {
                                    String taskId = taskEntry.getKey();
                                    List<TaskExecutionLog> taskLogs = tasksByIdInFunnel.get(taskId);
                                    TaskExecutionLog firstLog = taskLogs.get(0);

                                    List<StatusLogResponse> statusLogs = taskLogs.stream()
                                            .sorted(Comparator.comparing(TaskExecutionLog::getUpdatedAt))
                                            .map(log -> new StatusLogResponse(log.getStatus(), log.getUpdatedAt()))
                                            .collect(Collectors.toList());

                                    TaskResponse taskResponse = new TaskResponse(
                                            taskId,
                                            firstLog.getOrder(),
                                            firstLog.getHandledBy(),
                                            firstLog.getCreatedAt(),
                                            statusLogs
                                    );
                                    funnelTasks.add(taskResponse);
                                });
                    }
                    result.put(funnel, funnelTasks);
                });

        return result;
    }

    private List<FunnelGroup> groupTasksByFunnel(List<TaskDetails> sortedTasks) {
        List<FunnelGroup> funnelGroups = new ArrayList<>();
        if (sortedTasks.isEmpty()) {
            return funnelGroups;
        }
        String currentFunnel = null;
        FunnelGroup currentGroup = null;
        for (TaskDetails task : sortedTasks) {
            String taskFunnel = (task.getFunnel() != null) ? task.getFunnel() : UNKNOWN_FUNNEL;
            if (currentFunnel == null || !currentFunnel.equals(taskFunnel)) {
                currentFunnel = taskFunnel;
                currentGroup = new FunnelGroup();
                currentGroup.setFunnelName(taskFunnel);
                currentGroup.setTasks(new ArrayList<>());
                funnelGroups.add(currentGroup);
            }
            currentGroup.getTasks().add(task);
        }
        return funnelGroups;
    }

    private TaskDetails convertToTaskDetails(TaskExecutionLog log) {
        TaskDetails details = new TaskDetails();
        details.setTaskId(log.getTaskId());
        details.setFunnel(log.getFunnel() != null ? log.getFunnel() : UNKNOWN_FUNNEL);
        details.setActorId(log.getActorId());
        details.setStatus(log.getStatus());
        details.setUpdatedAt(log.getUpdatedAt());
        details.setMetadata(log.getMetadata());

        if ("sendback".equalsIgnoreCase(log.getTaskId())) {
            Map<String, Object> sendbackMetadata = (Map<String, Object>) log.getMetadata().get("sendbackMetadata");

            if (sendbackMetadata != null && sendbackMetadata.containsKey("key")) {
                String sendbackKey = (String) sendbackMetadata.get("key");

                if (sendbackKey != null) {
                    Optional<SendbackConfig> sendbackConfigOpt = sendbackConfigDao.findBySendbackKey(sendbackKey);

                    if (sendbackConfigOpt.isPresent()) {
                        SendbackConfig config = sendbackConfigOpt.get();

                        if (!config.getSubReasonList().isEmpty()) {
                            details.setTargetTaskId(config.getSubReasonList().get(0).getTargetTaskId());
                        }
                    }
                }
            }
        }

        return details;
    }

}
