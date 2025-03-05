package com.cars24.taskmanagement.backend.service.impl;

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
import java.util.List;

@Service
@Slf4j
public class ChangeStreamServiceImpl {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void watchTaskExecutionLog() {
        log.info("ChangeStreamServiceImpl [watchTaskExecutionLog] ");
        new Thread(() -> {
            mongoTemplate.getCollection("task_execution_log")
                    .watch(List.of())
                    .forEach(this::processChange);
        }).start();
    }

    private void processChange(ChangeStreamDocument<Document> change) {
        log.info("ChangeStreamServiceImpl [processChange] {}", change);
        if (change.getOperationType() == OperationType.INSERT) {
            Document fullDocument = change.getFullDocument();
            if (fullDocument != null) {
                String actorId = fullDocument.getString("actorId");
                String applicationId = fullDocument.getString("applicationId");
                String taskId = fullDocument.getString("taskId");

                updateActorMetrics(actorId, applicationId, taskId);
            }
        }
    }

    private void updateActorMetrics(String actorId, String applicationId, String taskId) {

        log.info("ChangeStreamServiceImpl [updateActorMetrics] {} {} {}", actorId, applicationId, taskId);
        Query query = new Query();
        query.addCriteria(Criteria.where("actorId").is(actorId)
                                  .and("applicationId").is(applicationId));

        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        if (existingDocument == null) {
            Document newEntry = new Document()
                    .append("applicationId", applicationId)
                    .append("actorId", actorId)
                    .append("tasks", List.of(new Document()
                            .append("taskId", taskId)
                            .append("visited", 1)
                            .append("duration", 0)
                    ));
            mongoTemplate.getCollection("actor_metrics").insertOne(newEntry);
        } else {
            List<Document> tasks = (List<Document>) existingDocument.get("tasks");
            boolean taskExists = tasks.stream().anyMatch(t -> t.getString("taskId").equals(taskId));

            if (!taskExists) {
                Update update = new Update().push("tasks", new Document()
                        .append("taskId", taskId)
                        .append("visited", 1) // Initial visit count
                        .append("duration", 0) // Default duration
                );
                mongoTemplate.updateFirst(query, update, "actor_metrics");
            }
        }
    }

}
