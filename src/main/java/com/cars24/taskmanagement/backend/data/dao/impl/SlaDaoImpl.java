package com.cars24.taskmanagement.backend.data.dao.impl;


import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class SlaDaoImpl {

    private final TaskExecutionTimeRepository repository;

    public SlaDaoImpl(TaskExecutionTimeRepository repository) {
        this.repository = repository;
    }

    public List<TaskExecutionTimeEntity> getAllTasks() {
        return repository.findAll();
    }

    public Optional<TaskExecutionTimeEntity> getTaskByApplicationId(String applicationId, String entityId) {
        return repository.findByApplicationIdAndEntityId(applicationId, entityId);
    }
}
