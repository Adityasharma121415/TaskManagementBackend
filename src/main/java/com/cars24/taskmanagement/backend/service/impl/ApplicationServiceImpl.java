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

    public List<Map<String, Object>> getTasksOrderedByTime(String applicationId) {
        List<TaskExecutionLog> tasks = taskExecutionDao.findByApplicationId(applicationId);

        // Step 1: Sort all tasks by updatedAt (oldest first)
        tasks.sort(Comparator.comparing(TaskExecutionLog::getUpdatedAt));

        // Step 2: Maintain task order and group them under their respective funnels
        List<Map<String, Object>> orderedTasks = new ArrayList<>();

        for (TaskExecutionLog task : tasks) {
            Map<String, Object> taskDetails = new LinkedHashMap<>(); // Use LinkedHashMap for order consistency
            taskDetails.put("funnel", task.getFunnel() != null ? task.getFunnel() : "UNKNOWN");
            taskDetails.put("taskId", task.getTaskId());
            taskDetails.put("status", task.getStatus());
            taskDetails.put("actorId", task.getActorId());
            taskDetails.put("updatedAt", task.getUpdatedAt());

            orderedTasks.add(taskDetails);
        }

        return orderedTasks;
    }
}




