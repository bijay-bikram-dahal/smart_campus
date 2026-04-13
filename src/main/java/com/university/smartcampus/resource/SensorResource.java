package com.university.smartcampus.resource;

import com.university.smartcampus.exception.LinkedResourceNotFoundException;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.service.InMemoryStorage;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.Locale;
import java.util.List;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @Inject
    private InMemoryStorage storage;

    public SensorResource() {
    }

    SensorResource(InMemoryStorage storage) {
        this.storage = storage;
    }

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors;
        if (type != null && !type.isBlank()) {
            sensors = storage().getSensorsByType(type.trim());
        } else {
            sensors = storage().getAllSensors();
        }
        return Response.ok(sensors).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        validateSensor(sensor);
        if (!storage().roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room with ID: " + sensor.getRoomId() + " does not exist. Cannot link sensor to a non-existent room."
            );
        }

        sensor.setType(sensor.getType().trim());
        sensor.setStatus(sensor.getStatus().trim().toUpperCase(Locale.ROOT));
        sensor.setRoomId(sensor.getRoomId().trim());

        Sensor createdSensor = storage().createSensor(sensor);
        storage().addSensorToRoom(createdSensor.getRoomId(), createdSensor.getId());
        return Response.created(URI.create("/api/v1/sensors/" + createdSensor.getId()))
                .entity(createdSensor)
                .build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = storage().getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        return Response.ok(sensor).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = storage().getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }

        storage().removeSensorFromRoom(sensor.getRoomId(), sensorId);
        boolean deleted = storage().deleteSensor(sensorId);
        if (!deleted) {
            throw new IllegalStateException("Failed to delete sensor with ID: " + sensorId);
        }
        return Response.noContent().build();
    }

    @Path("{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        if (storage().getSensor(sensorId) == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        return new SensorReadingResource(sensorId, storage());
    }

    private InMemoryStorage storage() {
        if (storage == null) {
            throw new IllegalStateException("InMemoryStorage was not injected.");
        }
        return storage;
    }

    private void validateSensor(Sensor sensor) {
        if (sensor == null) {
            throw new BadRequestException("Sensor payload is required.");
        }
        if (sensor.getType() == null || sensor.getType().isBlank()) {
            throw new BadRequestException("Sensor type is required.");
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            throw new BadRequestException("Sensor status is required.");
        }
        String normalizedStatus = sensor.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "MAINTENANCE", "OFFLINE").contains(normalizedStatus)) {
            throw new BadRequestException("Sensor status must be ACTIVE, MAINTENANCE, or OFFLINE.");
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            throw new BadRequestException("Sensor roomId is required.");
        }
    }
}
