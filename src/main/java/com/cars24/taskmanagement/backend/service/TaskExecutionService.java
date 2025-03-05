package com.cars24.taskmanagement.backend.service;


import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;

import java.util.List;
import java.util.Optional;

public interface TaskExecutionService {
    List<TaskExecutionEntity> findAll();
    Optional<TaskExecutionEntity> findById(String id);
    TaskExecutionEntity save(TaskExecutionEntity task);
}