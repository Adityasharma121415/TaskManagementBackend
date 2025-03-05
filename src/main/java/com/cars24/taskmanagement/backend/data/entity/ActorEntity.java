package com.cars24.taskmanagement.backend.data.entity;

import java.util.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "actor_metrics")
public class ActorEntity {

    @Id
    private String id;
    private String actorId;
    private String applicationId;
    private List<TaskEntity> tasks;
}