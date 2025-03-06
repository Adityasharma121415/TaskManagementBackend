package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;

@Data
public class TaskEntity {

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getVisited() {
        return visited;
    }

    public void setVisited(int visited) {
        this.visited = visited;
    }

    private String taskId;
    private double duration;
    private int visited;
}
