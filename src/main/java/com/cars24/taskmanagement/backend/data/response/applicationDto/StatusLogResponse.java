package com.cars24.taskmanagement.backend.data.response.applicationDto;



import lombok.Data;

import java.util.Date;

@Data
public class StatusLogResponse {

    private String status;
    private Date updatedAt;

    public StatusLogResponse(String status, Date updatedAt) {
        this.status = status;
        this.updatedAt = updatedAt;
    }



}

