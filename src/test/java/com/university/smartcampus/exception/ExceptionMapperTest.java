package com.university.smartcampus.exception;

import com.university.smartcampus.dto.ApiErrorResponse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionMapperTest {

    @Test
    void roomNotEmptyMapperReturnsConflictPayload() {
        Response response = new RoomNotEmptyExceptionMapper()
                .toResponse(new RoomNotEmptyException("Room still has sensors."));

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("CONFLICT", body.getError());
    }

    @Test
    void linkedResourceMapperReturns422Payload() {
        Response response = new LinkedResourceNotFoundExceptionMapper()
                .toResponse(new LinkedResourceNotFoundException("Missing room reference."));

        assertEquals(422, response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("UNPROCESSABLE_ENTITY", body.getError());
    }

    @Test
    void sensorUnavailableMapperReturnsForbiddenPayload() {
        Response response = new SensorUnavailableExceptionMapper()
                .toResponse(new SensorUnavailableException("Sensor unavailable."));

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("FORBIDDEN", body.getError());
    }

    @Test
    void webApplicationMapperReturnsJsonPayload() {
        Response response = new WebApplicationExceptionMapper()
                .toResponse(new NotFoundException("Room not found."));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("NOT_FOUND", body.getError());
    }

    @Test
    void globalMapperHidesInternalDetailsFromClients() {
        Response response = new GlobalExceptionMapper().toResponse(new NullPointerException("boom"));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals("INTERNAL_SERVER_ERROR", body.getError());
        assertEquals("An unexpected error occurred. Please contact the administrator.", body.getMessage());
    }
}
