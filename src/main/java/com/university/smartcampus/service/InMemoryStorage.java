package com.university.smartcampus.service;

import com.university.smartcampus.model.Room;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryStorage {
    // Thread-safe storage for rooms, sensors, and readings
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();
    private final AtomicLong timestampGenerator = new AtomicLong(System.currentTimeMillis());

    // Room operations
    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public Room createRoom(Room room) {
        if (room.getId() == null || room.getId().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }
        rooms.put(room.getId(), room);
        return room;
    }

    public boolean deleteRoom(String id) {
        Room room = rooms.get(id);
        if (room != null && (room.getSensorIds() == null || room.getSensorIds().isEmpty())) {
            rooms.remove(id);
            return true;
        }
        return false;
    }

    public Room addSensorToRoom(String roomId, String sensorId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.addSensorId(sensorId);
            rooms.put(roomId, room);
        }
        return room;
    }

    public Room removeSensorFromRoom(String roomId, String sensorId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.removeSensorId(sensorId);
            rooms.put(roomId, room);
        }
        return room;
    }

    // Sensor operations
    public List<Sensor> getAllSensors() {
        return new ArrayList<>(sensors.values());
    }

    public List<Sensor> getSensorsByType(String type) {
        List<Sensor> result = new ArrayList<>();
        for (Sensor sensor : sensors.values()) {
            if (sensor.getType().equalsIgnoreCase(type)) {
                result.add(sensor);
            }
        }
        return result;
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public Sensor createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }
        sensors.put(sensor.getId(), sensor);
        // Initialize readings list for this sensor
        sensorReadings.putIfAbsent(sensor.getId(), new ArrayList<>());
        return sensor;
    }

    public boolean deleteSensor(String id) {
        Sensor sensor = sensors.remove(id);
        sensorReadings.remove(id);
        return sensor != null;
    }

    public Sensor updateSensorCurrentValue(String sensorId, double value) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensor.setCurrentValue(value);
            sensors.put(sensorId, sensor);
        }
        return sensor;
    }

    // Sensor reading operations
    public List<SensorReading> getSensorReadings(String sensorId) {
        return sensorReadings.getOrDefault(sensorId, Collections.emptyList());
    }

    public SensorReading addSensorReading(String sensorId, SensorReading reading) {
        List<SensorReading> readings = sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>());
        if (reading.getId() == null || reading.getId().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(timestampGenerator.getAndIncrement());
        }
        readings.add(reading);
        return reading;
    }

    // Utility methods
    public boolean roomExists(String roomId) {
        return rooms.containsKey(roomId);
    }

    public boolean sensorExists(String sensorId) {
        return sensors.containsKey(sensorId);
    }

    public void clearAll() {
        rooms.clear();
        sensors.clear();
        sensorReadings.clear();
        timestampGenerator.set(System.currentTimeMillis());
    }
}