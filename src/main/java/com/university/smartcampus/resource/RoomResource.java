package com.university.smartcampus.resource;

import com.university.smartcampus.exception.LinkedResourceNotFoundException;
import com.university.smartcampus.exception.RoomNotEmptyException;
import com.university.smartcampus.model.Room;
import com.university.smartcampus.service.InMemoryStorage;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @Inject
    private InMemoryStorage storage;

    // GET /api/v1/rooms - Get all rooms
    @GET
    public Response getAllRooms() {
        List<Room> rooms = storage.getAllRooms();
        return Response.ok(rooms).build();
    }

    // POST /api/v1/rooms - Create a new room
    @POST
    public Response createRoom(Room room) {
        Room createdRoom = storage.createRoom(room);
        return Response.status(Response.Status.CREATED)
                .entity(createdRoom)
                .build();
    }

    // GET /api/v1/rooms/{roomId} - Get a specific room
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = storage.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room not found with ID: " + roomId)
                    .build();
        }
        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{roomId} - Delete a room
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        boolean deleted = storage.deleteRoom(roomId);
        if (!deleted) {
            Room room = storage.getRoom(roomId);
            if (room == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Room not found with ID: " + roomId)
                        .build();
            } else {
                throw new RoomNotEmptyException("Cannot delete room with ID: " + roomId +
                        " as it still has active sensors assigned to it.");
            }
        }
        return Response.noContent().build();
    }
}