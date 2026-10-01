# Todo App Backend

Spring Boot REST API for the Todo application. Todos are stored in a file-based H2 database
and belong to a seeded application user.

## Requirements

- Java 17+
- No separate Maven installation is required

## Run

```bash
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. The H2 console is available at
`http://localhost:8080/h2-console`; use JDBC URL `jdbc:h2:file:./data/todo-db` and username
`sa` with a blank password.

The database seeds `alice`, `bob`, and `carol` on first launch. The Angular demo login uses
`alice`.

## Verify

```bash
./mvnw test
```

The integration test covers the complete create, read, update, and delete lifecycle against an
isolated in-memory H2 database.
