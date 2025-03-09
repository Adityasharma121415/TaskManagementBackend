package com.cars24.taskmanagement.backend.data.response;

import lombok.Data;

import java.util.Date;

// Represents a status change event
@Data
public class StatusUpdate {
    private String status;

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private Date updatedAt;
}
