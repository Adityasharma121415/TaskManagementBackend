package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Repository
public interface TaskExecutionLogRepository extends MongoRepository<TaskExecutionLogEntity, String> {

    @Aggregation(pipeline = {
            "{ $match: { applicationId: ?0 } }",
            "{ $sort: { updatedAt: 1 } }"
    })
    List<TaskExecutionLogEntity> findTasksByApplicationIdSortedByUpdatedAt(String applicationId);

    List<TaskExecutionLogEntity> findByApplicationId(String applicationId);

    // Custom query to find tasks by taskIds, status, and updatedAt > afterTime
    @Query("{ 'taskId': { $in: ?0 }, 'status': ?1, 'applicationId': ?2, 'updatedAt': { $gt: ?3 } }")
    List<TaskExecutionLogEntity> findTasksByTaskIdsAndStatusAndApplicationIdAfterTime(
            Set<String> taskIds, String status, String applicationId, Date afterTime);
}