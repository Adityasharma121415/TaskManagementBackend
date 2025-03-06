package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;



@Service
public class ApplicationServiceImpl implements ApplicationService {

    @Autowired
    private ApplicationDao taskExecutionDao;

    public Map<String, List<Map<String, Object>>> getTasksGroupedByFunnel(String applicationId) {
        List<TaskExecutionLog> tasks = taskExecutionDao.findByApplicationId(applicationId);

        // First, calculate the minimum order for each funnel
        Map<String, Integer> funnelMinOrders = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getFunnel() != null ? task.getFunnel() : "UNKNOWN",
                        Collectors.mapping(TaskExecutionLog::getOrder, Collectors.minBy(Integer::compare))))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().orElse(Integer.MAX_VALUE)
                ));

        // Group tasks by funnel and then by taskId
        Map<String, Map<String, List<TaskExecutionLog>>> tasksByFunnelAndId = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getFunnel() != null ? task.getFunnel() : "UNKNOWN",
                        Collectors.groupingBy(TaskExecutionLog::getTaskId)
                ));

        // Create the final result structure
        LinkedHashMap<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        // Sort funnels by their minimum order
        funnelMinOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(funnelEntry -> {
                    String funnel = funnelEntry.getKey();
                    Map<String, List<TaskExecutionLog>> tasksByIdInFunnel = tasksByFunnelAndId.get(funnel);

                    // Create a list to hold all tasks in this funnel
                    List<Map<String, Object>> funnelTasks = new ArrayList<>();

                    // Get all unique task IDs in this funnel and their corresponding order
                    Map<String, Integer> taskOrders = tasksByIdInFunnel.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().get(0).getOrder() // Assuming all logs for a task have the same order
                            ));

                    // Sort tasks by their order
                    taskOrders.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .forEach(taskEntry -> {
                                String taskId = taskEntry.getKey();
                                List<TaskExecutionLog> taskLogs = tasksByIdInFunnel.get(taskId);

                                // Get the first log to extract task details (assuming they're the same across logs)
                                TaskExecutionLog firstLog = taskLogs.get(0);

                                Map<String, Object> taskDetails = new HashMap<>();
                                taskDetails.put("taskId", taskId);
                                taskDetails.put("order", firstLog.getOrder());
                                taskDetails.put("handledBy", firstLog.getHandledBy());
                                taskDetails.put("createdAt", firstLog.getCreatedAt());

                                // Sort status logs by updatedAt (oldest first)
                                List<Map<String, Object>> statusLogs = taskLogs.stream()
                                        .sorted(Comparator.comparing(TaskExecutionLog::getUpdatedAt))
                                        .map(log -> {
                                            Map<String, Object> statusLog = new HashMap<>();
                                            statusLog.put("status", log.getStatus());
                                            statusLog.put("updatedAt", log.getUpdatedAt());
                                            return statusLog;
                                        })
                                        .collect(Collectors.toList());

                                taskDetails.put("statusHistory", statusLogs);
                                funnelTasks.add(taskDetails);
                            });

                    result.put(funnel, funnelTasks);
                });

        return result;
    }
}