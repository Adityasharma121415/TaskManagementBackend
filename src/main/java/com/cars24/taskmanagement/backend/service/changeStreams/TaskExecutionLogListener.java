package com.cars24.taskmanagement.backend.service.changeStreams;

import com.cars24.taskmanagement.backend.service.redisCache.RedisCacheService;
import com.mongodb.client.model.Filters;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.cars24.taskmanagement.backend.constants.FileConstants;

@Service
@Slf4j
public class TaskExecutionLogListener {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private RedissonClient redissonClient;

    String RESUME_TOKEN_COLLECTION = FileConstants.RESUME_TOKEN_COLLECTION;
    String RESUME_TOKEN_KEY = FileConstants.RESUME_TOKEN_KEY;

    @PostConstruct
    public void watchTaskExecutionLog() {
        log.info("TaskExecutionLogListener [watchTaskExecutionLog] started...");

        new Thread(() -> {
            BsonDocument resumeToken = getStoredResumeToken();
            MongoCollection<Document> collection = mongoTemplate.getCollection("task_execution_log");

            MongoCursor<ChangeStreamDocument<Document>> cursor =
                    (resumeToken != null)
                            ? collection.watch(List.of()).resumeAfter(resumeToken).iterator()
                            : collection.watch(List.of()).iterator();

            while(cursor.hasNext()){
                ChangeStreamDocument<Document> change = cursor.next();
                processChange(change);
                storeResumeToken(change.getResumeToken());
            }
        }
        ).start();
    }

