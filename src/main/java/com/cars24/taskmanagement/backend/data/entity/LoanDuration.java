package com.cars24.taskmanagement.backend.data.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;


@Document(collection = "duration")
public class LoanDuration {

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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public List<Task> getSourcing() {
        return sourcing;
    }

    public void setSourcing(List<Task> sourcing) {
        this.sourcing = sourcing;
    }

    public List<Task> getCredit() {
        return credit;
    }

    public void setCredit(List<Task> credit) {
        this.credit = credit;
    }

    public List<Task> getConversion() {
        return conversion;
    }

    public void setConversion(List<Task> conversion) {
        this.conversion = conversion;
    }

    public List<Task> getFulfillment() {
        return fulfillment;
    }

    public void setFulfillment(List<Task> fulfillment) {
        this.fulfillment = fulfillment;
    }

    private String applicationId;
    private String entityId;
    private String channel;
    private List<Task> sourcing;
    private List<Task> credit;
    private List<Task> conversion;
    private List<Task> fulfillment;


    public static class Task { //
        private String taskId;
        private String new_time;

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getNew_time() {
            return new_time;
        }

        public void setNew_time(String new_time) {
            this.new_time = new_time;
        }

        public int getSendbacks() {
            return sendbacks;
        }

        public void setSendbacks(int sendbacks) {
            this.sendbacks = sendbacks;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public int getVisited() {
            return visited;
        }

        public void setVisited(int visited) {
            this.visited = visited;
        }

        private String updatedAt;
        private int sendbacks;
        private long duration;
        private int visited;
    }
}