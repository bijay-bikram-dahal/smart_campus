# Smart Campus Sensor & Room Management API

## Overview
This project implements the coursework brief for `5COSC022W Client-Server Architectures` using JAX-RS only. It exposes a versioned REST API for managing rooms, sensors, and sensor-reading history for a university smart-campus scenario.

The implementation keeps all data in memory, uses a singleton storage service for shared state, and applies JAX-RS exception mappers and filters to keep the API predictable, observable, and safe.

Base URL when launched with the embedded server:

```text
http://localhost:8080/api/v1
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

### Run with the embedded server
```bash
mvn exec:java
```

The server starts at:

```text
http://localhost:8080/api/v1
```

If port `8080` is already in use, the launcher automatically retries the next available port unless an explicit port is supplied. To force a specific port:

```bash
mvn exec:java -Dsmartcampus.port=8081
```

### Run with Tomcat
Deploy `target/smartcampus-1.0-SNAPSHOT.war` to Tomcat 10.1+ (or another Jakarta Servlet 6 compatible container).

The WAR maps the Jersey servlet under `/api/v1/*`. The final URL therefore depends on the Tomcat context path. For example:

```text
http://localhost:8080/smartcampus-1.0-SNAPSHOT/api/v1
```

If the WAR is deployed as the root application, the URL is:

```text
http://localhost:8080/api/v1
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

The commands below are ordered so they can be used as a short demonstration sequence. They use Windows command continuation (`^`). On macOS or Linux, replace `^` with `\`.

### 1. Discovery endpoint
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Create a room
```bash
curl -X POST http://localhost:8080/api/v1/rooms ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":20}"
```

### 3. Get all rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 4. Get a room by ID
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Create a valid sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"TEMP-001\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":22.5,\"roomId\":\"LIB-301\"}"
```

### 6. Show 422 for a sensor linked to a missing room
```bash
curl -X POST http://localhost:8080/api/v1/sensors ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"CO2-404\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":450.0,\"roomId\":\"MISSING-ROOM\"}"
```

### 7. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"
```

### 8. Add a reading to a sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings ^
  -H "Content-Type: application/json" ^
  -d "{\"value\":23.7}"
```

### 9. Get reading history
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 10. Show 403 when posting to a sensor in maintenance
First create a maintenance sensor in an existing room:

```bash
curl -X POST http://localhost:8080/api/v1/sensors ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"MAINT-001\",\"type\":\"Temperature\",\"status\":\"MAINTENANCE\",\"currentValue\":18.0,\"roomId\":\"LIB-301\"}"
```

Then attempt to append a reading:

```bash
curl -X POST http://localhost:8080/api/v1/sensors/MAINT-001/readings ^
  -H "Content-Type: application/json" ^
  -d "{\"value\":19.5}"
```

### 11. Show successful room deletion
```bash
curl -X POST http://localhost:8080/api/v1/rooms ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"TMP-101\",\"name\":\"Temporary Seminar Room\",\"capacity\":12}"
```

```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/TMP-101
```

### 12. Show 409 when deleting an occupied room
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 13. Show 415 for an unsupported request content type
```bash
curl -X POST http://localhost:8080/api/v1/rooms ^
  -H "Content-Type: text/plain" ^
  -d "LIB-302"
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
- `DiscoveryResourceTest`: validates API metadata, contact information, and hypermedia resource links.

## Video Demonstration Checklist
Use Postman or curl and show the response status, JSON body, and console logging output where relevant.

| Rubric area | Demonstrate |
| --- | --- |
| Part 1: Setup & Discovery | Start the server and call `GET /api/v1` to show version, contact, and resource links. |
| Part 2: Room Management | Create a room with `201 Created`, fetch it, list rooms, delete an empty room with `204`, and show `409 Conflict` when deleting a room with sensors. |
| Part 3: Sensors & Filtering | Create a sensor linked to an existing room, show `422` for a missing `roomId`, and filter with `GET /sensors?type=Temperature`. |
| Part 4: Sub-Resources | Use `/sensors/{sensorId}/readings` to POST a reading, GET reading history, and show that the parent sensor `currentValue` changed. |
| Part 5: Error Handling & Logging | Show `409`, `422`, `403`, `415`, and the request/response log entries containing method, URI, and status. |

## Rubric Alignment Checklist

| Criterion | Evidence in this project |
| --- | --- |
| 1.1 Architecture & Config | Maven WAR project using Jersey/JAX-RS only; `SmartCampusApplication` uses `@ApplicationPath("/api/v1")`; `InMemoryStorage` is bound as a singleton and synchronizes shared in-memory state. |
| 1.2 Discovery Endpoint | `DiscoveryResource` returns version, base path, contact details, and resource links from `GET /api/v1`. |
| 2.1 Room Implementation | `RoomResource` supports `GET /rooms`, `POST /rooms`, and `GET /rooms/{roomId}` with JSON responses and `Location` on creation. |
| 2.2 Deletion & Safety Logic | `DELETE /rooms/{roomId}` returns `204` for empty rooms and throws `RoomNotEmptyException` mapped to `409` for rooms with sensors. |
| 3.1 Sensor Integrity | `SensorResource` validates `roomId` before registration and maps missing linked rooms to `422 Unprocessable Entity`. |
| 3.2 Filtered Retrieval | `GET /sensors?type=...` filters sensors by type using `@QueryParam`, with case-insensitive matching. |
| 4.1 Sub-Resource Locator | `SensorResource#getSensorReadingResource` delegates `/sensors/{sensorId}/readings` to `SensorReadingResource`. |
| 4.2 Historical Management | `SensorReadingResource` supports `GET` and `POST`; successful `POST` appends history and updates the parent sensor's `currentValue`. |
| 5.1 Resource Conflict | `RoomNotEmptyExceptionMapper` returns `409 Conflict` with a JSON `ApiErrorResponse`. |
| 5.2 Dependency Validation | `LinkedResourceNotFoundExceptionMapper` returns `422` with a JSON error body. |
| 5.3 State Constraint | `SensorUnavailableExceptionMapper` returns `403 Forbidden` for readings posted to maintenance sensors. |
| 5.4 Global Safety Net | `GlobalExceptionMapper` catches unexpected `Throwable` values and returns a generic JSON `500` without stack traces. |
| 5.5 Logging Filters | `LoggingFilter` implements both request and response filters and logs HTTP method, URI, and status code. |

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
