package com.cars24.taskmanagement.backend.service.changeStreams;

import com.cars24.taskmanagement.backend.service.impl.TaskExecutionServiceImpl;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class TaskExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionListener.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TaskExecutionServiceImpl timeService;

    private ExecutorService executorService;

    @PostConstruct
    public void startChangeStream() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                mongoTemplate.getCollection("task_execution")
                        .watch()
                        .fullDocument(FullDocument.UPDATE_LOOKUP)  // Ensure full document is returned on updates
                        .forEach(this::processChangeStreamDocument);
            } catch (Exception e) {
                logger.error("Error in change stream processing", e);
            }
        });
    }

    private void processChangeStreamDocument(ChangeStreamDocument<Document> changeStreamDocument) {
        String taskId=null;
        String status=null;

        try {
            Document fullDocument = changeStreamDocument.getFullDocument();
            if (fullDocument == null) {
                logger.warn("Received null document in change stream event");
                return;
            }


            taskId = getString(fullDocument, "taskId");
            status = getString(fullDocument, "status");
            String funnel = getString(fullDocument, "funnel");
            String applicationId = getString(fullDocument, "applicationId");
            String entityId = getString(fullDocument, "entityId");
            String channel = getString(fullDocument, "channel");

            // Validate required fields
            if (taskId == null || status == null || funnel == null || applicationId == null || entityId == null || channel == null) {
                logger.warn("Incomplete task execution data: taskId={}, status={}, funnel={}, applicationId={}, entityId={}, channel={}",
                        taskId, status, funnel, applicationId, entityId, channel);
                return;
            }

            if (!acquireLock(taskId, status)) {
                logger.info("Skipping duplicate processing for taskId={}, status={}", taskId, status);
                return;
            }



            Instant createdAt = getInstant(fullDocument, "createdAt");
            Instant updatedAt = getInstant(fullDocument, "updatedAt");

            // Use createdAt for NEW status, updatedAt for others
            Instant eventTime = status.equalsIgnoreCase("NEW") ? createdAt : updatedAt;

            // Call service to update task execution time
            timeService.updateTaskExecutionTime(taskId, status, createdAt, updatedAt, funnel, applicationId, entityId, channel);
        } catch (Exception e) {
            logger.error("Error processing change stream event", e);
        }finally {
            if (taskId != null && status != null) {
                releaseLock(taskId, status);
            }
        }
    }

    private boolean acquireLock(String taskId, String status) {
        String key = "lock:task:" + taskId + ":status:" + status;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(String taskId, String status) {
        String key = "lock:task:" + taskId + ":status:" + status;
        redisTemplate.delete(key);
    }

    private String getString(Document document, String field) {
        Object value = document.get(field);
        return value instanceof String ? (String) value : null;
    }

    private Instant getInstant(Document document, String field) {
        Object value = document.get(field);
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant();
        }
        return Instant.now();
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
