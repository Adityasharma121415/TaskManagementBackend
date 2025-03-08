package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.ApplicationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ApplicationLogRepository extends MongoRepository<ApplicationLog, String> {
    List<ApplicationLog> findByApplicationId(String applicationId);
}