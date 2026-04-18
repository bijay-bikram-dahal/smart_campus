package com.university.smartcampus.exception;

import com.university.smartcampus.dto.ApiErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(ApiErrorResponse.of(Response.Status.FORBIDDEN, exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
