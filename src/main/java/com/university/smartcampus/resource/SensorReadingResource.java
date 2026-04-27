package com.university.smartcampus.resource;

import com.university.smartcampus.exception.SensorUnavailableException;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.model.SensorReading;
import com.university.smartcampus.service.InMemoryStorage;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

public class SensorReadingResource {

    private final String sensorId;
    private final InMemoryStorage storage;

    public SensorReadingResource(String sensorId, InMemoryStorage storage) {
        this.sensorId = sensorId;
        this.storage = storage;
    }

    // GET /api/v1/sensors/{sensorId}/readings - Get sensor reading history
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSensorReadings() {
        List<SensorReading> readings = storage.getSensorReadings(sensorId);
        return Response.ok(readings).build();
    }

    // POST /api/v1/sensors/{sensorId}/readings - Add a new sensor reading
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSensorReading(SensorReading reading) {
        // Check if sensor exists and is not in MAINTENANCE status
        Sensor sensor = storage.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Sensor not found with ID: " + sensorId)
                    .build();
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Sensor with ID: " + sensorId +
                    " is currently in MAINTENANCE status and cannot accept new readings.");
        }

        // Add the reading
        SensorReading savedReading = storage.addSensorReading(sensorId, reading);

        // Update the sensor's current value for consistency
        storage.updateSensorCurrentValue(sensorId, reading.getValue());

        return Response.status(Response.Status.CREATED)
                .entity(savedReading)
                .build();
    }
}