package com.university.smartcampus.resource;

import com.university.smartcampus.exception.LinkedResourceNotFoundException;
import com.university.smartcampus.model.Room;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.service.InMemoryStorage;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SensorResourceTest {
    private InMemoryStorage storage;
    private SensorResource sensorResource;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
        sensorResource = new SensorResource(storage);
        storage.createRoom(new Room("LIB-301", "Library Quiet Study", 20));
    }

    @Test
    void createSensorRequiresExistingRoom() {
        Sensor sensor = new Sensor(null, "Temperature", "ACTIVE", 20.0, "missing-room");

        assertThrows(LinkedResourceNotFoundException.class, () -> sensorResource.createSensor(sensor));
    }

    @Test
    void createSensorLinksBackToRoomAndReturnsLocation() {
        Response response = sensorResource.createSensor(
                new Sensor(null, "Temperature", "active", 22.3, "LIB-301")
        );

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getLocation());

        Sensor createdSensor = (Sensor) response.getEntity();
        assertEquals("ACTIVE", createdSensor.getStatus());
        assertEquals(1, storage.getRoom("LIB-301").getSensorIds().size());
    }

    @Test
    void getAllSensorsSupportsCaseInsensitiveTypeFilter() {
        sensorResource.createSensor(new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.3, "LIB-301"));
        sensorResource.createSensor(new Sensor("CO2-001", "CO2", "ACTIVE", 430.0, "LIB-301"));

        Response response = sensorResource.getAllSensors("temperature");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<?> sensors = (List<?>) response.getEntity();
        assertEquals(1, sensors.size());
    }

    @Test
    void subResourceLocatorRejectsMissingSensor() {
        assertThrows(NotFoundException.class, () -> sensorResource.getSensorReadingResource("missing-sensor"));
    }
}
