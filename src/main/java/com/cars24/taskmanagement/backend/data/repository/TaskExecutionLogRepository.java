package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLog;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskExecutionLogRepository extends MongoRepository<TaskExecutionLog, String> {

    @Aggregation(pipeline = {
            "{ $match: { applicationId: ?0 } }",
            "{ $sort: { updatedAt: 1 } }"
    })
    List<TaskExecutionLog> findTasksByApplicationIdSortedByUpdatedAt(String applicationId);
}