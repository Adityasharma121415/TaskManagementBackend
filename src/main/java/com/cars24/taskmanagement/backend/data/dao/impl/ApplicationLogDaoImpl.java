package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationLogDao;
import com.cars24.taskmanagement.backend.data.entity.ApplicationLog;
import com.cars24.taskmanagement.backend.data.repository.ApplicationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicationLogDaoImpl implements ApplicationLogDao {

    @Autowired
    private ApplicationLogRepository repository;

    @Override
    public List<ApplicationLog> getLogsByApplicationId(String applicationId) {
        return repository.findByApplicationId(applicationId);
    }
}