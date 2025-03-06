package com.cars24.taskmanagement.backend.service.changeStreams;

import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoCollection;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.BsonDocument;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class TaskExecutionLogListener {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void watchTaskExecutionLog() {
        log.info("TaskExecutionLogListener [watchTaskExecutionLog] started ...");

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
                log.error("TaskExecutionLogListener [watchTaskExecutionLog] Error: ", e);
            }
        }).start();
    }

    private void processChange(ChangeStreamDocument<Document> change) {
        log.info("TaskExecutionLogListener [processChange] {}", change);
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
        log.info("TaskExecutionLogListener [updateActorMetrics] {} {} {}", actorId, applicationId, taskId);

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

            log.info("TaskExecutionLogListener [updateActorMetrics] {}", tasks);

            boolean taskExists = false;
            int visited = 1;
            long duration = 0;

            for (Document task : tasks) {
                if (task.getString("taskId").equals(taskId)) {
                    visited = task.getInteger("visited", 0) + 1;

                    Object durationObj = task.get("duration");
                    if (durationObj instanceof Integer) {
                        duration = ((Integer) durationObj).longValue();
                    } else if (durationObj instanceof Long) {
                        duration = (Long) durationObj;
                    }

                    taskExists = true;
                    break;
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
            } else {
                mongoTemplate.getCollection("actor_metrics").updateOne(
                        query,
                        new Document("$push", new Document("tasks", new Document()
                                .append("taskId", taskId)
                                .append("visited", visited)
                                .append("duration", duration)))
                );
            }
        }
    }

    private void storeResumeToken(BsonDocument resumeToken) {
        if (resumeToken != null) {
            log.info("TaskExecutionLogListener [storeResumeToken] {}", resumeToken);

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
