package com.cars24.taskmanagement.backend.data.entity;

import java.time.Instant;
import java.util.*;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "actor_metrics")
@Builder
public class ActorEntity {

    @Id
    private String id;
    private String actorId;
    private String actorType;
    private String applicationId;
    private List<TaskEntity> tasks;
    private Long totalDuration;
    private Instant lastUpdatedAt;
}