package com.cars24.taskmanagement.backend.service.impl;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
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
        MessageListener<ChangeStreamEvent<Document>, Document> listener = event -> {
            if (event.getBody() != null) {
                processEvent(event);
            }
        };

        ChangeStreamRequest<Document> request = ChangeStreamRequest.builder(listener)
                .collection("task_execution") // ✅ Corrected the collection name
                .filter("{ 'updateDescription.updatedFields.status': { '$exists': true } }") // ✅ Only listen for status updates
                .build();

        subscription = mongoTemplate.changeStream(request, Document.class);
    }

    private void processEvent(ChangeStreamEvent<Document> event) {
        Document fullDocument = event.getBody();
        if (fullDocument != null) {
            // ✅ Extract necessary fields correctly
            String taskId = fullDocument.getString("taskId");
            String status = fullDocument.getString("status");
            String funnel = fullDocument.getString("funnel");  // Expect values: "CREDIT", "CONVERSION", "FULFILMENT"
            String applicationId = fullDocument.getString("applicationId");
            String entityId = fullDocument.getString("entityId");
            Instant eventTime = (fullDocument.getDate("updatedAt") != null) ?
                    fullDocument.getDate("updatedAt").toInstant() : Instant.now();

            // ✅ Call the service to update task_execution_time
            timeService.updateTaskExecutionTime(taskId, status, eventTime, funnel, applicationId, entityId);
        }
    }
}
