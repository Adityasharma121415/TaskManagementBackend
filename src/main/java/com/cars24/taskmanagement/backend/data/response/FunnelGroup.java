package com.cars24.taskmanagement.backend.data.response;


import java.util.List;

public class FunnelGroup {
        private String funnelName;
        private List<TaskDetails> tasks;

        // Default constructor
        public FunnelGroup() {
        }

        // Constructor with parameters
        public FunnelGroup(String funnelName, List<TaskDetails> tasks) {
            this.funnelName = funnelName;
            this.tasks = tasks;
        }

        // Getters and setters
        public String getFunnelName() {
            return funnelName;
        }

        public void setFunnelName(String funnelName) {
            this.funnelName = funnelName;
        }

        public List<TaskDetails> getTasks() {
            return tasks;
        }

        public void setTasks(List<TaskDetails> tasks) {
            this.tasks = tasks;
        }
    }
