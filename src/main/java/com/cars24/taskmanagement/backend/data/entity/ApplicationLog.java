package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "log")
public class ApplicationLog {
    @Id
    private String id;
    private String applicationId;
    private String message;
    private String level;
    private LocalDateTime timestamp;
}