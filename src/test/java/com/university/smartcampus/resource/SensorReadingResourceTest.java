package com.university.smartcampus.resource;

import com.university.smartcampus.exception.SensorUnavailableException;
import com.university.smartcampus.model.Room;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.model.SensorReading;
import com.university.smartcampus.service.InMemoryStorage;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SensorReadingResourceTest {
    private InMemoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
        storage.createRoom(new Room("LIB-301", "Library Quiet Study", 20));
    }

    @Test
    void addSensorReadingUpdatesParentSensorCurrentValue() {
        storage.createSensor(new Sensor("TEMP-001", "Temperature", "ACTIVE", 20.0, "LIB-301"));
        SensorReadingResource resource = new SensorReadingResource("TEMP-001", storage);

        Response response = resource.addSensorReading(new SensorReading(null, 0L, 24.6));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getLocation());

        SensorReading savedReading = (SensorReading) response.getEntity();
        assertNotNull(savedReading.getId());
        assertEquals(24.6, storage.getSensor("TEMP-001").getCurrentValue());
        assertEquals(1, storage.getSensorReadings("TEMP-001").size());
    }

    @Test
    void addSensorReadingRejectsMaintenanceSensors() {
        storage.createSensor(new Sensor("TEMP-001", "Temperature", "MAINTENANCE", 20.0, "LIB-301"));
        SensorReadingResource resource = new SensorReadingResource("TEMP-001", storage);

        assertThrows(
                SensorUnavailableException.class,
                () -> resource.addSensorReading(new SensorReading(null, 0L, 24.6))
        );
    }

    @Test
    void getSensorReadingsReturnsHistoricalData() {
        storage.createSensor(new Sensor("TEMP-001", "Temperature", "ACTIVE", 20.0, "LIB-301"));
        storage.addSensorReading("TEMP-001", new SensorReading("R-1", 100L, 21.0));
        storage.addSensorReading("TEMP-001", new SensorReading("R-2", 200L, 22.0));
        SensorReadingResource resource = new SensorReadingResource("TEMP-001", storage);

        Response response = resource.getSensorReadings();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<?> readings = (List<?>) response.getEntity();
        assertEquals(2, readings.size());
    }
}
