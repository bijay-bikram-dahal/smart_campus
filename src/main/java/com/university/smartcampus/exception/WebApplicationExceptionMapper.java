package com.university.smartcampus.exception;

import com.university.smartcampus.dto.ApiErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response originalResponse = exception.getResponse();
        Response.StatusType status = originalResponse.getStatusInfo();
        String message = exception.getMessage();
        if (message == null || message.isBlank() || message.startsWith("HTTP ")) {
            message = status.getReasonPhrase();
        }

        return Response.fromResponse(originalResponse)
                .entity(ApiErrorResponse.of(status, message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
