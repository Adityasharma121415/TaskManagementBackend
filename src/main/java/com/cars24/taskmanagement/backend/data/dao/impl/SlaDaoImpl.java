package com.cars24.taskmanagement.backend.data.dao.impl;


import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionTimeRepository;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SlaDaoImpl {

    private final TaskExecutionTimeRepository repository;

    public SlaDaoImpl(TaskExecutionTimeRepository repository) {
        this.repository = repository;
    }

    public List<TaskExecutionTimeEntity> getTasksByChannel(String channel) {
        return repository.findByChannel(channel);
    }
}
