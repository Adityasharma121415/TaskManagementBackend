package com.cars24.taskmanagement.backend.data.response;

import lombok.Data;

import java.util.List;

// Represents a group of tasks in a funnel
@Data
public class FunnelGroup {
    private String funnelName;
    private int order; // For sorting funnels

    public List<TaskInfo> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskInfo> tasks) {
        this.tasks = tasks;
    }

    public String getFunnelName() {
        return funnelName;
    }

    public void setFunnelName(String funnelName) {
        this.funnelName = funnelName;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    private List<TaskInfo> tasks;
}
