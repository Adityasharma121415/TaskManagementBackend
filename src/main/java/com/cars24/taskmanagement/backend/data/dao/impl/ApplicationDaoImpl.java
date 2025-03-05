package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationDaoImpl implements ApplicationDao {

    private TaskExecutionLogRepository repository;

    // Add explicit constructor for dependency injection
    @Autowired
    public ApplicationDaoImpl(TaskExecutionLogRepository repository) {
        this.repository = repository;
    }

    public List<TaskExecutionLog> findByApplicationId(String applicationId) {
        return repository.findByApplicationId(applicationId);
    }
}
