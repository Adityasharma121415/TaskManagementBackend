package com.cars24.taskmanagement.backend.data.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

// Contains task details and status history
@Data
public class TaskInfo {
    private String taskId;
    private int order;
    private String handledBy;

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<StatusUpdate> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<StatusUpdate> statusHistory) {
        this.statusHistory = statusHistory;
    }

    private Date createdAt;
    private List<StatusUpdate> statusHistory;
}
