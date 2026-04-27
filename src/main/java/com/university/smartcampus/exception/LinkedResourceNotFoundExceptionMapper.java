package com.university.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "UNPROCESSABLE_ENTITY");
        errorResponse.put("message", exception.getMessage());
        errorResponse.put("status", "422");

        return Response.status(Response.Status.UNPROCESSABLE_ENTITY)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}