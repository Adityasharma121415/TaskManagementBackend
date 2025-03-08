package com.cars24.taskmanagement.backend.service;

import com.cars24.taskmanagement.backend.data.entity.ApplicationLog;

import java.util.List;

public interface ApplicationLogService {
    List<ApplicationLog> getLogsByApplicationId(String applicationId);
    void setupChangeStream();
}