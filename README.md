# Ledger Server
## Overview

The Ledger Server is a microservice responsible for handling account transfers and maintaining an immutable, insert-only ledger. It provides REST APIs to apply transfers atomically, query ledger entries, and ensures idempotency by transfer ID.

The service is designed for fault tolerance and concurrent-safe operations.

## Features

- Atomic transfers (balance update + ledger entries)

- Idempotency enforcement by transferId

- Insert-only ledger entries (audit log style)

- Optimistic locking or SELECT ... FOR UPDATE to prevent negative balances

- Structured JSON logging

- Circuit breaker for downstream services

## API Documentation

The service provides OpenAPI/Swagger documentation, but only when running with the dev Spring profile:

- Swagger UI: http://localhost:8081/swagger-ui.html

- OpenAPI JSON/YAML: http://localhost:8081/v3/api-docs

Use this documentation to explore all available endpoints, request/response formats, and example payloads.
When running in prod or other profiles, Swagger UI and API docs are disabled for security reasons.
To access Swagger locally, start the application with the dev profile:

## Startup Instructions
### Prerequisites

Java 21+

Gradle

## Running Locally (H2)

````
    git clone https://github.com/Ntobe/ledger.git
    cd ledger
    ./gradlew clean build
    ./gradlew bootRun --args='--spring.profiles.active=dev' 
````

## Testing

- Unit and integration tests use H2 in-memory database by default.

- Circuit breaker, concurrency, and idempotency tests are included.

````
    ./gradlew test
````

## Quick Docker Startup (H2 In-Memory, Dev Profile)

You can run the Ledger Server locally with Docker without any external database, using H2 in-memory:

1. Build the Docker Image

From the project root, run:

````
    docker build -t ledger-server .
````

2. Run the Ledger Server Container

````
   docker run -p 8081:8081 -e SPRING_PROFILES_ACTIVE=dev ledger-server
````
- -p 8080:8080 exposes the application on localhost:8080

- SPRING_PROFILES_ACTIVE=dev enables the Swagger UI

3. Access the Application
- Swagger UI: http://localhost:8081/swagger-ui.html

- OpenAPI JSON/YAML: http://localhost:8081/v3/api-docs

    All data is stored in-memory (H2) and will be lost when the container stops.

4. Stop the Container

````
    docker stop <container_id_or_name>
````