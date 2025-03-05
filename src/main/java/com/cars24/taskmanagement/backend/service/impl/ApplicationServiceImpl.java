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

    public List<Map<String, Object>> getTasksGroupedByFunnel(String applicationId) {
        List<TaskExecutionLog> tasks = taskExecutionDao.findByApplicationId(applicationId);

        // Step 1: Sort tasks in ascending order of updatedAt
        tasks.sort(Comparator.comparing(TaskExecutionLog::getUpdatedAt));

        // Step 2: Transform into required response format
        List<Map<String, Object>> orderedTasks = new ArrayList<>();

        for (TaskExecutionLog task : tasks) {
            Map<String, Object> taskDetails = new LinkedHashMap<>();
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



