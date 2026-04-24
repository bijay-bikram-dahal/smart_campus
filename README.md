# Smart Campus Sensor & Room Management API

## Overview
This project implements the coursework brief for `5COSC022W Client-Server Architectures` using JAX-RS only. It exposes a versioned REST API for managing rooms, sensors, and sensor-reading history for a university smart-campus scenario.

The implementation keeps all data in memory, uses a singleton storage service for shared state, and applies JAX-RS exception mappers and filters to keep the API predictable, observable, and safe.

Base URL after deployment:

```text
http://localhost:8080/smartcampus/api/v1
```

## Technology Stack
- Java 11 source compatibility
- Maven
- Jersey 3 (JAX-RS)
- Jakarta Servlet 6
- In-memory collections only (`ConcurrentHashMap`, `ArrayList`, synchronized service methods)
- JUnit 5

## Architecture Summary
- `SmartCampusApplication` is the JAX-RS bootstrap class and binds `InMemoryStorage` as a singleton.
- `DiscoveryResource` exposes API metadata at `GET /api/v1`.
- `RoomResource` manages room creation, listing, detail retrieval, and guarded deletion.
- `SensorResource` manages sensors, validates `roomId`, supports filtering by `type`, and exposes the sub-resource locator.
- `SensorReadingResource` handles nested reading history under `/sensors/{sensorId}/readings`.
- Exception mappers convert expected and unexpected failures into JSON responses.
- `LoggingFilter` logs incoming request method/URI and outgoing status code.

## Build Instructions

### Prerequisites
- JDK 17 or later installed locally
- Maven 3.8+ available on your machine
- A Jakarta Servlet compatible container for deployment, such as Tomcat 10.1+

### Build and Test
```bash
mvn clean test package
```

This produces:

```text
target/smartcampus-1.0-SNAPSHOT.war
```

### Run
Deploy `target/smartcampus-1.0-SNAPSHOT.war` to Tomcat 10.1+ (or another Jakarta Servlet 6 compatible container).

If deployed to a default Tomcat instance, the API is available at:

```text
http://localhost:8080/smartcampus/api/v1
```

## Endpoint Summary

### Discovery
- `GET /api/v1`

### Rooms
- `GET /api/v1/rooms`
- `POST /api/v1/rooms`
- `GET /api/v1/rooms/{roomId}`
- `DELETE /api/v1/rooms/{roomId}`

### Sensors
- `GET /api/v1/sensors`
- `GET /api/v1/sensors?type=CO2`
- `POST /api/v1/sensors`
- `GET /api/v1/sensors/{sensorId}`
- `DELETE /api/v1/sensors/{sensorId}`

### Sensor Readings
- `GET /api/v1/sensors/{sensorId}/readings`
- `POST /api/v1/sensors/{sensorId}/readings`

## Sample curl Commands

### 1. Discovery endpoint
```bash
curl -X GET http://localhost:8080/smartcampus/api/v1
```

### 2. Create a room
```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/rooms ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":20}"
```

### 3. Get all rooms
```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/rooms
```

### 4. Get a room by ID
```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/rooms/LIB-301
```

### 5. Create a valid sensor
```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"TEMP-001\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":22.5,\"roomId\":\"LIB-301\"}"
```

### 6. Show 422 for a sensor linked to a missing room
```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"CO2-404\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":450.0,\"roomId\":\"MISSING-ROOM\"}"
```

### 7. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/smartcampus/api/v1/sensors?type=Temperature"
```

### 8. Add a reading to a sensor
```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors/TEMP-001/readings ^
  -H "Content-Type: application/json" ^
  -d "{\"value\":23.7}"
```

### 9. Get reading history
```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/sensors/TEMP-001/readings
```

### 10. Show 403 when posting to a sensor in maintenance
```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors/MAINT-001/readings ^
  -H "Content-Type: application/json" ^
  -d "{\"value\":19.5}"
