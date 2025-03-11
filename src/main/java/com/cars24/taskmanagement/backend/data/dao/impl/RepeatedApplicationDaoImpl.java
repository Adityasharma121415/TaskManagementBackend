package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import com.cars24.taskmanagement.backend.data.repository.TaskExecutionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RepeatedApplicationDaoImpl implements ApplicationDao {

    @Autowired
    private TaskExecutionLogRepository taskExecutionLogRepository;

    @Override
    public List<TaskExecutionLog> findTasksByApplicationIdSortedByUpdatedAt(String applicationId) {
        return taskExecutionLogRepository.findTasksByApplicationIdSortedByUpdatedAt(applicationId);
    }
}

