package com.cars24.taskmanagement.backend.data.dao.impl;

import com.cars24.taskmanagement.backend.data.dao.ApplicationDao;
import com.cars24.taskmanagement.backend.data.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationDaoImpl implements ApplicationDao {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public List<Log> getSortedLogs(String applicationId) {
        return applicationRepository.findByApplicationIdOrderByUpdatedAtDesc(applicationId);
    }
}