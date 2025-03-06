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
public class TaskExecutionTimeServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionTimeServiceImpl.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TaskExecutionServiceImpl taskExecutionService;

    private ExecutorService executorService;

    @PostConstruct
    public void startChangeStream() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                logger.info("Starting change stream listener for task_execution collection");
                mongoTemplate.getCollection("Task_exec")
                        .watch()
                        .forEach(this::processChangeStreamDocument);
            } catch (Exception e) {
                logger.error("Error in change stream processing", e);
            }
        });
    }

    private void processChangeStreamDocument(ChangeStreamDocument<Document> changeStreamDocument) {
        try {
            // Check operation type - we're interested in inserts and updates
            String operationType = changeStreamDocument.getOperationType().getValue();
            logger.info("Received change stream event: {}", operationType);

            Document fullDocument = changeStreamDocument.getFullDocument();
            if (fullDocument == null) {
                logger.warn("Received null document in change stream event");
                return;
            }

            // Log the full document for debugging
            logger.debug("Full document: {}", fullDocument.toJson());

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
            if (eventTime == null) {
                eventTime = Instant.now();
                logger.info("Using current time as event time");
            }

            // Call service to update task execution time
            logger.info("Calling updateTaskExecutionTime with taskId={}, status={}, funnel={}",
                    taskId, status, funnel);
            taskExecutionService.updateTaskExecutionTime(taskId, status, eventTime, funnel, applicationId, entityId);
        } catch (Exception e) {
            logger.error("Error processing change stream event", e);
        }
    }

    private String getString(Document document, String field) {
        Object value = document.get(field);
        if (value == null) {
            logger.debug("Field {} is null in document", field);
            return null;
        }
        return value.toString();
    }

    private Instant getInstant(Document document, String field) {
        Object value = document.get(field);
        if (value == null) {
            logger.debug("Field {} is null in document", field);
            return null;
        }

        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant();
        } else {
            logger.warn("Field {} is not a Date object: {}", field, value.getClass().getName());
            return null;
        }
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}