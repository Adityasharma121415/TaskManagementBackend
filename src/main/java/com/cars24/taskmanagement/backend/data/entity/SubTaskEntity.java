package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class SubTaskEntity {
    private String taskId;
    private Instant newTime;
    private Instant todoTime;

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Instant getNewTime() {
        return newTime;
    }

    public void setNewTime(Instant newTime) {
        this.newTime = newTime;
    }

    public Instant getTodoTime() {
        return todoTime;
    }

    public void setTodoTime(Instant todoTime) {
        this.todoTime = todoTime;
    }

    public Instant getSendbackTime() {
        return sendbackTime;
    }

    public void setSendbackTime(Instant sendbackTime) {
        this.sendbackTime = sendbackTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    private Instant completedTime;
    private Instant sendbackTime;
    private String status;
    private long duration;
    private int visited;
}