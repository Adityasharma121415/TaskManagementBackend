package com.cars24.taskmanagement.backend.service.changeStreams;

import com.cars24.taskmanagement.backend.constants.FileConstants;
import com.cars24.taskmanagement.backend.service.redisCache.RedisCacheService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.Document;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskExecutionLogListenerTMM {

    private final MongoTemplate mongoTemplate;

    private final RedisCacheService redisCacheService;

    @RabbitListener(queues = FileConstants.TASK_EXEC_LOG_QUEUE)
    public void subscribeDocument(Document fullDocument){
        log.info("TaskExecutionLogListenerTMM [subscribeDocument] {}", fullDocument);

        if (fullDocument == null) {
            log.warn("TaskExecutionLogListenerTMM [subsribeDocument] document is null");
            return;
        }

        try{
            processDocument(fullDocument);
        }
        catch (Exception e){
            log.error("Exception ", e);
        }

    }

    public void processDocument(Document fullDocument){

        log.info("TaskExecutionLogListenerTMM [processDocument] {}", fullDocument);

        String actorId = fullDocument.getString("actorId");
        String applicationId = fullDocument.getString("applicationId");
        String taskId = fullDocument.getString("taskId");
        String status = fullDocument.getString("status");
        String actorType = fullDocument.getString("actorType");
        Instant updatedAt = Instant.ofEpochMilli(fullDocument.getLong("updatedAt"));
        String handledBy = fullDocument.getString("handledBy");
        String executionType = fullDocument.getString("executionType");
        String funnel = fullDocument.getString("funnel");

        int initialTask = 0;
        if(executionType.equals("MANUAL")){
            initialTask = initializeActorMetrics(actorId, applicationId, taskId, status, updatedAt, actorType, handledBy);
        }
        else{
            initialTask = initializeSystemMetrics(funnel, applicationId, taskId, status, updatedAt);
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
                log.info("TaskExecutionLogListenerTMM [processChange] duration: {}",duration);
                updateActorMetrics(actorId, applicationId, taskId, status, duration, updatedAt);
                redisCacheService.removeTaskStartTime(applicationId, taskId, actorId);
            }
            else if(executionType.equals("AUTOMATED")){
                startUpdatedAt = redisCacheService.getSystemTaskStartTime(funnel, applicationId, taskId);
                if(startUpdatedAt != null){
                    duration = updatedAt.toEpochMilli() - startUpdatedAt.toEpochMilli();
                }
                log.info("TaskExecutionLogListenerTMM [processChange] duration: {}",duration);
                updateSystemMetrics(funnel, applicationId, taskId, status, duration, updatedAt);
                redisCacheService.removeSystemTaskStartTime(funnel, applicationId, taskId);
            }
        }
        else{
            // this is to just update the status like if the status is SKIPPED, IN_PROGRESS
            if(executionType.equals("MANUAL")){
                updateActorMetrics(actorId, applicationId, taskId, status, 0L, updatedAt);
            }
            else{
                updateSystemMetrics(funnel, applicationId, taskId, status, 0L, updatedAt);
            }
        }
    }

    private int initializeSystemMetrics(String funnel, String applicationId, String taskId, String status, Instant updatedAt){
        log.info("TaskExecutionLogListenerTMM [initializeSystemMetrics] {} {} {} {} {}", funnel, applicationId, taskId, status, updatedAt);

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
                        .append("visited", "NEW".equals(status) ? 1 : 0)
                        .append("duration", 0));
                mongoTemplate.updateFirst(query, update, "actor_metrics");
                initialTask = 1;
            }
        }
        return initialTask;
    }

    private int initializeActorMetrics(String actorId, String applicationId, String taskId, String status, Instant updatedAt, String actorType, String handledBy) {

        log.info("TaskExecutionLogListenerTMM [initializeActorMetrics] {}, {}, {}, {}, {} {}", actorId, applicationId, taskId, status, updatedAt, handledBy);

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
                        .append("visited", "NEW".equals(status) ? 1 : 0)
                        .append("duration", 0));
                mongoTemplate.updateFirst(query, update, "actor_metrics");
                initialTask = 1;
            }
        }
        return initialTask;
    }

    private void updateSystemMetrics(String funnel, String applicationId, String taskId, String status, long duration, Instant updatedAt){
        log.info("TaskExecutionLogListenerTMM [updateSystemMetrics] {} {} {} {} {} {}", funnel, applicationId, taskId, status, duration, updatedAt);

        Query query = new Query(Criteria.where("funnel").is(funnel).and("applicationId").is(applicationId));
        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        if (existingDocument == null){
            log.info("TaskExecutionLogListenerTMM [updateSystemMetrics] existingDocument cannot be null.");
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
//            update.set("tasks.$.status", status);
//
//            if ("NEW".equals(status) || "TODO".equals(status)) {
//                update.inc("tasks.$.visited", 1);
//            }
//
//            if (!"NEW".equals(status) && !"TODO".equals(status)) {
//                update.inc("tasks.$.duration", duration);
//                update.inc("totalDuration", duration);
//            }
//
//            mongoTemplate.updateFirst(taskQuery, update, "actor_metrics");

            // -------------------

            String existingStatus = existingTask.getString("status");

            update.set("tasks.$.status", status);
            if (!"NEW".equals(status) && !"TODO".equals(status)) {
                update.inc("tasks.$.duration", duration);
                update.inc("totalDuration", duration);
            }

            if(existingStatus.equals("NEW") && (status.equals("TODO") || status.equals("IN_PROGRESS"))){
                mongoTemplate.updateFirst(taskQuery, update, "actor_metrics");
                return;
            }

            if ("NEW".equals(status) || "TODO".equals(status)) {
                update.inc("tasks.$.visited", 1);
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
        log.info("TaskExecutionLogListenerTMM [updateActorMetrics] {} {} {} {} {} {}", actorId, applicationId, taskId, status, duration, updatedAt);

        Query query = new Query(Criteria.where("applicationId").is(applicationId).and("actorId").is(actorId));
        Document existingDocument = mongoTemplate.findOne(query, Document.class, "actor_metrics");

        if (existingDocument == null){
            log.info("TaskExecutionLogListenerTMM [updateActorMetrics] existingDocument cannot be null.");
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

            String existingStatus = existingTask.getString("status");

            update.set("tasks.$.status", status);
            if (!"NEW".equals(status) && !"TODO".equals(status)) {
                update.inc("tasks.$.duration", duration);
                update.inc("totalDuration", duration);
            }

            if(existingStatus.equals("NEW") && (status.equals("TODO") || status.equals("IN_PROGRESS"))){
                mongoTemplate.updateFirst(taskQuery, update, "actor_metrics");
                return;
            }

            if ("NEW".equals(status) || "TODO".equals(status)) {
                update.inc("tasks.$.visited", 1);
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
}
