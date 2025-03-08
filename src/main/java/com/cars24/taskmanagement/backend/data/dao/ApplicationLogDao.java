package com.cars24.taskmanagement.backend.data.dao;

import com.cars24.taskmanagement.backend.data.entity.ApplicationLog;

import java.util.List;

public interface ApplicationLogDao {
    List<ApplicationLog> getLogsByApplicationId(String applicationId);
}