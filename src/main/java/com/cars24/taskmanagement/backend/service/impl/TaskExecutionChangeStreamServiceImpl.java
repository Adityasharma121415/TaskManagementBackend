package com.cars24.taskmanagement.backend.service.impl;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TaskExecutionChangeStreamServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionChangeStreamServiceImpl.class);

    @Autowired
    private MongoTemplate mongoTemplate;

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
                        .forEach(this::processChangeStreamDocument);
            } catch (Exception e) {
                logger.error("Error in change stream processing", e);
            }
        });
    }

    private void processChangeStreamDocument(ChangeStreamDocument<Document> changeStreamDocument) {
        try {
            Document fullDocument = changeStreamDocument.getFullDocument();
            if (fullDocument == null) {
                logger.warn("Received null document in change stream event");
                return;
            }

            // Safe extraction of fields
            String taskId = getString(fullDocument, "taskId");
            String status = getString(fullDocument, "status");
            String funnel = getString(fullDocument, "funnel");
            String applicationId = getString(fullDocument, "applicationId");
            String entityId = getString(fullDocument, "entityId");

            // Validate required fields
            if (taskId == null || status == null || funnel == null || applicationId == null || entityId == null) {
                logger.warn("Incomplete task execution data: taskId={}, status={}, funnel={}, applicationId={}, entityId={}",
                        taskId, status, funnel, applicationId, entityId);
                return;
            }

            Instant eventTime = getInstant(fullDocument, "updatedAt");

            // Call service to update task execution time
            timeService.updateTaskExecutionTime(taskId, status, eventTime, funnel, applicationId, entityId);
        } catch (Exception e) {
            logger.error("Error processing change stream event", e);
        }
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