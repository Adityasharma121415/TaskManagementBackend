package com.cars24.taskmanagement.backend.service.impl;
import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.response.dto.StatusLogResponse;
import com.cars24.taskmanagement.backend.data.response.dto.TaskResponse;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    @Autowired
    private ApplicationDao taskExecutionDao;

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

        // Group tasks by funnel and then by taskId
        Map<String, Map<String, List<TaskExecutionLog>>> tasksByFunnelAndId = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> Optional.ofNullable(task.getFunnel()).orElse("UNKNOWN"),
                        Collectors.groupingBy(TaskExecutionLog::getTaskId)
                ));

        // Create the final response
        LinkedHashMap<String, List<TaskResponse>> result = new LinkedHashMap<>();

        // Sort funnels by their minimum order
        funnelMinOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(funnelEntry -> {
                    String funnel = funnelEntry.getKey();
                    Map<String, List<TaskExecutionLog>> tasksByIdInFunnel = tasksByFunnelAndId.get(funnel);

                    // List to hold tasks for this funnel
                    List<TaskResponse> funnelTasks = new ArrayList<>();

                    if (tasksByIdInFunnel != null) {
                        // Get all unique task IDs in this funnel and their order
                        Map<String, Integer> taskOrders = tasksByIdInFunnel.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().get(0).getOrder()
                                ));

                        // Sort tasks by order and transform them into DTOs
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
}
