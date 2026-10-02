# Daylight API

Java 17+ / Spring Boot 4 / Spring Security / Spring Data JPA / Flyway / H2.
Use Java 21 for the same runtime as CI.

    ./mvnw spring-boot:run
    ./mvnw --batch-mode --no-transfer-progress verify

The API listens on 8080. Data is under data/; no accounts are seeded.
The H2 web console is disabled. Register through the UI or POST /auth/register.

See the [root README](../README.md), [security model](../docs/SECURITY.md) and
[OpenAPI contract](src/main/resources/static/openapi.yaml). Swagger UI is bundled
locally at /swagger-ui.html, with no external CDN.

The older /users/{username}/todos API is retained for compatibility but is not used
by the new UI. Ownership is enforced there too. Its DELETE now moves tasks to Trash.
New integrations should use /api/tasks, including version-checked updates.

EXERCISE_GUIDE_ZH.md is retained as historical learning material, not the current API contract.
