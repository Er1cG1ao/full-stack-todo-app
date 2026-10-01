# Full-Stack Todo App

A full-stack Todo application built with Angular and Spring Boot. The frontend provides a
simple authenticated workflow and the backend persists per-user todos in an H2 database.

## Features

- Demo login and guarded Angular routes
- Create, read, update, and delete todos
- Per-user data ownership
- Persistent file-based H2 database
- Consistent JSON error responses
- OpenAPI documentation bundled with the backend
- Frontend and backend automated tests

## Tech stack

- Frontend: Angular 22, TypeScript, Bootstrap 5
- Backend: Java 17+, Spring Boot 4, Spring Data JPA, Maven
- Database: H2

## Project structure

```text
todo-app/
├── frontend/   Angular application
└── backend/    Spring Boot REST API
```

## Run locally

### 1. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

The API runs at `http://localhost:8080`. H2 data is stored under `backend/data/`, which is
ignored by Git.

### 2. Start the frontend

Use Node.js 22.22.3+, 24.15.0+, or 26+.

```bash
cd frontend
npm ci
npm start
```

Open `http://localhost:4200` and sign in with:

- Username: `alice`
- Password: `dummy`

The login is intentionally a client-side demonstration. It is not production authentication.

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm test -- --watch=false
```

## API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/hello-world-bean` | Connectivity check |
| GET | `/hello-world/path-variable/{name}` | Personalized connectivity check |
| GET | `/users/{username}/todos` | List a user's todos |
| GET | `/users/{username}/todos/{id}` | Get one todo |
| POST | `/users/{username}/todos` | Create a todo |
| PUT | `/users/{username}/todos/{id}` | Update a todo |
| DELETE | `/users/{username}/todos/{id}` | Delete a todo |

After starting the backend, API documentation is available at
`http://localhost:8080/swagger-ui.html`.
