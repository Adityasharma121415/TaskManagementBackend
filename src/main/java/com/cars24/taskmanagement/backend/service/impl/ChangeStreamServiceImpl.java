package com.cars24.taskmanagement.backend.service.impl;

import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.BsonDocument;
import org.bson.BsonTimestamp;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class ChangeStreamServiceImpl {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String RESUME_TOKEN_COLLECTION = "resume_tokens";
    private static final String RESUME_TOKEN_KEY = "change_stream_resume_token";

    @PostConstruct
    public void watchTaskExecutionLog() {
        log.info("ChangeStreamServiceImpl [watchTaskExecutionLog] started");

        new Thread(() -> {
            MongoCollection<Document> collection = mongoTemplate.getCollection("task_execution_log");
            BsonDocument resumeToken = getStoredResumeToken();

            try (MongoCursor<ChangeStreamDocument<Document>> cursor =
                         (resumeToken == null)
                                 ? collection.watch(List.of()).fullDocument(FullDocument.UPDATE_LOOKUP).iterator()
                                 : collection.watch(List.of()).startAfter(resumeToken).fullDocument(FullDocument.UPDATE_LOOKUP).iterator()) {

                while (cursor.hasNext()) {
                    ChangeStreamDocument<Document> change = cursor.next();
                    processChange(change);
                    storeResumeToken(change.getResumeToken());
                }
            } catch (Exception e) {
                log.error("ChangeStreamServiceImpl [watchTaskExecutionLog] Error: ", e);
            }
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

        Document query = new Document("actorId", actorId).append("applicationId", applicationId);
        Document existingDocument = mongoTemplate.getCollection("actor_metrics").find(query).first();

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
            List<Document> tasks = existingDocument.getList("tasks", Document.class);

            log.info("ChangeStreamServiceImpl [updateActorMetrics] {}", tasks);

            boolean taskExists = false;
            int visited = 1;
            int duration = 0;

            for (Document task : tasks) {
                if (task.getString("taskId").equals(taskId)) {
                    visited = task.getInteger("visited", 0) + 1;
                    duration = task.getInteger("duration", 0);
                    taskExists = true;
                    break; // Exit loop once found
                }
            }

            if(taskExists) {
                mongoTemplate.getCollection("actor_metrics").updateOne(
                        new Document("actorId", actorId)
                                .append("applicationId", applicationId)
                                .append("tasks.taskId", taskId),
                        new Document("$inc", new Document("tasks.$.visited", 1))
                                .append("$set", new Document("tasks.$.duration", duration))
                );
            } else{

            mongoTemplate.getCollection("actor_metrics").updateOne(
                    query,
                    new Document("$push", new Document("tasks", new Document()
                            .append("taskId", taskId)
                            .append("visited", visited)
                            .append("duration", duration)))
            );}
        }

    }

    private void storeResumeToken(BsonDocument resumeToken) {
        if (resumeToken != null) {
            log.info("ChangeStreamServiceImpl [storeResumeToken] {}", resumeToken);

            Document tokenDocument = new Document("_id", "resume_token")
                    .append("token", Document.parse(resumeToken.toJson()));

            mongoTemplate.getCollection("resume_tokens")
                    .replaceOne(new Document("_id", "resume_token"), tokenDocument, new ReplaceOptions().upsert(true));


        } else {
            log.warn("Attempted to store null resume token!");
        }
    }

    private BsonDocument getStoredResumeToken() {
        Document tokenDocument = mongoTemplate.getCollection("resume_tokens")
                .find(new Document("_id", "resume_token")).first();

        if (tokenDocument != null) {
            Object tokenObj = tokenDocument.get("token");

            if (tokenObj instanceof Document) {
                BsonDocument storedToken = BsonDocument.parse(((Document) tokenObj).toJson());
                log.info("Retrieved stored resume token: {}", storedToken);
                return storedToken;
            } else if (tokenObj instanceof Binary) {
                BsonDocument storedToken = BsonDocument.parse(new String(((Binary) tokenObj).getData()));
                log.info("Retrieved stored resume token from Binary format: {}", storedToken);
                return storedToken;
            } else {
                log.warn("Unexpected resume token format: {}", tokenObj.getClass());
            }
        }

        log.info("No resume token found, starting fresh.");
        return null;
    }
}
