package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Repository
public class ApplicationDaoImpl implements ApplicationDao {

    @Autowired
    private TaskExecutionLogRepository taskExecutionLogRepository;

    @Override
    public List<TaskExecutionLog> findTasksByApplicationIdSortedByUpdatedAt(String applicationId) {
        return taskExecutionLogRepository.findTasksByApplicationIdSortedByUpdatedAt(applicationId);
    }
}

