package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "task_execution_time")
public class TaskExecutionTimeEntity {
    @Id
    private String id;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public List<SubTaskEntity> getSourcing() {
        return sourcing;
    }

    public void setSourcing(List<SubTaskEntity> sourcing) {
        this.sourcing = sourcing;
    }

    public List<SubTaskEntity> getCredit() {
        return credit;
    }

    public void setCredit(List<SubTaskEntity> credit) {
        this.credit = credit;
    }

    public List<SubTaskEntity> getConversion() {
        return conversion;
    }

    public void setConversion(List<SubTaskEntity> conversion) {
        this.conversion = conversion;
    }

    public List<SubTaskEntity> getFulfillment() {
        return fulfillment;
    }

    public void setFulfillment(List<SubTaskEntity> fulfillment) {
        this.fulfillment = fulfillment;
    }

    private String applicationId;
    private String entityId;
    private List<SubTaskEntity> sourcing = new ArrayList<>();
    private List<SubTaskEntity> credit = new ArrayList<>();
    private List<SubTaskEntity> conversion = new ArrayList<>();
    private List<SubTaskEntity> fulfillment = new ArrayList<>();
}