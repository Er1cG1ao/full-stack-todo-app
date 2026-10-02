# Validation report — 2026-10-02

## Verified locally

| Check | Result |
|---|---|
| Frontend source formatting | Pass |
| Frontend unit/component tests | 28 passed |
| Strict TypeScript and Angular templates | Enabled; build and no-emit check pass |
| Frontend production build | Pass; initial transfer estimate about 87 kB plus a lazy workspace chunk about 16 kB |
| Backend unit/integration tests | 21 passed, no failures or skips |
| Backend executable packaging | Pass, daylight-api-1.0.0.jar |
| OpenAPI schema/reference validation | Pass |
| Shell launcher/backup syntax | Pass |
| Stopped-file backup helper | Pass; private backup created, ignored by Git |
| Original database integrity | Original file and backup SHA-256 match |
| Real legacy database copy upgrade | Migrated to v4; original file untouched |
| npm dependency audit | 0 reported vulnerabilities at validation time |

Frontend coverage includes authentication/CSRF handling, signup recovery-key display,
password recovery, task views/search/tags/dates, quick add, completion, undo,
selection limits, conflicts and board rendering.

Backend coverage includes registration validation, password hashing, cookie/session
rotation, logout, CORS/CSRF enforcement, anonymous/cross-account denial, rich task
metadata, stale/missing versions, Trash/restore/purge, atomic bulk rollback, recurrence
without duplicate generation, project rename/remove behavior, import rollback,
password/recovery-key changes, one-time key consumption, readiness, concurrent rate
limits and old-schema preservation.

The real legacy database copy contained 3 accounts and 6 tasks before migration.
The actual source database remains unchanged and privately backed up.

## Not verified here

- Real-browser visual, desktop/mobile and end-to-end acceptance: local preview access
  was denied by the browser permission policy. No alternate browser or workaround was used.
- Docker image builds/container health: Docker is not installed on this machine.
- Public HTTPS cookies, proxy deployment, performance/load, penetration tests,
  accessibility audit and disaster recovery drills.

Flyway reports a compatibility warning for H2 2.4.240 (newer than its last verified
H2 version). The included migration/validation tests pass, but this warning is not
treated as an external compatibility certification.

Automated tests/builds are useful evidence, not proof that all defects are eliminated
or that this project is ready for unrestricted public production use.
