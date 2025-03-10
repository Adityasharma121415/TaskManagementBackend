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
import java.util.Date;
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
                        log.info("StatusChangeListener [processChange] Duration: {}", duration);
                        updateActorMetrics(applicationId, taskId, actorId, duration, updatedAt);
                        redisCacheService.removeTaskStartTime(applicationId, taskId, actorId);
                    }
                }
            }
        }
    }

    private void updateActorMetrics(String applicationId, String taskId, String actorId, long duration, Instant updatedAt) {
        log.info("StatusChangeListener [updateActorMetrics] {}, {}, {}, {}, {}", applicationId, taskId, actorId, duration, updatedAt);

        Query query = new Query(Criteria.where("applicationId").is(applicationId).and("actorId").is(actorId));
        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        Instant latestUpdatedAt = updatedAt;

        if (existingDocument != null && existingDocument.containsKey("lastUpdatedAt")) {
            Object lastUpdatedAtObj = existingDocument.get("lastUpdatedAt");

            if (lastUpdatedAtObj instanceof Date) {
                Instant existingUpdatedAt = ((Date) lastUpdatedAtObj).toInstant();
                if (existingUpdatedAt.isAfter(updatedAt)) {
                    latestUpdatedAt = existingUpdatedAt;
                }
            } else {
                log.warn("Unexpected format for lastUpdatedAt: {}", lastUpdatedAtObj);
            }
        }

        if (existingDocument == null) {
            Document newEntry = new Document()
                    .append("applicationId", applicationId)
                    .append("actorId", actorId)
                    .append("tasks", List.of(new Document()
                            .append("taskId", taskId)
                            .append("visited", 1)
                            .append("duration", duration)))
                    .append("totalDuration", duration)
                    .append("lastUpdatedAt", Date.from(latestUpdatedAt));
            mongoTemplate.getCollection("actor_metrics").insertOne(newEntry);
            return;
        }

        List<Document> tasks = (List<Document>) existingDocument.get("tasks");
        boolean taskExists = false;

        for (Document task : tasks) {
            if (taskId.equals(task.getString("taskId"))) {
                taskExists = true;
                break;
            }
        }

        Update update = new Update()
                .inc("totalDuration", duration)
                .set("lastUpdatedAt", Date.from(latestUpdatedAt));

        if (taskExists) {
            Query taskQuery = new Query(Criteria.where("applicationId").is(applicationId)
                    .and("actorId").is(actorId)
                    .and("tasks.taskId").is(taskId));

            update.inc("tasks.$.visited", 1)
                  .inc("tasks.$.duration", duration);

            mongoTemplate.updateFirst(taskQuery, update, "actor_metrics");
        } else {
            update.push("tasks", new Document()
                    .append("taskId", taskId)
                    .append("visited", 1)
                    .append("duration", duration));
            mongoTemplate.updateFirst(query, update, "actor_metrics");
        }
    }
}
