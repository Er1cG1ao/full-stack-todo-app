# Running and operating Daylight

## Local use

Use the root launcher or the two-terminal commands in README.md.
Database migrations run automatically before the API starts; Hibernate validates
the result. Take a stopped-file backup before upgrading an existing installation.
Never enable automatic schema deletion or reset the data directory to fix a migration.

If ports 8080 or 4200 are occupied, stop the existing application. The launcher does
not terminate other processes or change firewall/browser settings.

## Optional Docker Compose

Docker/Compose is optional; do not buy a hosted service to run this project.
The supplied configuration binds the frontend to loopback and keeps the backend
private inside the Compose network.

    cp .env.example .env
    # Edit .env: choose a strong DB_PASSWORD, do not leave it blank.
    docker compose up --build -d
    docker compose logs -f

Open http://localhost:8088. Check readiness at /health. The backend runs as an
unprivileged user, has a memory limit and keeps H2 data in the todo-data named volume.
The frontend container health check tests the backend/database through nginx.

    docker compose down

Stopping containers keeps the volume. Do not add a volume-deletion flag unless you
intend to erase every account and task. This configuration does not deploy anything
to a cloud provider and does not request paid resources.

Container recipes use official major-version image tags. Pin reviewed image digests
for a reproducible public release and scan/update them regularly. The development
machine used for this delivery did not have Docker, so the recipes have not been
runtime-verified here. Native production builds and API tests are verified.

## Public HTTPS deployment checklist

1. Obtain a domain/HTTPS termination only under your own explicit spending approval.
2. Keep backend 8080 inaccessible from the public network.
3. Route frontend and API through the same origin. Preserve Host; configure the
   exact APP_ORIGIN and set SESSION_COOKIE_SECURE=true.
4. Configure trusted proxy handling and edge rate limits. Current application limits
   use direct peer addresses and are shared by clients behind one nginx proxy.
5. Restrict filesystem access, protect .env/backups and maintain a tested backup schedule.
6. Use one backend instance with H2. Horizontal scaling needs a server database,
   shared sessions and shared rate limits.
7. Validate browser behavior on desktop/mobile, test cookie/CSRF handling over HTTPS,
   and run container smoke/security tests before inviting users.

No HTTPS certificate, DNS change, server purchase or hosting subscription was created.

## Backups and restore

In-app JSON export contains only the current account's tasks and projects. It includes
Trash but excludes credentials. Import adds fresh task IDs under the current account;
old ownership and creation/update/completion timestamps are not restored. Deleted
items remain in Trash. Project names are reused if present; existing colors win.
Imports accept 500 tasks and 100 projects, with a 5 MB UI/nginx request limit.
Large exports can be split into valid version-1 backup files; for full-fidelity
recovery including all accounts, use a stopped-database backup instead.

For the native installation:

    # Stop the backend first:
    ./scripts/backup.sh

Keep a second copy on storage you already own. The helper refuses to copy an open
database and never overwrites an existing backup. Backups contain private data and
password/recovery digests; they are not safe public artifacts.

To restore, stop the backend, preserve the current database as a separate backup, then
copy the selected backup to backend/data/todo-db.mv.db. Check permissions and retain
the matching database password. Start the same or a newer compatible app version.
Older app versions may not understand newer schemas; rollback needs the pre-upgrade
database snapshot, not just an older executable.

For Compose, stop the backend container before backing up/restoring the volume.
scripts/backup.sh operates on the native data directory, not on Docker volumes.

## Intentional limits

- Personal accounts, not collaborative/team workspaces.
- Priority board is a second view; change priorities through the task editor.
- Daily/weekly/monthly recurrence creates one next task when completed. An overdue
  task advances from today, avoiding a backlog; reopening/recompleting the same
  occurrence does not generate another copy. New occurrences reset subtasks.
- Dates are calendar dates. There are no time-of-day reminders or external notifications.
- Recurrence uses the browser's IANA time zone sent as X-Time-Zone. API clients may
  send the same optional header; without it the server's calendar date is used.
- Online-only writes; connection failures are surfaced, not silently stored offline.
- Task lists load in memory. This is not designed for millions of tasks per account.
- No automatic email recovery. Save your recovery key and generate another after use.
