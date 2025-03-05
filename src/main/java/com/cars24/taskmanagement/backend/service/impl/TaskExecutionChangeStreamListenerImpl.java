package com.cars24.taskmanagement.backend.service.impl;


import javax.annotation.PostConstruct;
import java.time.Instant;

@Component
public class TaskExecutionChangeStreamListenerImpl {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TaskExecutionTimeService timeService;

    @PostConstruct
    public void startChangeStream() {
        ChangeStreamRequest<Document> request = ChangeStreamRequest.builder()
                .collection("task_execution")
                .publishTo(event -> processEvent(event))
                .build();

        Subscription subscription = mongoTemplate.changeStream(request, Document.class);
    }

    private void processEvent(ChangeStreamEvent<Document> event) {
        Document fullDocument = event.getBody();
        if (fullDocument != null) {
            // Extract necessary fields.
            String taskId = fullDocument.getString("taskId");
            String status = fullDocument.getString("status");
            String funnel = fullDocument.getString("funnel");  // Expect values: "CREDIT", "CONVERSION", "FULFILMENT"
            String applicationId = fullDocument.getString("applicationId");
            String entityId = fullDocument.getString("entityId");
            Instant eventTime = (fullDocument.getDate("updatedAt") != null) ?
                    fullDocument.getDate("updatedAt").toInstant() : Instant.now();

            // Call the service to update task_execution_time accordingly.
            timeService.updateTaskExecutionTime(taskId, status, eventTime, funnel, applicationId, entityId);
        }
    }
}
