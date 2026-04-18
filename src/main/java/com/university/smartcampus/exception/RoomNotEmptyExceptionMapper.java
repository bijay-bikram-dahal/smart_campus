package com.university.smartcampus.exception;

import com.university.smartcampus.dto.ApiErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(ApiErrorResponse.of(Response.Status.CONFLICT, exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
