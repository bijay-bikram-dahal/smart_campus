# Smart Campus Sensor & Room Management API

## Overview
This is a JAX-RS RESTful API for managing rooms and sensors in a smart campus environment. The API provides endpoints for managing rooms, sensors, and sensor readings with proper error handling and logging.

## Technology Stack
- JAX-RS (Jersey 3.1.0)
- Java 11
- Maven
- In-memory data storage (HashMap/ArrayList)

## API Endpoints

### Discovery
- `GET /api/v1/` - API information and available resources

### Room Management
- `GET /api/v1/rooms` - Get all rooms
- `POST /api/v1/rooms` - Create a new room
- `GET /api/v1/rooms/{roomId}` - Get a specific room
- `DELETE /api/v1/rooms/{roomId}` - Delete a room (fails if room has sensors)

### Sensor Management
- `GET /api/v1/sensors` - Get all sensors (optional `type` query parameter for filtering)
- `POST /api/v1/sensors` - Create a new sensor (must reference existing room)
- `GET /api/v1/sensors/{sensorId}` - Get a specific sensor
- `DELETE /api/v1/sensors/{sensorId}` - Delete a sensor

### Sensor Readings
- `GET /api/v1/sensors/{sensorId}/readings` - Get sensor reading history
- `POST /api/v1/sensors/{sensorId}/readings` - Add a new sensor reading (fails if sensor is in MAINTENANCE status)

## Building and Running the Application

### Prerequisites
- Java JDK 11 or higher
- Maven 3.6 or higher

### Build
```bash
mvn clean package
```

### Run
```bash
mvn tomcat7:run
```
Or deploy the generated WAR file to any servlet container (Tomcat, Jetty, etc.)

The API will be available at: `http://localhost:8080/smartcampus/api/v1/`

## Sample curl Commands

### 1. Get API Information
```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name":"Library Quiet Study","capacity":20}'
```

### 3. Get All Rooms
```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/rooms
```

### 4. Create a Sensor (linked to existing room)
```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type":"Temperature","status":"ACTIVE","currentValue":22.5,"roomId":"LIB-301"}'
```

### 5. Get Sensors by Type
```bash
curl -X GET "http://localhost:8080/smartcampus/api/v1/sensors?type=Temperature"
```

### 6. Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors/SENSOR-ID/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.0}'
```

### 7. Get Sensor Readings History
```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/sensors/SENSOR-ID/readings
```

## Error Handling
The API implements comprehensive error handling:
- 409 Conflict: When trying to delete a room with active sensors
- 422 Unprocessable Entity: When linking a sensor to a non-existent room
- 403 Forbidden: When trying to add readings to a sensor in MAINTENANCE status
- 500 Internal Server Error: For unexpected exceptions
- 404 Not Found: When resources don't exist

## Logging
All API requests and responses are logged using java.util.logging.Logger for observability.

## Answers to Coursework Questions

### Part 1: Service Architecture & Setup
1. **JAX-RS Resource Lifecycle**: By default, JAX-RS resource classes are instantiated per-request. This means a new instance is created for each incoming HTTP request. This impacts in-memory data management by requiring thread-safe storage solutions since multiple instances may access shared data concurrently. We use ConcurrentHashMap and proper synchronization to prevent race conditions.

2. **Discovery Endpoint & HATEOAS**: The discovery endpoint provides hypermedia links (HATEOAS) which allows clients to dynamically discover API resources without hardcoding URLs. This makes the API more resilient to changes and reduces client-server coupling.

### Part 2: Room Management
1. **Returning IDs vs Full Objects**: Returning only IDs reduces bandwidth usage but requires clients to make additional requests for details. Returning full objects increases bandwidth but reduces client-side complexity. For list operations, returning IDs is often preferred for efficiency, with expansion available via query parameters.

2. **DELETE Idempotency**: DELETE is idempotent in our implementation. Deleting an already-deleted room returns 404 (not found) on subsequent calls, which is acceptable as the end result (room remains deleted) is the same.

### Part 3: Sensor Operations & Linking
1. **@Consumes Annotation**: If a client sends data in a format not matching @Consumes (e.g., text/plain instead of application/json), JAX-RS returns 415 Unsupported Media Type. This ensures type safety and prevents processing of malformed data.

2. **Query Parameters vs Path for Filtering**: Query parameters (/api/v1/sensors?type=CO2) are superior for filtering because they allow combining multiple filters, remain optional without changing URL structure, and don't imply resource hierarchy. Path-based filtering (/api/v1/sensors/type/CO2) suggests type is a resource rather than a filter attribute.

### Part 4: Deep Nesting with Sub-Resources
1. **Sub-Resource Locator Benefits**: This pattern separates concerns, improves code maintainability, and allows reusable sub-resources. Instead of one large controller with all nested paths, we have focused classes handling specific responsibilities.

2. **Historical Data Management**: Our implementation stores readings in a per-sensor list and updates the sensor's currentValue on each new reading to maintain consistency between historical data and current state.

### Part 5: Advanced Error Handling & Logging
1. **Resource Conflict (409)**: Used when deleting rooms with sensors to indicate the conflict with current state.
2. **Dependency Validation (422)**: More accurate than 404 for missing references in valid payloads, as the room resource exists conceptually but the link is invalid.
3. **State Constraint (403)**: Appropriate for sensors in MAINTENANCE status refusing new readings.
4. **Global Safety Net (500)**: Prevents leaking stack traces that could reveal internal implementation details to attackers.
5. **Logging Filters**: Centralized logging via AOP-style filters is cleaner than manual logging in each method, ensuring consistent observability without code duplication.