```

### 11. Show 409 when deleting an occupied room
```bash
curl -X DELETE http://localhost:8080/smartcampus/api/v1/rooms/LIB-301
```

## Error Handling Summary
- `400 Bad Request`: invalid JSON payload shape or invalid field values
- `403 Forbidden`: adding a reading to a `MAINTENANCE` sensor
- `404 Not Found`: missing room or sensor
- `409 Conflict`: deleting a room that still has assigned sensors
- `415 Unsupported Media Type`: request body sent in a format that does not match `@Consumes(MediaType.APPLICATION_JSON)`
- `422 Unprocessable Entity`: sensor payload references a room that does not exist
- `500 Internal Server Error`: generic fallback for unexpected runtime failures

## Testing
Run:

```bash
mvn test
```

Current automated coverage includes:
- `RoomResourceTest`: validates room creation, room listing, room validation, room-not-found handling, and the occupied-room deletion guard.
- `SensorResourceTest`: validates room-link integrity checks, sensor creation, room back-linking, and type filtering.
- `SensorReadingResourceTest`: validates reading creation, reading history retrieval, current-value synchronization, and the maintenance-state constraint.
- `ExceptionMapperTest`: validates JSON payloads and status codes for the custom and global exception mappers.

## Report Answers

### Part 1.1: JAX-RS Resource Lifecycle
By default, JAX-RS resource classes are request-scoped. The runtime typically creates a new resource instance for each incoming request rather than reusing one singleton instance for all calls. That lifecycle reduces accidental shared mutable state inside resource classes, but it does not remove concurrency concerns from the application because all request-scoped resources can still interact with the same shared backend objects.

In this coursework, the shared backend is the in-memory storage layer. If multiple requests hit the API at the same time, they can still race while creating rooms, linking sensors, deleting rooms, or appending readings. To handle that safely, the implementation binds `InMemoryStorage` as a singleton service and keeps the shared collections there. The storage class uses concurrent maps plus synchronized state-changing methods, which is enough for coursework scale and prevents partial updates such as deleting a room while another request links a sensor into it.

### Part 1.2: Why HATEOAS Matters
Hypermedia is useful because it turns the API response into a navigation surface instead of treating the API as a fixed list of hardcoded URLs. A discovery response that advertises `rooms` and `sensors` lets the client learn the valid entry points dynamically.

That helps client developers in several ways:
- it reduces tight coupling to static documentation
- it makes versioned changes easier to absorb
- it allows clients to follow server-provided links instead of guessing endpoint structure
- it improves self-documentation and onboarding, especially for new consumers

Static documentation is still helpful, but hypermedia lets the API itself describe where clients can go next.

### Part 2.1: IDs Only vs Full Room Objects
Returning only room IDs keeps responses smaller and reduces bandwidth, which matters when collections grow large. It is also useful when the client only needs identifiers to drive later detail requests.

Returning full room objects reduces client-side round trips because the client immediately gets the room name, capacity, and linked sensor IDs. That makes list pages and dashboards simpler to build.

The trade-off is therefore:
- IDs only: lower payload cost, more follow-up requests
- full objects: higher payload cost, lower client complexity

For this coursework, returning full room objects is a reasonable default because the domain model is small and clients benefit from richer responses during testing and demonstration.

### Part 2.2: DELETE Idempotency
DELETE is idempotent when repeating the same request does not keep changing the server state after the first successful effect. In this implementation:

- first `DELETE /rooms/{id}` on an empty existing room returns `204 No Content` and removes the room
- repeating the same request later returns `404 Not Found`

The status code changes, but the server state does not change after the first deletion because the room remains absent. That still satisfies idempotency: the repeated request does not produce additional side effects.

For occupied rooms, repeated deletes consistently return `409 Conflict` until the underlying sensor links are removed, so the state is also stable across retries.

### Part 3.1: What `@Consumes(MediaType.APPLICATION_JSON)` Enforces
The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares that the endpoint accepts JSON only. If a client sends `text/plain`, `application/xml`, or another unsupported content type, JAX-RS rejects the request before the resource method executes.

The technical consequence is typically an HTTP `415 Unsupported Media Type` response. This is valuable because:
- the endpoint avoids trying to deserialize the wrong payload format
- the contract stays explicit and predictable
- invalid content negotiation is handled by the framework consistently

So the mismatch is not just a validation failure inside application code. It is a framework-level refusal to invoke a JSON-only method with the wrong media type.

### Part 3.2: Why Query Parameters Suit Filtering Better
`GET /sensors?type=CO2` is a better design than `/sensors/type/CO2` for filtering a collection because the resource being addressed is still the same collection: `sensors`. The `type` value is not a new nested resource; it is a filter applied to the collection view.

Query parameters are stronger for this use case because they:
- keep filters optional
- scale naturally to multiple filters later
- preserve a clean resource hierarchy
- express searching and slicing instead of resource identity

Path parameters are best when the value identifies the resource itself, such as `/rooms/LIB-301`, not when it only narrows a result set.

### Part 4.1: Benefits of the Sub-Resource Locator Pattern
The sub-resource locator keeps the parent `SensorResource` focused on sensor collection concerns and delegates reading-history behavior to `SensorReadingResource`. That separation improves maintainability because the nested logic lives in a class that deals only with the sensor-reading context.

Compared with putting every nested route into one large controller, delegation helps by:
- reducing class size and cognitive load
- grouping related behavior together
- making nested resources easier to test in isolation
- keeping future extension safer, for example if reading-specific validation or analytics are added later

In larger APIs, this pattern prevents a single resource class from turning into a monolithic controller with too many responsibilities.

### Part 5.2: Why 422 Is More Accurate Than 404 Here
`404 Not Found` usually means the target resource identified by the request URI does not exist. In this case, the URI `/api/v1/sensors` is valid and the client is calling the correct endpoint. The problem is inside the otherwise valid JSON payload: the referenced `roomId` does not point to a real room.

That is why `422 Unprocessable Entity` is more semantically accurate:
- the request syntax is valid
- the endpoint exists
- the body was parsed successfully
- the payload still cannot be accepted because a business dependency is missing

Using `422` communicates that the request was understood but failed semantic validation.

### Part 5.4: Cybersecurity Risk of Exposing Stack Traces
Returning raw Java stack traces to external clients leaks implementation detail that should stay internal. An attacker can learn:
- class names and package structure
- framework and library choices
- method names and call flow
- internal file paths
- potential vulnerable components or outdated dependencies
- assumptions about null handling, indexing, or error boundaries

That information makes recon easier and lowers the effort required to craft targeted attacks. A generic `500 Internal Server Error` response is safer because it gives the client the outcome without exposing internal structure.

### Part 5.5: Why JAX-RS Filters Are Better for Logging
Logging is a cross-cutting concern. If logging statements are manually inserted into every resource method, the code becomes repetitive, easy to forget, and inconsistent across endpoints.

JAX-RS filters are better because they:
- centralize request and response logging in one place
- apply uniformly to every endpoint
- keep resource classes focused on business logic
- make logging easier to modify later without editing every resource method

That separation is cleaner architecturally and better aligned with the framework’s intended extension points.
