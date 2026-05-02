package com.university.smartcampus.resource;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryResourceTest {

    @Test
    @SuppressWarnings("unchecked")
    void discoveryEndpointReturnsVersionContactAndResourceLinks() {
        Response response = new DiscoveryResource().getApiInfo();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertEquals("v1", body.get("version"));
        assertTrue(body.containsKey("contact"));

        Map<String, String> resources = (Map<String, String>) body.get("resources");
        assertEquals("/api/v1", resources.get("self"));
        assertEquals("/api/v1/rooms", resources.get("rooms"));
        assertEquals("/api/v1/sensors", resources.get("sensors"));
    }
}
