package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationDaoImpl implements ApplicationDao {


    private final TaskExecutionLogRepository repository;

    public List<TaskExecutionLog> findByApplicationId(String applicationId) {
        return repository.findByApplicationIdOrderByUpdatedAt(applicationId);
    }
}
