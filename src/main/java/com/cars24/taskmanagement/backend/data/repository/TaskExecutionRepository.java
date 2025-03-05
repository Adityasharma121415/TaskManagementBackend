package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskExecutionRepository extends MongoRepository<TaskExecutionEntity, String> {
    // Custom queries if needed
}
