package com.university.smartcampus.resource;

import com.university.smartcampus.exception.LinkedResourceNotFoundException;
import com.university.smartcampus.exception.SensorUnavailableException;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.service.InMemoryStorage;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @Inject
    private InMemoryStorage storage;

    // GET /api/v1/sensors - Get all sensors (with optional type filter)
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors;
        if (type != null && !type.isEmpty()) {
            sensors = storage.getSensorsByType(type);
        } else {
            sensors = storage.getAllSensors();
        }
        return Response.ok(sensors).build();
    }

    // POST /api/v1/sensors - Create a new sensor
    @POST
    public Response createSensor(Sensor sensor) {
        // Validate that the room exists
        if (!storage.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room with ID: " + sensor.getRoomId() + " does not exist. Cannot link sensor to non-existent room.");
        }

        Sensor createdSensor = storage.createSensor(sensor);
        // Link the sensor to the room
        storage.addSensorToRoom(sensor.getRoomId(), createdSensor.getId());
        return Response.status(Response.Status.CREATED)
                .entity(createdSensor)
                .build();
    }

    // GET /api/v1/sensors/{sensorId} - Get a specific sensor
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = storage.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Sensor not found with ID: " + sensorId)
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // DELETE /api/v1/sensors/{sensorId} - Delete a sensor
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = storage.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Sensor not found with ID: " + sensorId)
                    .build();
        }

        // Remove sensor from room
        storage.removeSensorFromRoom(sensor.getRoomId(), sensorId);

        // Delete sensor
        boolean deleted = storage.deleteSensor(sensorId);
        if (!deleted) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to delete sensor")
                    .build();
        }
        return Response.noContent().build();
    }

    // Sub-resource locator for sensor readings
    @Path("{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        // Verify sensor exists
        if (storage.getSensor(sensorId) == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        return new SensorReadingResource(sensorId, storage);
    }
}