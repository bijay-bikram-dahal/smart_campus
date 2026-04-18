package com.university.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response getApiInfo() {
        Map<String, Object> apiInfo = new HashMap<>();
        apiInfo.put("name", "Smart Campus Sensor & Room Management API");
        apiInfo.put("version", "v1");
        apiInfo.put("basePath", "/api/v1");
        apiInfo.put("description", "JAX-RS service for rooms, sensors, and historical sensor readings.");

        Map<String, String> contact = new HashMap<>();
        contact.put("team", "Smart Campus Platform");
        contact.put("email", "smartcampus-admin@westminster.ac.uk");
        apiInfo.put("contact", contact);

        Map<String, String> resources = new HashMap<>();
        resources.put("self", "/api/v1");
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        apiInfo.put("resources", resources);

        return Response.ok(apiInfo).build();
    }
}
