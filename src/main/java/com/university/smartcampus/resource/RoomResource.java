package com.university.smartcampus.resource;

import com.university.smartcampus.dto.ApiErrorResponse;
import com.university.smartcampus.exception.RoomNotEmptyException;
import com.university.smartcampus.model.Room;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @Inject
    private InMemoryStorage storage;

    public RoomResource() {
    }

    RoomResource(InMemoryStorage storage) {
        this.storage = storage;
    }

    @GET
    public Response getAllRooms() {
        List<Room> rooms = storage().getAllRooms();
        return Response.ok(rooms).build();
    }

    @POST
    public Response createRoom(Room room) {
        validateRoom(room);

        Room createdRoom = storage().createRoom(room);
        return Response.created(URI.create("/api/v1/rooms/" + createdRoom.getId()))
                .entity(createdRoom)
                .build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = storage().getRoom(roomId);
        if (room == null) {
            throw new NotFoundException("Room not found with ID: " + roomId);
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        if (!storage().roomExists(roomId)) {
            throw new NotFoundException("Room not found with ID: " + roomId);
        }

        if (storage().roomHasSensors(roomId)) {
            throw new RoomNotEmptyException(
                    "Cannot delete room with ID: " + roomId + " because sensors are still assigned to it."
            );
        }

        boolean deleted = storage().deleteRoom(roomId);
        if (!deleted) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ApiErrorResponse.of(Response.Status.CONFLICT, "Room could not be deleted."))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.noContent().build();
    }

    private InMemoryStorage storage() {
        if (storage == null) {
            throw new IllegalStateException("InMemoryStorage was not injected.");
        }
        return storage;
    }

    private void validateRoom(Room room) {
        if (room == null) {
            throw new BadRequestException("Room payload is required.");
        }
        if (room.getName() == null || room.getName().isBlank()) {
            throw new BadRequestException("Room name is required.");
        }
        if (room.getCapacity() <= 0) {
            throw new BadRequestException("Room capacity must be greater than zero.");
        }
    }
}
