# Daylight

A calm, private workspace for tasks, projects and everyday progress.
Angular 22 + Spring Boot 4 + persistent H2 storage, in one repository.

## What you can do

- Register, sign in, sign out, change your password and recover access with a one-time recovery key
- Create tasks with notes, optional due dates, priorities, tags and subtasks
- Organize tasks into colored projects; rename or remove projects without losing tasks
- Focus with Inbox, Today (including overdue), Upcoming, Important and Completed views
- Search titles, notes, tags, projects and subtasks; filter and sort your results
- Switch between a grouped list and a priority board
- Repeat tasks daily, weekly or monthly; completing one generates its next occurrence
- Quickly add, duplicate, complete, reopen or bulk-update tasks
- Move tasks to Trash, undo deletion, restore them or confirm permanent deletion
- Export tasks/projects as JSON and import a backup without overwriting existing data
- Use light/dark themes, a responsive sidebar and keyboard shortcuts: N for new task, / for search

## Run locally

Requirements: Java 17+ (21 recommended), Node.js 24.19.0, npm 11.16.0.
No paid account or separate database server is required.

    # From the repository root:
    ./scripts/dev.sh

Open [Daylight](http://127.0.0.1:4200). Create an account and save your recovery key in
a password manager. There are no default/demo accounts. Stop both services with Ctrl+C.
The launcher builds the backend and installs frontend dependencies if missing.
It never installs Java/Node or changes their system settings.

Or use two terminals:

    cd backend
    ./mvnw spring-boot:run

    cd frontend
    npm ci
    npm start

Frontend API calls use relative URLs. Angular proxies /auth and /api to port 8080;
nginx does the same in the container deployment. There is no hardcoded production hostname.

## Data and account safety

H2 stores accounts, password hashes, recovery-key digests, projects and tasks in
backend/data/todo-db.mv.db. This directory is ignored by Git.
Passwords use BCrypt with cost 12. Session cookies are HttpOnly and SameSite=Lax;
all writes require CSRF tokens. The backend always derives ownership from the session,
not from a client-supplied username.

Sessions expire after 30 minutes of inactivity and do not survive server restarts.
Changing/resetting a password invalidates existing sessions on their next request.
Only theme and layout preferences are stored in browser local storage; credentials and
tasks are not. Recovery keys are shown once, stored only as SHA-256 digests, and consumed
on use. Generating another key invalidates the previous one.

Flyway versions the database schema; Hibernate validates it instead of silently changing
it. Existing data is preserved. Old accounts with no password hash remain locked and
cannot be claimed through public registration.

Stop the backend before taking a whole-database backup:

    ./scripts/backup.sh

This writes a private, Git-ignored backup under backups/. In-app export contains tasks
and projects, not credentials. Treat both kinds of backup as private.

## Verify

    cd frontend
    npm ci
    npm run check

    cd backend
    ./mvnw --batch-mode --no-transfer-progress verify

Tests cover UI interactions, filtering, authentication, CSRF, cross-account access,
optimistic edit conflicts, atomic bulk actions, recurrence, soft deletion, project
renames, backup rollback, password/recovery-key changes, rate limits and legacy migration.
GitHub Actions runs format checks, frontend tests/build and backend tests/package.
Tests use isolated databases, never your local data.

## Deployment and API

For an optional local container deployment, copy .env.example to .env, choose a
strong database password, then run:

    docker compose up --build -d

Open http://localhost:8088. The backend is not published to the host; data lives in a
named volume. This is a local HTTP setup, not a public HTTPS deployment.

- [Deployment, backups and limitations](docs/DEPLOYMENT.md)
- [Security model](docs/SECURITY.md)
- [中文交付说明](docs/HANDOFF.zh-CN.md)
- [API reference](backend/src/main/resources/static/openapi.yaml)
- Local Swagger UI: http://localhost:8080/swagger-ui.html
- Readiness: GET /health checks database connectivity and returns only UP/DOWN

## Boundaries

This is a polished single-instance personal productivity app, not a claim of audited
enterprise readiness. H2 and in-memory sessions/rate limits are intentional local-first
choices. There is no team sharing, file storage, offline synchronization, push/email
reminders, SSO or hosted service. Lost passwords require a saved recovery key; there is
no email-based recovery or email verification.

Backup imports accept at most 500 tasks and 100 projects per file; the UI accepts files
up to 5 MB. Imports add data, so importing the same file twice creates duplicate tasks.
Bulk operations accept up to 100 tasks. Recurrence is calendar-based, not time-of-day.
See the deployment guide before exposing the app to the Internet.

## Layout

    frontend/       Angular workspace, themes, icons and tests
    backend/        REST API, security, Flyway migrations and integration tests
    scripts/        Local launcher and stopped-database backup helper
    docs/           Operating notes, security model and delivery report
    compose.yaml    Optional local nginx + Java deployment

No paid services, hosting resources or subscriptions are provisioned by this project.
