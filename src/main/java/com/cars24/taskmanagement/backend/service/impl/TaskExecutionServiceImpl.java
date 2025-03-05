package com.cars24.taskmanagement.backend.service.impl;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskExecutionServiceImpl {
    private final TaskExecutionRepository repository;

    public TaskExecutionEntity saveTask(TaskExecutionEntity task) {
        return repository.save(task);
    }

    public Optional<TaskExecutionEntity> getTaskById(String id) {
        return repository.findById(id);
    }

    public List<TaskExecutionEntity> getAllTasks() {
        return repository.findAll();
    }
}
