package com.cars24.taskmanagement.backend.data.response;

import lombok.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Data
@RequiredArgsConstructor
public class TaskDetails {
    private String funnel;

    private String actorId;
    private String status;

    private Date updatedAt;
    private String taskId;
    private int sendbacks;
    private int duration;
    private Map<String, Object> metadata;

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getFunnel() {
        return funnel;
    }

    public void setFunnel(String funnel) {
        this.funnel = funnel;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getSendbacks() {
        return sendbacks;
    }

    public void setSendbacks(int sendbacks) {
        this.sendbacks = sendbacks;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}