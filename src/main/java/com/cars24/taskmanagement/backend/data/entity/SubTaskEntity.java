package com.cars24.taskmanagement.backend.data.entity;

import lombok.Data;
import java.time.Instant;

import static java.lang.Math.abs;

@Data
public class SubTaskEntity {
    private String taskId;
    private Instant new_time;
    private Instant todo_time;
    private Instant completed_time;
    private Instant sendback_time;
    private Instant inprogress_time;
    private Instant updatedAt;
    private Instant createdAt;
    private int sendbacks;
    private long duration;
    private int visited;
    private int revisit;
    private String statusoftask;


    public SubTaskEntity(String taskId, Instant createdAt) {
        this.taskId = taskId;
//        this.new_time = updatedAt;
        this.new_time = createdAt;
        this.visited = 0;
        this.sendbacks = 0;
        this.revisit = 0;
    }

    public void updateStatus(String status, Instant updatedAt, boolean flag) {
        switch (status.toUpperCase()) {
            case "TODO":
                this.todo_time = updatedAt;
                this.visited++;
                break;

            case "IN_PROGRESS":
                this.inprogress_time = updatedAt;
                this.visited++;
                if(this.statusoftask.equalsIgnoreCase("TODO")) {
                    this.duration += abs(updatedAt.toEpochMilli() - this.todo_time.toEpochMilli());
                }
                break;

            case "COMPLETED":
                if (this.todo_time != null && this.visited > 1) {
                    this.completed_time = updatedAt;
                    if(this.statusoftask.equalsIgnoreCase("TODO")) {
                        this.duration += abs(updatedAt.toEpochMilli() - this.todo_time.toEpochMilli());
                        this.revisit++;
                    }
                }
                if (this.inprogress_time != null && this.visited > 1) {
                    this.completed_time = updatedAt;
                    if(this.statusoftask.equalsIgnoreCase("IN_PROGRESS")) {
                        this.duration += abs(updatedAt.toEpochMilli() - this.inprogress_time.toEpochMilli());
                        this.revisit++;
                    }
                }
                else if (this.new_time != null && (this.visited == 1 || this.todo_time == null) && (this.visited == 1 ||inprogress_time == null)) {
                    this.completed_time = updatedAt;
                    if(this.statusoftask.equalsIgnoreCase("NEW")) {
                        this.duration += abs(updatedAt.toEpochMilli() - this.new_time.toEpochMilli());
//                    this.sendbacks++;

                    }
                }


                break;

            case "SENDBACK":
                this.sendback_time = updatedAt;

                if (this.todo_time != null ) {
                    this.duration += abs(updatedAt.toEpochMilli() - this.todo_time.toEpochMilli()) ;
                }
                if (this.inprogress_time != null ) {
                    this.duration += abs(updatedAt.toEpochMilli() - this.inprogress_time.toEpochMilli()) ;
                }
                else if (this.new_time != null && this.visited == 1) {
                    this.duration += abs(updatedAt.toEpochMilli() - this.new_time.toEpochMilli()) ;
                }
                break;

            case "FAILED":
                this.visited = Math.max(0, this.visited - 1);
                if(this.statusoftask.equalsIgnoreCase("TODO") ) {
                    this.duration += abs(updatedAt.toEpochMilli() - this.todo_time.toEpochMilli());
                    this.revisit++;
                }
                if(this.statusoftask.equalsIgnoreCase("IN_PROGRESS") ) {
                    this.duration += abs(updatedAt.toEpochMilli() - this.inprogress_time.toEpochMilli());
                    this.revisit++;
                }
                break;

            case "NEW":
                this.new_time = updatedAt;
                break;

            case "SKIPPED":
                this.duration += abs(updatedAt.toEpochMilli() - this.new_time.toEpochMilli()) ;
                break;

        }
        if(flag == true) {
            sendbacks++;
        }
        this.statusoftask = status;
        this.updatedAt = updatedAt;
    }
}