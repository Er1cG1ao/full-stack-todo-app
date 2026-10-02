# Security model

## Implemented protections

- All task, project and backup endpoints require a server session.
- Ownership is derived from the authenticated principal. Guessed IDs belonging to
  another account return 404. The compatibility username API additionally returns 403
  for another username.
- BCrypt cost 12 passwords; minimum 8 characters, maximum 72 UTF-8 bytes. Usernames
  are case-sensitive and limited to 3–50 letters, digits, dots, hyphens or underscores.
- Session IDs rotate on login. Cookies are HttpOnly, SameSite=Lax, and optionally
  Secure for HTTPS. Sessions expire after 30 minutes of inactivity.
- Spring Security CSRF protects all writes, including login, registration, reset and logout.
- Password changes/reset increment a credential version. Existing sessions fail their
  next request. A separate optimistic row version prevents concurrent credential writes
  from silently overwriting each other.
- Recovery keys contain 256 bits from SecureRandom. Only SHA-256 digests are persisted.
  They are displayed once, compared without ordinary string equality, replaced on
  regeneration and cleared on successful reset. Accounts without a recovery key cannot
  use the reset endpoint.
- Login/register/password/recovery operations have bounded, synchronized in-memory
  rate limits. Responses include HTTP 429 and Retry-After.
- Authentication attempts: 60 per direct IP per 5 minutes; login additionally 10 per
  username/IP per 15 minutes; registration 20 per IP per hour; recovery 10 per IP per
  15 minutes. Restarting the server resets these limits.
- Direct peer addresses are used; attacker-supplied forwarded IP headers are not trusted.
- Task updates require a version on the canonical API. Stale edits return 409.
- Imports and bulk changes are transactional. Invalid items roll back the request.
- Request fields are bounded; server-owned IDs, timestamps and account ownership are
  ignored on task creation. Notes are plain text, rendered with Angular escaping.
- H2 console and demo credentials are absent/disabled; database files, .env and
  backups are ignored by Git. No browser storage contains credentials.
- nginx supplies CSP, frame denial, MIME sniffing protection and a permissions policy.
  Styles allow inline CSS because Angular applies component styles dynamically.

## Deployment responsibilities

Use HTTPS and Secure cookies before Internet exposure. Keep the database volume,
environment file and backups private. Rotate database credentials before first use;
an existing database's password cannot simply be changed by editing .env.
Upgrade dependencies and base images regularly, review changes and test backups.

The direct-peer rate limit is deliberately conservative behind a reverse proxy:
all clients behind nginx share one backend IP budget. Do not blindly trust
X-Forwarded-For to work around it. A public deployment needs a carefully configured
trusted proxy and edge limits; multiple replicas need shared rate/session stores.

This project does not encrypt the H2 file, enforce MFA, provide email verification,
run a distributed abuse service, or claim penetration-testing/compliance certification.
Browser-level visual/end-to-end testing and container runtime checks remain separate
release gates when those environments are available.

## Reporting

Do not post account passwords, recovery keys, database files or backups in public issues.
For a security concern, share a minimal reproduction with the repository owner privately.
