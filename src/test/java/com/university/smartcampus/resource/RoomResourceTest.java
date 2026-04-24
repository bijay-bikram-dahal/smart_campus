package com.university.smartcampus.resource;

import com.university.smartcampus.exception.RoomNotEmptyException;
import com.university.smartcampus.model.Room;
import com.university.smartcampus.model.Sensor;
import com.university.smartcampus.service.InMemoryStorage;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomResourceTest {
    private InMemoryStorage storage;
    private RoomResource roomResource;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
        roomResource = new RoomResource(storage);
    }

    @Test
    void createRoomReturnsCreatedEntityAndLocation() {
        Response response = roomResource.createRoom(new Room(null, "Library Quiet Study", 20));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getLocation());

        Room createdRoom = (Room) response.getEntity();
        assertNotNull(createdRoom.getId());
        assertEquals("Library Quiet Study", createdRoom.getName());
        assertEquals(20, createdRoom.getCapacity());
    }

    @Test
    void getAllRoomsReturnsStoredRooms() {
        roomResource.createRoom(new Room(null, "Library Quiet Study", 20));
        roomResource.createRoom(new Room(null, "Physics Lab", 15));

        Response response = roomResource.getAllRooms();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<?> rooms = (List<?>) response.getEntity();
        assertEquals(2, rooms.size());
    }

    @Test
    void deleteRoomRejectsOccupiedRoom() {
        Room room = storage.createRoom(new Room("LIB-301", "Library Quiet Study", 20));
        Sensor sensor = storage.createSensor(new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, room.getId()));
        storage.addSensorToRoom(room.getId(), sensor.getId());

        assertThrows(RoomNotEmptyException.class, () -> roomResource.deleteRoom(room.getId()));
    }

    @Test
    void getRoomThrowsNotFoundForMissingRoom() {
        assertThrows(NotFoundException.class, () -> roomResource.getRoom("missing-room"));
    }

    @Test
    void createRoomRejectsInvalidCapacity() {
        assertThrows(BadRequestException.class, () -> roomResource.createRoom(new Room(null, "Broken Room", 0)));
    }
}
