# TaskFlow — implementation and design plan

## Approved scope and architecture

Deliver a full-stack employee task management system, not a static dashboard. The frontend is React 18, Vite and JavaScript, using React Router and a centralized Axios client. The primary application backend is Java 21 with Spring Boot, Spring Web, Spring Data JPA/Hibernate, Spring Security, validation, and Maven. MySQL is the durable data store, accessed through the platform-managed MySQL-compatible service via runtime environment configuration. The managed database is shared across development and published runtime; schema creation and seed behavior must be deterministic and non-destructive.

During development, Vite listens on the configured Preview port 3000 and proxies `/api` requests to the Spring Boot service on 8080. In the production image, Maven packages the Spring application and Vite compiles the client; Spring serves the built static client and the REST API from a single listener honoring `PORT` (default 3000). The browser uses same-origin relative API paths. Maven Wrapper files make the Java build reproducible even though Maven is not installed in the sandbox.

Authentication follows the brief's explicit email/password requirement rather than the platform default identity provider. Spring Security verifies BCrypt password hashes and issues an HTTP-only application session cookie; every request resolves the user and role server-side. `MANAGER` APIs are restricted server-side; `EMPLOYEE` queries are scoped to the authenticated employee and employee status updates accept only the status field. Passwords and secrets never enter frontend bundles or API DTOs. Development-only demo accounts are seeded idempotently and production can disable demo seeding.

Core data consists of Employee (unique email, role, active state and profile fields) and Task (title, description, employee foreign key, priority, status, start date, deadline and audit timestamps) with indexes and relational constraints. DTO validation and services enforce active-assignee, valid enum, deadline ordering, ownership and role rules. Dashboard and reports are aggregate database/API responses, never frontend constants.

## Project structure

- `frontend/`: Vite/React app; `src/components/` reusable navigation, badges, dialogs, forms and data display; `src/pages/` login, dashboards, employees, all tasks, my tasks, detail and reports; `src/layouts/` authenticated shell; `src/services/` Axios API modules; `src/context/` auth/notifications; `src/hooks/` shared query state; `public/manus-routes.json` route manifest.
- `backend/`: Maven Spring Boot module; `src/main/java/.../entity`, `repository`, `service`, `controller`, `dto`, `security`, `exception`, and `config`; `src/main/resources/` application settings and migration/seed assets.
- `database/`: MySQL schema/indexes and development seed reference.
- `docs/`: API reference, ER diagram and system architecture diagram.
- Root: `pom.xml` (or wrapper module), Maven Wrapper, `package.json` and lockfile/workspace policy, Dockerfile, `.env.example`, `README.md`, `plan.md`, and `TODO.md`.

## Interaction and information architecture

The manager workspace uses a persistent, collapsible dark-navy rail with a compact brand mark and role-aware links, a restrained white top bar for page title/search/profile, then asymmetric work-focused content: a thin metric band, prioritized deadline queue, recent task table, and employee progress. Tasks open a detail route or modal with clear owner/status controls. Employee views omit manager-only navigation and present a personal task queue with search, status/priority filters, deadline ordering and status updates. Employee and task forms use inline validation; destructive actions require confirmation; mutations show clear success/error feedback and refresh affected data. Loading, empty and error states are designed, not placeholders. Responsive layouts collapse navigation and reflow tables/actions for tablet and mobile.

## Visual design system

- **Design movement:** Modern editorial enterprise SaaS: Linear-like operational restraint, but with a warmer workspace canvas and original TaskFlow identity.
- **Core principles:** Make work status legible at a glance; favor compact hierarchy over card repetition; use color only for actions and operational meaning; keep user-controlled edits close to their data.
- **Color philosophy:** Deep ink navy anchors navigation and trust; a light mineral-white canvas keeps long queues readable; slate provides quiet secondary hierarchy; a distinct cool teal marks primary actions and focus; green/amber/red are reserved for completion, caution and overdue/high-risk states. No broad gradients.
- **Layout paradigm:** Persistent left rail plus a wide editorial workbench. Metrics sit in a single horizontal strip above a denser task queue; the right edge can carry contextual deadlines/progress without turning every section into an equal card grid.
- **Signature elements:** A custom TaskFlow mark made from three staggered task bars forming a forward path; narrow teal selection indicator at active navigation; compact monospaced date/ID labels paired with precise status pills.
- **Interaction philosophy:** Direct manipulation with explicit feedback. Keep controls quiet until useful; use keyboard-friendly dialogs and visible focus; confirm irreversible deletion; avoid motion that delays task work.
- **Animation:** Short 120–180 ms opacity/color transitions for navigation, menu and dialog; subtle progress-bar fill when refreshed; respect reduced-motion and avoid entrance choreography or looping animation.
- **Typography system:** Inter (or a close system sans fallback) for interface text with 12–14 px dense table labels, 15–16 px body, 20–24 px section titles and restrained 28 px page title. Use IBM Plex Mono/system monospace only for compact IDs and dates. Maintain clear weights and line-height.
- **Brand essence:** Task coordination for teams that need a reliable view of ownership and deadlines without project-management clutter; precise, calm, dependable.
- **Brand voice:** Short, direct, work-centered. Example: “Work, clearly assigned.” and “Nothing slips past the deadline.” Buttons use specific verbs such as “Create task” and “Save changes.”
- **Wordmark & logo:** Original TaskFlow logotype paired with the three staggered-bar path mark, not a default text treatment alone.
- **Signature brand color:** TaskFlow teal `#1D9A8A`.

## Dependencies, runtime and limits

Use Axios and React Router on the client; Spring MVC, Spring Security, Spring Data JPA, Hibernate, MySQL Connector/J, validation and a reviewed secure session mechanism on the server. Read managed `DATABASE_URL` and any signing/session secrets at runtime; document safe local environment variables in `.env.example` and never commit real secrets. Use Node/pnpm with a pinned package manager and reviewed build-script permissions. Configure Java compilation through Maven Wrapper and validate with Maven's compiler/test lifecycle because sandbox diagnostics do not provide a Java language server. Keep the route manifest synchronized and verify it is served as JSON, not SPA fallback. The user did not request public publication; build and Preview the site, but do not enable auto-publication or claim a public URL unless publication is separately requested and confirmed.
