package com.cars24.taskmanagement.backend.service.changeStreams;

import com.cars24.taskmanagement.backend.constants.FileConstants;
import com.cars24.taskmanagement.backend.service.impl.TaskExecutionServiceImpl;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExecutionListener {

    private final MongoTemplate mongoTemplate;

    private final TaskExecutionServiceImpl timeService;

    private final RedissonClient redissonClient;
    private ExecutorService executorService;

    @PostConstruct
    public void startChangeStream() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                mongoTemplate.getCollection("task_execution")
                        .watch()
                        .fullDocument(FullDocument.UPDATE_LOOKUP)
                        .forEach(this::processChangeStreamDocument);
            } catch (Exception e) {
                log.error("Error in change stream processing", e);
            }
        });
    }

    private void processChangeStreamDocument(ChangeStreamDocument<Document> changeStreamDocument) {

        Document fullDocument = changeStreamDocument.getFullDocument();
        if (fullDocument == null) {
            log.info("TaskExecutionListener [processChangeStreamDocument] Received null document in change stream event");
            return;
        }

        String lockKey = "lock:task:" + fullDocument.getObjectId("_id").toHexString();
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(FileConstants.REDISSON_CLIENT_WAIT_TIME, FileConstants.REDISSON_CLIENT_LEASE_TIME, java.util.concurrent.TimeUnit.SECONDS);

            if(isLocked){
                log.info("TaskExecutionListener [processChange] Acquired distributed lock on key: {}", lockKey);
                String taskId = getString(fullDocument, "taskId");
                String status = getString(fullDocument, "status");
                String funnel = getString(fullDocument, "funnel");
                String applicationId = getString(fullDocument, "applicationId");
                String entityId = getString(fullDocument, "entityId");
                String channel = getString(fullDocument, "channel");

                Instant createdAt = getInstant(fullDocument, "createdAt");
                Instant updatedAt = getInstant(fullDocument, "updatedAt");


                Instant eventTime = status.equalsIgnoreCase("NEW") ? createdAt : updatedAt;


                timeService.updateTaskExecutionTime(taskId, status, createdAt, updatedAt, funnel, applicationId, entityId, channel);
            }
            else{
                log.info("TaskExecutionListener [processChangeStreamDocument] Unable to acquire lock for task: {}", fullDocument.getObjectId("_id").toHexString());
            }
        } catch (Exception e) {
            log.error("Error processing change stream event", e);
            Thread.currentThread().interrupt();
        }finally {
            if (isLocked) {
                lock.unlock();
                log.info("TaskExecutionListener [processChangeStreamDocument] Released distributed lock on key: {}", lockKey);
            }
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