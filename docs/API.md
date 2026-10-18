# API usage

## Open Swagger UI

Start Daylight and open `/swagger-ui.html` on the same origin as the frontend:

| Setup | Swagger UI |
| --- | --- |
| Angular development server | http://127.0.0.1:4200/swagger-ui.html |
| Docker Compose (default port) | http://localhost:8088/swagger-ui.html |
| Backend only | http://127.0.0.1:8080/swagger-ui.html |

For a deployed instance, append `/swagger-ui.html` to its public app URL.
The UI loads its bundled assets and [OpenAPI contract](../backend/src/main/resources/static/openapi.yaml)
through the same proxy. The contract uses `/` as its server URL, so requests use the
current origin rather than a fixed localhost address.

## Sign in and try requests

1. Create an account through Daylight, or execute `POST /auth/register` in Swagger UI.
   Save the recovery key returned at registration.
2. Execute `POST /auth/login` with your username and password.
3. Execute `GET /auth/me`, then try `/api/tasks` or `/api/projects`.
4. For `PUT /api/tasks/{id}`, include the `version` from the last task response.
   A stale version returns `409 Conflict`; fetch the task again before retrying.
5. Execute `POST /auth/logout` when finished.

The browser retains the HttpOnly session cookie. Swagger UI fetches a fresh CSRF token
from `/auth/csrf` before each same-origin write and attaches the server-provided header.
A missing or invalid session still returns `401`; CSRF tokens do not grant account access.

Other clients must retain cookies and fetch `/auth/csrf` themselves, then include the
returned `headerName` and `token` on every write, including registration, login and logout.
Refresh the token after authentication changes. The [HTTP request examples](../backend/api.http)
provide read requests and logout; their `host` variable can point to your app URL.

New integrations should use `/api/tasks`, `/api/projects` and `/api/backup`.
The older `/users/{username}/todos` routes remain for compatibility.
