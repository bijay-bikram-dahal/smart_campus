package com.university.smartcampus.exception;

import com.university.smartcampus.dto.ApiErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.log(Level.SEVERE, "Unhandled server error", exception);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiErrorResponse.of(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred. Please contact the administrator."
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
