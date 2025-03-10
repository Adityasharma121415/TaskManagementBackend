package com.cars24.taskmanagement.backend.data.exception;


public class SlaException extends RuntimeException {

    public SlaException(String message) {
        super(message);
    }

    public SlaException(String message, Throwable cause) {
        super(message, cause);
    }
}
