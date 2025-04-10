package com.cars24.taskmanagement.backend.service.changeStreams;

import com.cars24.taskmanagement.backend.constants.FileConstants;
import com.cars24.taskmanagement.backend.data.entity.TaskExecutionLogEntity;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionLogListenerBFF {

    private final RedissonClient redissonClient;

    private final MongoTemplate mongoTemplate;

    private final RabbitTemplate rabbitTemplate;

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

    public void processChange(ChangeStreamDocument<Document> change) {
        log.info("TaskExecutionLogListenerBFF [processChange] {}", change);

        Document fullDocument = change.getFullDocument();
        if (fullDocument == null) {
            log.warn("TaskExecutionLogListenerBFF [processChange] fullDocument is null");
            return;
        }

        String lockKey = "lock:task:" + fullDocument.getObjectId("_id").toHexString();
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(FileConstants.REDISSON_CLIENT_WAIT_TIME, FileConstants.REDISSON_CLIENT_LEASE_TIME, java.util.concurrent.TimeUnit.SECONDS);
            if (isLocked) {
                log.info("TaskExecutionLogListenerBFF [processChange] Acquired distributed lock on key: {}", lockKey);

                if (change.getOperationType() == OperationType.INSERT) {
                    // here i have to push it to queue (enqueue) the document
                    publishDocument(fullDocument);
                }
            } else {
                log.info("TaskExecutionLogListenerBFF [processChange] Skipped processing for lockKey {} as lock is held by another instance.", lockKey);
            }
        } catch (InterruptedException e) {
            log.error("TaskExecutionLogListenerBFF [processChange] Error while trying to acquire lock for key: {}", lockKey, e);
            Thread.currentThread().interrupt();
        } finally {
            if (isLocked) {
                lock.unlock();
                log.info("TaskExecutionLogListenerBFF [processChange] Released distributed lock on key: {}", lockKey);
            }
        }
    }

    public void publishDocument(Document document){
        log.info("TaskExecutionLogListenerBFF [publishDocument]: {}", document.toString());
        rabbitTemplate.convertAndSend(FileConstants.EXCHANGE, FileConstants.TASK_EXEC_LOG_ROUTING_KEY, document);
    }

    public void storeResumeToken(BsonDocument resumeToken) {
        if (resumeToken != null) {
            log.info("TaskExecutionLogListener [storeResumeToken] {}", resumeToken);

            Document tokenDocument = new Document("_id", FileConstants.RESUME_TOKEN_KEY)
                    .append("token", Document.parse(resumeToken.toJson()));

            mongoTemplate.getCollection(FileConstants.RESUME_TOKEN_COLLECTION)
                    .replaceOne(Filters.eq("_id", FileConstants.RESUME_TOKEN_KEY),
                            tokenDocument,
                            new ReplaceOptions().upsert(true));

        } else {
            log.warn("Attempted to store null resume token!");
        }
    }

    public BsonDocument getStoredResumeToken() {
        Document tokenDocument = mongoTemplate.getCollection(FileConstants.RESUME_TOKEN_COLLECTION)
                .find(Filters.eq("_id", FileConstants.RESUME_TOKEN_KEY)).first();

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
