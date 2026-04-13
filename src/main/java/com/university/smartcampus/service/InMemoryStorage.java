package com.university.smartcampus.service;

import com.university.smartcampus.model.Room;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryStorage {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();
    private final AtomicLong timestampGenerator = new AtomicLong(System.currentTimeMillis());
    private final Object lock = new Object();

    public List<Room> getAllRooms() {
        synchronized (lock) {
            return rooms.values().stream()
                    .map(this::copyRoom)
                    .sorted(Comparator.comparing(Room::getId))
                    .collect(Collectors.toList());
        }
    }

    public Room getRoom(String id) {
        synchronized (lock) {
            return copyRoom(rooms.get(id));
        }
    }

    public Room createRoom(Room room) {
        synchronized (lock) {
            Room roomToStore = copyRoom(room);
            if (roomToStore.getId() == null || roomToStore.getId().isBlank()) {
                roomToStore.setId(UUID.randomUUID().toString());
            }
            roomToStore.setSensorIds(new ArrayList<>());
            rooms.put(roomToStore.getId(), roomToStore);
            return copyRoom(roomToStore);
        }
    }

    public boolean deleteRoom(String id) {
        synchronized (lock) {
            Room room = rooms.get(id);
            if (room == null || roomHasSensorsInternal(room)) {
                return false;
            }
            rooms.remove(id);
            return true;
        }
    }

    public Room addSensorToRoom(String roomId, String sensorId) {
        synchronized (lock) {
            Room room = rooms.get(roomId);
            if (room != null) {
                room.addSensorId(sensorId);
            }
            return copyRoom(room);
        }
    }

    public Room removeSensorFromRoom(String roomId, String sensorId) {
        synchronized (lock) {
            Room room = rooms.get(roomId);
            if (room != null) {
                room.removeSensorId(sensorId);
            }
            return copyRoom(room);
        }
    }

    public List<Sensor> getAllSensors() {
        synchronized (lock) {
            return sensors.values().stream()
                    .map(this::copySensor)
                    .sorted(Comparator.comparing(Sensor::getId))
                    .collect(Collectors.toList());
        }
    }

    public List<Sensor> getSensorsByType(String type) {
        synchronized (lock) {
            List<Sensor> result = new ArrayList<>();
            for (Sensor sensor : sensors.values()) {
                if (sensor.getType().equalsIgnoreCase(type)) {
                    result.add(copySensor(sensor));
                }
            }
            result.sort(Comparator.comparing(Sensor::getId));
            return result;
        }
    }

    public Sensor getSensor(String id) {
        synchronized (lock) {
            return copySensor(sensors.get(id));
        }
    }

    public Sensor createSensor(Sensor sensor) {
        synchronized (lock) {
            Sensor sensorToStore = copySensor(sensor);
            if (sensorToStore.getId() == null || sensorToStore.getId().isBlank()) {
                sensorToStore.setId(UUID.randomUUID().toString());
            }
            sensors.put(sensorToStore.getId(), sensorToStore);
            sensorReadings.putIfAbsent(sensorToStore.getId(), new ArrayList<>());
            return copySensor(sensorToStore);
        }
    }

    public boolean deleteSensor(String id) {
        synchronized (lock) {
            Sensor sensor = sensors.remove(id);
            sensorReadings.remove(id);
            return sensor != null;
        }
    }

    public Sensor updateSensorCurrentValue(String sensorId, double value) {
        synchronized (lock) {
            Sensor sensor = sensors.get(sensorId);
            if (sensor != null) {
                sensor.setCurrentValue(value);
            }
            return copySensor(sensor);
        }
    }

    public List<SensorReading> getSensorReadings(String sensorId) {
        synchronized (lock) {
            List<SensorReading> readings = sensorReadings.get(sensorId);
            if (readings == null) {
                return List.of();
            }
            return readings.stream()
                    .map(this::copyReading)
                    .sorted(Comparator.comparingLong(SensorReading::getTimestamp))
                    .collect(Collectors.toList());
        }
    }

    public SensorReading addSensorReading(String sensorId, SensorReading reading) {
        synchronized (lock) {
            SensorReading readingToStore = copyReading(reading);
            List<SensorReading> readings = sensorReadings.computeIfAbsent(sensorId, key -> new ArrayList<>());
            if (readingToStore.getId() == null || readingToStore.getId().isBlank()) {
                readingToStore.setId(UUID.randomUUID().toString());
            }
            if (readingToStore.getTimestamp() <= 0) {
                readingToStore.setTimestamp(timestampGenerator.getAndIncrement());
            }
            readings.add(readingToStore);
            return copyReading(readingToStore);
        }
    }

    public boolean roomExists(String roomId) {
        synchronized (lock) {
            return rooms.containsKey(roomId);
        }
    }

    public boolean sensorExists(String sensorId) {
        synchronized (lock) {
            return sensors.containsKey(sensorId);
        }
    }

    public boolean roomHasSensors(String roomId) {
        synchronized (lock) {
            Room room = rooms.get(roomId);
            return room != null && roomHasSensorsInternal(room);
        }
    }

    public void clearAll() {
        synchronized (lock) {
            rooms.clear();
            sensors.clear();
            sensorReadings.clear();
            timestampGenerator.set(System.currentTimeMillis());
        }
    }

    private boolean roomHasSensorsInternal(Room room) {
        return room.getSensorIds() != null && !room.getSensorIds().isEmpty();
    }

    private Room copyRoom(Room room) {
        if (room == null) {
            return null;
        }
        Room copy = new Room(room.getId(), room.getName(), room.getCapacity());
        copy.setSensorIds(room.getSensorIds() == null ? new ArrayList<>() : new ArrayList<>(room.getSensorIds()));
        return copy;
    }

    private Sensor copySensor(Sensor sensor) {
        if (sensor == null) {
            return null;
        }
        return new Sensor(
                sensor.getId(),
                sensor.getType(),
                sensor.getStatus(),
                sensor.getCurrentValue(),
                sensor.getRoomId()
        );
    }

    private SensorReading copyReading(SensorReading reading) {
        if (reading == null) {
            return null;
        }
        return new SensorReading(reading.getId(), reading.getTimestamp(), reading.getValue());
    }
}
