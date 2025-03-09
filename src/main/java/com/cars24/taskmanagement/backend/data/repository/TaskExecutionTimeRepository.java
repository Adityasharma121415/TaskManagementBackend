package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskExecutionTimeRepository extends MongoRepository<TaskExecutionTimeEntity, String> {
    Optional<TaskExecutionTimeEntity> findByApplicationIdAndEntityId(String applicationId, String entityId);
    List<TaskExecutionTimeEntity> findByChannel(String channel);
}
