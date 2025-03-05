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

        return tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getFunnel() != null ? task.getFunnel() : "UNKNOWN",  // Handle null funnel values
                        Collectors.mapping(task -> {
                            Map<String, Object> taskDetails = new HashMap<>();
                            taskDetails.put("taskId", task.getTaskId());
                            taskDetails.put("status", task.getStatus());
                            taskDetails.put("actorId", task.getActorId());
                            taskDetails.put("updatedAt", task.getUpdatedAt());
                            return taskDetails;
                        }, Collectors.toList())));
    }


}



