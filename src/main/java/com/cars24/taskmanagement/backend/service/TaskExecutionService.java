package com.cars24.taskmanagement.backend.service;


import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskExecutionService {
    List<TaskExecutionEntity> findAll();
    Optional<TaskExecutionEntity> findById(String id);
    //TaskExecutionEntity save(TaskExecutionEntity task);
    public TaskExecutionEntity save(TaskExecutionEntity task);
    void updateTaskExecutionTime(
            String taskId, String status, Instant eventTime,
            String funnel, String applicationId, String entityId
    );

    void updateTaskExecutionTime(
            String taskId, String status, Instant createdAt, Instant updatedAt,
            String funnel, String applicationId, String entityId
    );

    void updateTaskExecutionTime(
            String taskId, String status, Instant createdAt, Instant updatedAt,
            String funnel, String applicationId, String entityId, String channel
    );
}