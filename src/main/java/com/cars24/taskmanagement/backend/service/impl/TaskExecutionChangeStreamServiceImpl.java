package com.cars24.taskmanagement.backend.service.impl;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.data.mongodb.core.messaging.Subscription;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

@Component
public class TaskExecutionChangeStreamServiceImpl {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TaskExecutionServiceImpl timeService;

    private Subscription subscription;

    @PostConstruct
    public void startChangeStream() {
        // ✅ Corrected MessageListener Generic Type
        MessageListener<ChangeStreamEvent<Document>> listener = event -> {
            if (event.getBody() != null) {
                processEvent(event);
            }
        };

        // ✅ Use MongoChangeStreamRequest.builder()
        MongoChangeStreamRequest<Document> request = MongoChangeStreamRequest.builder(listener, Document.class)
                .collection("task_execution")
                .filter(Aggregation.newAggregation(  // ✅ Correct filter syntax
                        Aggregation.match(
                                Document.parse("{ 'updateDescription.updatedFields.status': { '$exists': true } }")
                        )
                ))
                .build();

        subscription = mongoTemplate.watch(request, Document.class);
    }

    private void processEvent(ChangeStreamEvent<Document> event) {
        Document fullDocument = event.getBody();
        if (fullDocument != null) {
            String taskId = fullDocument.getString("taskId");
            String status = fullDocument.getString("status");
            String funnel = fullDocument.getString("funnel");
            String applicationId = fullDocument.getString("applicationId");
            String entityId = fullDocument.getString("entityId");
            Instant eventTime = fullDocument.getDate("updatedAt") != null
                    ? fullDocument.getDate("updatedAt").toInstant()
                    : Instant.now();

            // ✅ Ensure the method exists in TaskExecutionServiceImpl before calling it
            timeService.updateTaskExecutionTime(taskId, status, eventTime, funnel, applicationId, entityId);
        }
    }
}
