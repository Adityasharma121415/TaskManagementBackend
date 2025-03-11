package com.cars24.taskmanagement.backend.service.changeStreams;

import com.cars24.taskmanagement.backend.service.impl.TaskExecutionServiceImpl;
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
public class TaskExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionListener.class);

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


            String taskId = getString(fullDocument, "taskId");
            String status = getString(fullDocument, "status");
            String funnel = getString(fullDocument, "funnel");
            String applicationId = getString(fullDocument, "applicationId");
            String entityId = getString(fullDocument, "entityId");
            String channel = getString(fullDocument, "channel"); // Extracting channel


            if (taskId == null || status == null || funnel == null || applicationId == null || entityId == null || channel == null) {
                logger.warn("Incomplete task execution data: taskId={}, status={}, funnel={}, applicationId={}, entityId={}, channel={}",
                        taskId, status, funnel, applicationId, entityId, channel);
                return;
            }


            Instant createdAt = getInstant(fullDocument, "createdAt");
            Instant updatedAt = getInstant(fullDocument, "updatedAt");

            Instant eventTime = status.equalsIgnoreCase("NEW") ? createdAt : updatedAt;


            timeService.updateTaskExecutionTime(taskId, status, createdAt, updatedAt, funnel, applicationId, entityId, channel);
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
