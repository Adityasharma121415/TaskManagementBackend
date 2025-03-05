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

        // Group tasks by funnel
        Map<String, List<Map<String, Object>>> groupedTasks = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getFunnel() != null ? task.getFunnel() : "UNKNOWN",
                        Collectors.mapping(task -> {
                            Map<String, Object> taskDetails = new HashMap<>();
                            taskDetails.put("taskId", task.getTaskId());
                            taskDetails.put("status", task.getStatus());
                            taskDetails.put("actorId", task.getActorId());
                            taskDetails.put("updatedAt", task.getUpdatedAt());
                            taskDetails.put("order", task.getOrder());
                            return taskDetails;
                        }, Collectors.toList())));

        // Create a sorted map based on the minimum order values
        LinkedHashMap<String, List<Map<String, Object>>> sortedByFunnelOrder = new LinkedHashMap<>();

        funnelMinOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    String funnel = entry.getKey();
                    sortedByFunnelOrder.put(funnel, groupedTasks.get(funnel));
                });

        return sortedByFunnelOrder;
    }
}


