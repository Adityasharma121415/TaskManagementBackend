package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationDao taskExecutionDao;

    public LinkedHashMap<String, List<Map<String, Object>>> getTasksGroupedByFunnel(String applicationId) {
        List<TaskExecutionLog> tasks = taskExecutionDao.findByApplicationId(applicationId);

        // Step 1: Sort tasks by updatedAt in ascending order (oldest first)
        tasks.sort(Comparator.comparing(TaskExecutionLog::getUpdatedAt));

        // Step 2: Group tasks by funnel while maintaining insertion order
        LinkedHashMap<String, List<Map<String, Object>>> groupedTasks = new LinkedHashMap<>();

        for (TaskExecutionLog task : tasks) {
            String funnel = task.getFunnel() != null ? task.getFunnel() : "UNKNOWN";

            groupedTasks.putIfAbsent(funnel, new ArrayList<>()); // Ensure funnel exists

            Map<String, Object> taskDetails = new LinkedHashMap<>(); // Use LinkedHashMap for order consistency
            taskDetails.put("taskId", task.getTaskId());
            taskDetails.put("status", task.getStatus());
            taskDetails.put("actorId", task.getActorId());
            taskDetails.put("updatedAt", task.getUpdatedAt());

            groupedTasks.get(funnel).add(taskDetails);
        }

        return groupedTasks;
    }
}