    private void processChange(ChangeStreamDocument<Document> change) {
        log.info("TaskExecutionLogListener [processChange] {}", change);

        Document fullDocument = change.getFullDocument();
        if (fullDocument == null) {
            log.warn("TaskExecutionLogListener [processChange] fullDocument is null");
            return;
        }

        String lockKey = "lock:task:" + fullDocument.getObjectId("_id").toHexString();
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(FileConstants.REDISSON_CLIENT_WAIT_TIME, FileConstants.REDISSON_CLIENT_LEASE_TIME, java.util.concurrent.TimeUnit.SECONDS);
            if (isLocked) {
                log.info("TaskExecutionLogListener [processChange] Acquired distributed lock on key: {}", lockKey);

                String actorId = fullDocument.getString("actorId");
                String applicationId = fullDocument.getString("applicationId");
                String taskId = fullDocument.getString("taskId");
                String status = fullDocument.getString("status");
                String actorType = fullDocument.getString("actorType");
                Instant updatedAt = fullDocument.getDate("updatedAt").toInstant();
                String handledBy = fullDocument.getString("handledBy");
                String executionType = fullDocument.getString("executionType");
                String funnel = fullDocument.getString("funnel");

                int initialTask = 0;
                if (change.getOperationType() == OperationType.INSERT) {
                    if(executionType.equals("MANUAL")){
                        initialTask = initializeActorMetrics(actorId, applicationId, taskId, status, updatedAt, actorType, handledBy);
                    }
                    else{
                        initialTask = initializeSystemMetrics(funnel, applicationId, taskId, status, updatedAt);
                    }
                }

                if ("NEW".equals(status) || "TODO".equals(status)) {
                    if(executionType.equals("MANUAL")){
                        redisCacheService.storeTaskStartTime(applicationId, taskId, actorId, updatedAt);
                        if(initialTask == 0){
                            updateActorMetrics(actorId, applicationId, taskId, status, 0L, updatedAt);
                        }
                    }
                    else if(executionType.equals("AUTOMATED")){
                        redisCacheService.storeSystemTaskStartTime(funnel, applicationId, taskId, updatedAt);
                        if(initialTask == 0){
                            updateSystemMetrics(funnel, applicationId, taskId, status, 0L, updatedAt);
                        }
                    }
                }
                else if ("COMPLETED".equals(status) || "FAILED".equals(status) || "SENDBACK".equals(status)) {
                    long duration = 0L;
                    Instant startUpdatedAt;
                    if(executionType.equals("MANUAL")){
                        startUpdatedAt = redisCacheService.getTaskStartTime(applicationId, taskId, actorId);
                        if(startUpdatedAt != null){
                            duration = updatedAt.toEpochMilli() - startUpdatedAt.toEpochMilli();
                        }
                        log.info("TaskExecutionLogListener [processChange] duration: {}",duration);
                        updateActorMetrics(actorId, applicationId, taskId, status, duration, updatedAt);
                        redisCacheService.removeTaskStartTime(applicationId, taskId, actorId);
                    }
                    else if(executionType.equals("AUTOMATED")){
                        startUpdatedAt = redisCacheService.getSystemTaskStartTime(funnel, applicationId, taskId);
                        if(startUpdatedAt != null){
                            duration = updatedAt.toEpochMilli() - startUpdatedAt.toEpochMilli();
                        }
                        log.info("TaskExecutionLogListener [processChange] duration: {}",duration);
                        updateSystemMetrics(funnel, applicationId, taskId, status, duration, updatedAt);
                        redisCacheService.removeSystemTaskStartTime(funnel, applicationId, taskId);
                    }
                }
            } else {
                log.info("TaskExecutionLogListener [processChange] Skipped processing for lockKey {} as lock is held by another instance.", lockKey);
            }
        } catch (InterruptedException e) {
            log.error("TaskExecutionLogListener [processChange] Error while trying to acquire lock for key: {}", lockKey, e);
            Thread.currentThread().interrupt();
        } finally {
            if (isLocked) {
                lock.unlock();
                log.info("TaskExecutionLogListener [processChange] Released distributed lock on key: {}", lockKey);
            }
        }
    }


    private int initializeSystemMetrics(String funnel, String applicationId, String taskId, String status, Instant updatedAt){
        log.info("TaskExecutionLogListener [initializeSystemMetrics] {} {} {} {} {}", funnel, applicationId, taskId, status, updatedAt);

        int initialTask = 0;
        Query query = new Query(Criteria.where("funnel").is(funnel).and("applicationId").is(applicationId));
        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        if(existingDocument == null){
            Document newEntry = new Document()
                    .append("funnel", funnel)
                    .append("applicationId", applicationId)
                    .append("tasks", List.of(new Document()
                            .append("taskId", taskId)
                            .append("status", status)
                            .append("visited", "NEW".equals(status) || "TODO".equals(status) ? 1 : 0)
                            .append("duration", 0)))
                    .append("totalDuration", 0)
                    .append("lastUpdatedAt", Date.from(updatedAt));
            mongoTemplate.getCollection("actor_metrics").insertOne(newEntry);
            initialTask = 1;
        }
        else{
            List<Document> tasks = (List<Document>) existingDocument.get("tasks");
            boolean taskExists = tasks.stream().anyMatch(task -> task.getString("taskId").equals(taskId));

            if (!taskExists) {
                Update update = new Update().push("tasks", new Document()
                        .append("taskId", taskId)
                        .append("status", status)
                        .append("visited", "NEW".equals(status) || "TODO".equals(status) ? 1 : 0)
                        .append("duration", 0));
                mongoTemplate.updateFirst(query, update, "actor_metrics");
                initialTask = 1;
            }
        }
        return initialTask;
    }

    private int initializeActorMetrics(String actorId, String applicationId, String taskId, String status, Instant updatedAt, String actorType, String handledBy) {

        log.info("TaskExecutionLogListener [initializeActorMetrics] {}, {}, {}, {}, {} {}", actorId, applicationId, taskId, status, updatedAt, handledBy);

        int initialTask = 0;
        Query query = new Query(Criteria.where("applicationId").is(applicationId).and("actorId").is(actorId));
        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        if (existingDocument == null) {
            Document newEntry = new Document()
                    .append("applicationId", applicationId)
                    .append("actorId", actorId)
                    .append("actorType", actorType)
                    .append("handledBy", handledBy)
                    .append("tasks", List.of(new Document()
                            .append("taskId", taskId)
                            .append("status", status)
                            .append("visited", "NEW".equals(status) || "TODO".equals(status) ? 1 : 0)
                            .append("duration", 0)))
                    .append("totalDuration", 0)
                    .append("lastUpdatedAt", Date.from(updatedAt));
            mongoTemplate.getCollection("actor_metrics").insertOne(newEntry);
            initialTask = 1;
        }
        else {
            List<Document> tasks = (List<Document>) existingDocument.get("tasks");
            boolean taskExists = tasks.stream().anyMatch(task -> task.getString("taskId").equals(taskId));

            if (!taskExists) {
                Update update = new Update().push("tasks", new Document()
                        .append("taskId", taskId)
                        .append("status", status)
                        .append("visited", "NEW".equals(status) || "TODO".equals(status) ? 1 : 0)
                        .append("duration", 0));
                mongoTemplate.updateFirst(query, update, "actor_metrics");
                initialTask = 1;
            }
        }
        return initialTask;
    }

    private void updateSystemMetrics(String funnel, String applicationId, String taskId, String status, long duration, Instant updatedAt){
        log.info("TaskExecutionLogListener [updateSystemMetrics] {} {} {} {} {} {}", funnel, applicationId, taskId, status, duration, updatedAt);

        Query query = new Query(Criteria.where("funnel").is(funnel).and("applicationId").is(applicationId));
        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        if (existingDocument == null){
            log.info("TaskExecutionLogListener [updateSystemMetrics] existingDocument cannot be null.");
            return;
        }

        Update update = new Update().set("lastUpdatedAt", Date.from(updatedAt));

        Query taskQuery = new Query(Criteria.where("funnel").is(funnel)
                .and("applicationId").is(applicationId)
                .and("tasks.taskId").is(taskId));

        Document existingTask = existingDocument.getList("tasks", Document.class).stream()
                .filter(task -> task.getString("taskId").equals(taskId))
                .findFirst()
                .orElse(null);

        if (existingTask != null) {
            update.set("tasks.$.status", status);

            if ("NEW".equals(status) || "TODO".equals(status)) {
                update.inc("tasks.$.visited", 1);
            }

            if (!"NEW".equals(status) && !"TODO".equals(status)) {
                update.inc("tasks.$.duration", duration);
                update.inc("totalDuration", duration);
            }

            mongoTemplate.updateFirst(taskQuery, update, "actor_metrics");
        }
        else {
            Document newTask = new Document()
                    .append("taskId", taskId)
                    .append("status", status)
                    .append("visited", "NEW".equals(status) || "TODO".equals(status) ? 1 : 0)
                    .append("duration", 0);

            update.push("tasks", newTask);
            mongoTemplate.updateFirst(query, update, "actor_metrics");
        }
    }

    private void updateActorMetrics(String actorId, String applicationId, String taskId, String status, long duration, Instant updatedAt) {
        log.info("TaskExecutionLogListener [updateActorMetrics] {} {} {} {} {} {}", actorId, applicationId, taskId, status, duration, updatedAt);

        Query query = new Query(Criteria.where("applicationId").is(applicationId).and("actorId").is(actorId));
        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        if (existingDocument == null){
            log.info("TaskExecutionLogListener [updateActorMetrics] existingDocument cannot be null.");
            return;
        }

        Update update = new Update().set("lastUpdatedAt", Date.from(updatedAt));

        Query taskQuery = new Query(Criteria.where("applicationId").is(applicationId)
                .and("actorId").is(actorId)
                .and("tasks.taskId").is(taskId));

        Document existingTask = existingDocument.getList("tasks", Document.class).stream()
                .filter(task -> task.getString("taskId").equals(taskId))
                .findFirst()
                .orElse(null);

        if (existingTask != null) {
            update.set("tasks.$.status", status);

            if ("NEW".equals(status) || "TODO".equals(status)) {
                update.inc("tasks.$.visited", 1);
            }

            if (!"NEW".equals(status) && !"TODO".equals(status)) {
                update.inc("tasks.$.duration", duration);
                update.inc("totalDuration", duration);
            }

            mongoTemplate.updateFirst(taskQuery, update, "actor_metrics");
        }
        else {
            Document newTask = new Document()
                    .append("taskId", taskId)
                    .append("status", status)
                    .append("visited", "NEW".equals(status) || "TODO".equals(status) ? 1 : 0)
                    .append("duration", 0);

            update.push("tasks", newTask);
            mongoTemplate.updateFirst(query, update, "actor_metrics");
        }
    }

    private void storeResumeToken(BsonDocument resumeToken) {
        if (resumeToken != null) {
            log.info("TaskExecutionLogListener [storeResumeToken] {}", resumeToken);

            Document tokenDocument = new Document("_id", RESUME_TOKEN_KEY)
                    .append("token", Document.parse(resumeToken.toJson()));

            mongoTemplate.getCollection(RESUME_TOKEN_COLLECTION)
                    .replaceOne(Filters.eq("_id", RESUME_TOKEN_KEY),
                            tokenDocument,
                            new ReplaceOptions().upsert(true));

        } else {
            log.warn("Attempted to store null resume token!");
        }
    }

    private BsonDocument getStoredResumeToken() {
        Document tokenDocument = mongoTemplate.getCollection(RESUME_TOKEN_COLLECTION)
                .find(Filters.eq("_id", RESUME_TOKEN_KEY)).first();

        if (tokenDocument != null && tokenDocument.containsKey("token")) {
            Object tokenObj = tokenDocument.get("token");

            if (tokenObj instanceof Document) {
                Document tokenDoc = (Document) tokenObj;
                BsonDocument storedToken = BsonDocument.parse(tokenDoc.toJson());
                log.info("Retrieved stored resume token: {}", storedToken);
                return storedToken;
            } else {
                log.warn("Unexpected resume token format: {}", tokenObj.getClass());
            }
        }

        log.info("No resume token found, starting fresh.");
        return null;
    }
}