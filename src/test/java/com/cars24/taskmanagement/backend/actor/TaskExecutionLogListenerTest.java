package com.cars24.taskmanagement.backend.actor;

import com.cars24.taskmanagement.backend.service.changeStreams.TaskExecutionLogListener;
import com.cars24.taskmanagement.backend.service.redisCache.RedisCacheService;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TaskExecutionLogListenerTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private RedissonClient redissonClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MongoCollection<Document> mongoCollection;

    @Mock
    private FindIterable<Document> findIterable;

    @Mock
    private MongoCursor<Document> mongoCursor;

    @InjectMocks
    private TaskExecutionLogListener taskExecutionLogListener;

    @Mock
    private RLock rLock;

    private final String applicationId = "UCE1000003486";
    private final String taskId = "ogl_check";
    private final String actorId = "132";

    @BeforeEach
    void setUp() {
        when(redissonClient.getLock(any())).thenReturn(rLock);
        when(mongoTemplate.getCollection(anyString())).thenReturn(mongoCollection);
        when(mongoCollection.find(any(BsonDocument.class))).thenReturn(findIterable);
    }

    @Test
    void testProcessChangeCompletedManualTask() throws InterruptedException {
        Document fullDocument = new Document()
                .append("_id", new org.bson.types.ObjectId())
                .append("actorId", actorId)
                .append("applicationId", applicationId)
                .append("taskId", taskId)
                .append("status", "COMPLETED")
                .append("executionType", "MANUAL")
                .append("updatedAt", Date.from(Instant.now()));

        ChangeStreamDocument<Document> change = mock(ChangeStreamDocument.class);
        when(change.getFullDocument()).thenReturn(fullDocument);
        when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(redisCacheService.getTaskStartTime(eq(applicationId), eq(taskId), eq(actorId)))
                .thenReturn(Instant.now().minusSeconds(300));

        taskExecutionLogListener.processChange(change);

        verify(redisCacheService, times(1)).removeTaskStartTime(eq(applicationId), eq(taskId), eq(actorId));
        verify(rLock, times(1)).unlock();
    }

    @Test
    void testStoreResumeToken() {
        BsonDocument mockToken = BsonDocument.parse("{ resumeToken: '8267CE945' }");

        taskExecutionLogListener.storeResumeToken(mockToken);

        verify(mongoTemplate, times(1)).getCollection(anyString());
        verify(mongoCollection, times(1))
                .replaceOne(any(), any(Document.class), any(ReplaceOptions.class));
    }
}