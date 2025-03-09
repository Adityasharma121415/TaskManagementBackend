package com.cars24.taskmanagement.backend.data.repository;

import com.cars24.taskmanagement.backend.data.entity.TaskExecutionTimeEntity;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface TaskExecutionTimeRepository extends MongoRepository<TaskExecutionTimeEntity, String> {

    List<TaskExecutionTimeEntity> findByChannel(String channel);

    @Aggregation(pipeline = {
            "{ $match: { channel: ?0 } }",
            "{ $match: { taskId: { $ne: null } } }", // Added filter to exclude documents with null taskId
            "{ $group: { " +
                    "    _id: '$taskId', " +
                    "    totalDuration: { $sum: '$duration' }, " +
                    "    totalVisited: { $sum: '$visited' }, " +
                    "    totalRecords: { $sum: 1 }, " +
                    "    totalSendbacks: { $sum: '$sendbacks' } " +
                    "} }",
            "{ $project: { " +
                    "    taskId: '$_id', " +
                    "    _id: 0, " +
                    "    timeTaken: { $divide: ['$totalDuration', { $add: ['$totalVisited', '$totalRecords'] }] }, " +
                    "    sendbacks: { $divide: ['$totalSendbacks', '$totalRecords'] } " +
                    "} }"
    })
    List<Map<String, Object>> getAggregatedTaskExecutionByChannel(String channel);

    Optional<TaskExecutionTimeEntity> findByApplicationIdAndEntityId(String applicationId, String entityId);
}