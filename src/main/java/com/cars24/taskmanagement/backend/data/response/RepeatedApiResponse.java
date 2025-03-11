package com.cars24.taskmanagement.backend.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class RepeatedApiResponse {
        private int statusCode;
        private boolean success;
        private String message;
        private String service;
        private Object data;

    }

