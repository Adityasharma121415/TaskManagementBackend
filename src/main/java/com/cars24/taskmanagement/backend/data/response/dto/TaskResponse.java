package com.cars24.taskmanagement.backend.data.response.dto;



import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class TaskResponse {
    private String taskId;
    private int order;
    private String handledBy;
    private Date createdAt;
    private List<StatusLogResponse> statusHistory;

    // New fields
    private long duration;
    private int sendbacks;
    private int visited;
    //private String updatedAt;

    // Default constructor
    public TaskResponse() {
    }

    // Constructor with original parameters
    public TaskResponse(String taskId, int order, String handledBy, Date createdAt, List<StatusLogResponse> statusHistory) {
        this.taskId = taskId;
        this.order = order;
        this.handledBy = handledBy;
        this.createdAt = createdAt;
        this.statusHistory = statusHistory;
    }

    // Getters and setters for original fields
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<StatusLogResponse> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<StatusLogResponse> statusHistory) {
        this.statusHistory = statusHistory;
    }


    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getSendbacks() {
        return sendbacks;
    }

    public void setSendbacks(int sendbacks) {
        this.sendbacks = sendbacks;
    }

    public int getVisited() {
        return visited;
    }

    public void setVisited(int visited) {
        this.visited = visited;
    }


}
