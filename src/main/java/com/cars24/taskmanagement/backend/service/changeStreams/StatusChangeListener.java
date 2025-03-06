package com.cars24.taskmanagement.backend.service.changeStreams;

import com.cars24.taskmanagement.backend.service.redisCache.RedisCacheService;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class StatusChangeListener {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisCacheService redisCacheService;

    @PostConstruct
    public void watchTaskExecutionLog() {
        log.info("StatusChangeListener [watchTaskExecutionLog] started ...");
        new Thread(() -> {
            mongoTemplate.getCollection("task_execution_log")
                    .watch(List.of())
                    .forEach(this::processChange);
        }).start();
    }

    private void processChange(ChangeStreamDocument<Document> change) {
        log.info("StatusChangeListener [processChange] {}", change);

        if (change.getOperationType() == OperationType.INSERT) {
            Document fullDocument = change.getFullDocument();
            if (fullDocument != null) {
                String actorId = fullDocument.getString("actorId");
                String applicationId = fullDocument.getString("applicationId");
                String taskId = fullDocument.getString("taskId");
                String status = fullDocument.getString("status");
                Instant updatedAt = fullDocument.getDate("updatedAt").toInstant();

                if ("NEW".equals(status)) {
                    redisCacheService.storeTaskStartTime(applicationId, taskId, actorId, updatedAt);
                } else if ("COMPLETED".equals(status) || "FAILED".equals(status) || "SENDBACK".equals(status)) {
                    Instant startUpdatedAt = redisCacheService.getTaskStartTime(applicationId, taskId, actorId);
                    if (startUpdatedAt != null) {
                        long duration = updatedAt.toEpochMilli() - startUpdatedAt.toEpochMilli();
                        updateActorMetrics(applicationId, taskId, actorId, duration);
                        redisCacheService.removeTaskStartTime(applicationId, taskId, actorId);
                    }
                }
            }
        }
    }

    private void updateActorMetrics(String applicationId, String taskId, String actorId, long duration) {
        log.info("StatusChangeListener [updateActorMetrics] {}, {}, {}, {}", applicationId, taskId, actorId, duration);
        Query query = new Query(Criteria.where("applicationId").is(applicationId)
                                        .and("actorId").is(actorId));

        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        if (existingDocument == null) {
            Document newEntry = new Document()
                    .append("applicationId", applicationId)
                    .append("actorId", actorId)
                    .append("tasks", List.of(new Document()
                            .append("taskId", taskId)
                            .append("visited", 1)
                            .append("duration", duration)
                    ));
            mongoTemplate.getCollection("actor_metrics").insertOne(newEntry);
        } else {
            List<Document> tasks = (List<Document>) existingDocument.get("tasks");

            for (Document task : tasks) {
                if (taskId.equals(task.getString("taskId"))) {
                    Object existingDurationObj = task.get("duration");

                    long existingDuration = 0;
                    if (existingDurationObj instanceof Integer) {
                        existingDuration = ((Integer) existingDurationObj).longValue();
                    } else if (existingDurationObj instanceof Long) {
                        existingDuration = (Long) existingDurationObj;
                    }

                    Update update = new Update()
                            .inc("tasks.$.visited", 1)
                            .set("tasks.$.duration", existingDuration + duration);

                    Query taskQuery = new Query(Criteria.where("applicationId").is(applicationId)
                            .and("actorId").is(actorId)
                            .and("tasks.taskId").is(taskId));

                    mongoTemplate.updateFirst(taskQuery, update, "actor_metrics");
                    return;
                }
            }

            mongoTemplate.updateFirst(query, new Update().push("tasks", new Document()
                    .append("taskId", taskId)
                    .append("visited", 1)
                    .append("duration", duration)
            ), "actor_metrics");
        }
    }
}
