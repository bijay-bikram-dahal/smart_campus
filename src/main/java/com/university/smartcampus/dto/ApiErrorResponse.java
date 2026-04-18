package com.university.smartcampus.dto;

import jakarta.ws.rs.core.Response;

import java.time.Instant;

public class ApiErrorResponse {
    private final String error;
    private final String message;
    private final int status;
    private final String timestamp;

    public ApiErrorResponse(String error, String message, int status, String timestamp) {
        this.error = error;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    public static ApiErrorResponse of(Response.StatusType status, String message) {
        return new ApiErrorResponse(
                status.getReasonPhrase().toUpperCase().replace(' ', '_'),
                message,
                status.getStatusCode(),
                Instant.now().toString()
        );
    }

    public static ApiErrorResponse of(Response.Status status, String message) {
        return of((Response.StatusType) status, message);
    }

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(error, message, status, Instant.now().toString());
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
