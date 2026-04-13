package com.university.smartcampus.resource;

import com.university.smartcampus.exception.SensorUnavailableException;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.model.SensorReading;
import com.university.smartcampus.service.InMemoryStorage;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

public class SensorReadingResource {

    private final String sensorId;
    private final InMemoryStorage storage;

    public SensorReadingResource(String sensorId, InMemoryStorage storage) {
        this.sensorId = sensorId;
        this.storage = storage;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSensorReadings() {
        List<SensorReading> readings = storage.getSensorReadings(sensorId);
        return Response.ok(readings).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSensorReading(SensorReading reading) {
        validateReading(reading);
        Sensor sensor = storage.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor with ID: " + sensorId + " is in MAINTENANCE status and cannot accept new readings."
            );
        }

        SensorReading savedReading = storage.addSensorReading(sensorId, reading);
        storage.updateSensorCurrentValue(sensorId, savedReading.getValue());
        return Response.created(URI.create("/api/v1/sensors/" + sensorId + "/readings/" + savedReading.getId()))
                .entity(savedReading)
                .build();
    }

    private void validateReading(SensorReading reading) {
        if (reading == null) {
            throw new BadRequestException("Sensor reading payload is required.");
        }
    }
}
