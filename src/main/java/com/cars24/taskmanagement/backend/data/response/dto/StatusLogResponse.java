package com.cars24.taskmanagement.backend.data.response.dto;



import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
public class StatusLogResponse {
    private String status;
    private Date updatedAt;
}

