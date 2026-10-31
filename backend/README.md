# TaskFlow backend

Java 21 / Spring Boot 3.4 REST API with JPA/Hibernate, BCrypt, opaque database-backed sessions and MySQL persistence. Run from the repository root with `./mvnw -pl backend test` or `./mvnw -pl backend spring-boot:run` (Maven Wrapper is supplied at the root).

## Runtime configuration

Set `DATABASE_URL` to the managed MySQL URL (`mysql://user:password@host:3306/database?...`) or set `JDBC_DATABASE_URL` to a JDBC URL directly. The managed URL parser handles the platform's Node-style JSON `ssl` option without passing it to Connector/J, decodes percent-escaped credentials, and requires `sslMode=VERIFY_IDENTITY` for non-local hosts. Local development can instead use `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`. Configure exact browser origins in `TASKFLOW_CORS_ALLOWED_ORIGINS` as a comma-separated list. `TASKFLOW_COOKIE_SECURE` defaults to `true`; use `false` only for plain-HTTP local testing. Cookie-authenticated POST/PUT/PATCH/DELETE requests must send `X-TaskFlow-Request: 1`.

Hibernate uses `ddl-auto=update`. `src/main/resources/schema.sql` documents the corresponding MySQL tables, foreign keys, checks, and indexes. No test uses the managed database: the `test` profile uses an isolated in-memory H2 database with create/drop lifecycle.

## Development-only seed

Activate the `dev` or `local` Spring profile and set `TASKFLOW_SEED_DEMO=true` plus a private `TASKFLOW_DEMO_PASSWORD` before startup. The seed runs only when the employee table is empty and creates `manager@taskflow.demo`, `avery@taskflow.demo`, `jordan@taskflow.demo`, and `sam@taskflow.demo`; all use the supplied password. Never enable this seed in production. No demo credential is embedded in Java source.

## REST surface

- `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`
- Manager: `/api/employees` CRUD (delete soft-deactivates); `/api/dashboard/employee-progress`; `/api/reports/task-summary`, `/api/reports/employee-performance`
- Authenticated dashboards: `/api/dashboard/stats` and `/api/dashboard/recent-tasks` are scoped to the signed-in role/employee.
- Tasks: `GET/POST /api/tasks`, `GET/PUT/DELETE /api/tasks/{id}`, `PATCH /api/tasks/{id}/status`
- Public liveness: `GET /api/health`; readiness: `GET /api/health/ready`

Task and employee listings are database-paged. Task queries support search/status/priority/employeeId/overdue and allowlisted sort fields. Employee users are always scoped to their own tasks; they may only change status. Passwords and token values are never persisted in plaintext or returned in DTOs.
