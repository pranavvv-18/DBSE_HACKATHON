# TaskFlow delivery tracker

## 1. Authentication, session handling and role protection — COMPLETE
- Provide a professional email/password login with Manager and Employee roles; securely log in; redirect to the role-aware dashboard; support secure logout; and protect frontend routes and backend APIs.
- Use BCrypt password hashing. Store authentication sessions securely; do not return passwords, password hashes or session tokens from APIs. For HTTPS Preview, use an HttpOnly session cookie with `SameSite=None; Secure`; keep plain-HTTP localhost handling separate.
- Employees can access only their own assigned tasks and may update only their task status. Employees cannot access manager-only APIs or change task assignments, priority, deadline or other restricted fields. Only managers can create, assign, edit or delete tasks.
- Enforce roles and task ownership on the backend, not just in the frontend. Apply input validation, exact-origin CORS, centralized meaningful exception handling, and protection against unauthorized access. Keep database credentials in environment configuration; do not expose secrets in frontend bundles or source.
- Provide demo login credentials for development only and keep demo seeding separate from production behavior.
- Evidence: live manager and employee sign-in, verified secure cookie flags, role-scoped dashboard/task queries and `403` for employee manager-analytics access; H2 authorization tests pass. A frontend Node regression test confirms all nine write calls carry `X-TaskFlow-Request: 1`, and GET requests do not.

## 2. Employee directory and account management — COMPLETE
- A manager can add, view, edit, delete or deactivate employees, search employees, and view tasks assigned to an employee. Deactivation preserves existing task history and prevents new assignments; inactive accounts remain visible and can be reactivated.
- Employee fields include employee ID, full name, unique email, password, department, designation, role, account status and created date. Validate employee inputs; do not expose stored passwords or hashes.
- Only managers can use employee-management APIs. Forms use the backend and display loading, error, success and empty states; confirm destructive actions.
- Evidence: H2 integration coverage for create, search, edit/reactivation, deactivation, retained inactive records and rejection of new assignments to inactive employees; live manager employee listing succeeds.

## 3. Task creation, assignment, task details and status — COMPLETE
- A manager can create, view, edit, delete and assign tasks to active employees; set deadlines and priority; and monitor status. A task includes task ID, title, description, assigned employee, priority, status, start date, deadline, creation date and last-updated date.
- Priorities are High, Medium and Low. Statuses are Pending, In Progress and Completed. Every task needs a valid title, valid priority/status and valid active employee assignee. A deadline cannot precede the start date.
- Employees see their assigned task descriptions, deadlines and priorities; they can update status and mark tasks complete but cannot alter assignment, priority or deadline. Completed work contributes to statistics. A task is overdue only when its deadline is before the current date and it is not completed.
- Include a Task Details view. Task deletion is confirmed; actions provide clear success/failure feedback; no visible action is left nonfunctional.
- Evidence: H2 integration coverage for task creation/assignment, ownership checks, employee status completion, manager-only update/delete, title/status/priority/employee API filtering and task removal; live seeded task records are returned by MySQL-backed APIs.

## 4. My Tasks, all-task search, filters and sorting — COMPLETE
- Employees see only their assigned tasks; they can search tasks, filter by status and priority, sort by deadline, view task details, and update status or complete work.
- Users can search task titles, filter by employee, priority, status and overdue, and sort by creation date or deadline. Use API-side filtering and pagination where practical.
- Employee queries remain server-scoped even if IDs or filters are manipulated by the client. Saved changes persist in the database and remain after refresh/restart.
- Evidence: ownership is enforced in the service/API; H2 tests manipulate `employeeId` while confirming the employee sees only owned tasks; API filter/pagination tests and live ownership checks pass.

## 5. Manager and employee dashboards — COMPLETE
- Show actual backend/database values for Total Tasks, Pending Tasks, In Progress Tasks, Completed Tasks, Overdue Tasks and Total Employees; never hardcode dashboard statistics.
- Include task-status distribution, recent tasks, upcoming deadlines, high-priority tasks and employee task-progress overview. Provide a separate manager dashboard and a personalized employee dashboard. Refresh affected data after task operations.
- Dashboard charts and figures reflect persisted task records; overdue excludes completed tasks; loading, error and empty states are meaningful. Do not substitute fake charts or sample statistics after API integration.
- Evidence: manager-wide aggregates and employee-only aggregates are computed by repository queries; live dashboard checks confirm organization and personal counts differ and match the corresponding task lists.

## 6. Reports and analytics — COMPLETE
- Reports show total tasks, completed percentage, pending count, in-progress count, overdue count and employee-wise task completion. Compute all statistics from actual database records and expose performance data through manager-authorized APIs.
- Evidence: report endpoint and status-update aggregation tested against isolated H2 records; live manager dashboard reads persisted sample records.

## 7. REST backend, MySQL persistence, schema and sample data — COMPLETE
- Use Java/Spring Boot as the primary backend: Spring Web/REST, Spring Data JPA, Hibernate, Spring Security and Maven. React communicates with Spring REST APIs; MySQL stores persistent application data. Do not use browser localStorage as database persistence, Python as the primary backend, Firebase, Supabase or another backend in place of Spring Boot/MySQL. React must not access the database directly.
- Separate entities, repositories, services, controllers, DTOs, security configuration, exception handling and validation. Return appropriate HTTP status codes and meaningful errors, and document endpoints.
- Authentication API: `POST /api/auth/login`, `GET /api/auth/me` (plus secure logout). Employee API: `POST /api/employees`, `GET /api/employees`, `GET /api/employees/{id}`, `PUT /api/employees/{id}`, `DELETE /api/employees/{id}`. Task API: `POST /api/tasks`, `GET /api/tasks`, `GET /api/tasks/{id}`, `PUT /api/tasks/{id}`, `PATCH /api/tasks/{id}/status`, `DELETE /api/tasks/{id}`. Dashboard API: `GET /api/dashboard/stats`, `GET /api/dashboard/recent-tasks`, `GET /api/dashboard/employee-progress`. Reports API: `GET /api/reports/task-summary`, `GET /api/reports/employee-performance`.
- Employee relational data includes primary key ID, full name, unique email, password hash, department, designation, role, active state and created timestamp. Task relational data includes primary key ID, title, description, assigned-employee foreign key, priority, status, start date, deadline, created timestamp and updated timestamp. One employee has many tasks; each task belongs to one assigned employee. Include constraints and indexes; provide a MySQL schema reference and JPA schema generation.
- Provide development sample data and development-only demo credentials; do not overwrite existing user records when seeding.
- Evidence: Spring Boot starts against managed MySQL; sample records persist through API restart; H2 tests and JDBC URL/TLS parser regression test pass; README contains setup, run and demo instructions; SQL schema and API docs are included.

## 8. Responsive SaaS interface, required pages and project documentation — COMPLETE
- Deliver Login, Manager Dashboard, Employee Dashboard, Employee Management, Create Task page/modal, All Tasks, My Tasks, Task Details, Reports & Analytics, and Profile & Account Settings.
- Use an original, professional enterprise SaaS design inspired by Linear, Asana, ClickUp and Notion without copying them: deep navy/charcoal navigation, white/light-neutral canvas, muted slate text, restrained blue/teal accent and subtle green/amber/red for statuses/priorities; professional sans typography comparable to Inter.
- Provide a professional sidebar/top bar, aligned sections, consistent margins and spacing, readable tables/task cards, polished forms/modals/buttons and meaningful loading/empty states. Support desktop, tablet and mobile.
- Avoid excessive cards/rounding, gradients, glassmorphism, decorative illustrations, emojis, oversized headings, excessive animation, random colors, fake statistics/charts, unnecessary clutter and generic template-dashboard appearance.
- Use reusable React components, centralized Axios API client, React Router, protected routes and role-aware navigation, API-backed forms, success/failure notices, loading indicators, destructive confirmations, graceful errors, validation and responsive layouts.
- Deliver React/Vite JavaScript source, Java/Spring Boot source, MySQL configuration/schema, APIs and authorization, ER diagram, system architecture diagram, API endpoint documentation and a README with complete setup/run instructions and development demo accounts. Keep `manus-routes.json` accurate to page routes.
- Evidence: production Vite build passes; React route manifest is served as HTTP 200 valid JSON with all nine routes; project includes documentation, source diagrams/renderings and logo metadata.

## 9. Build, runtime verification and delivery — PARTIAL (core application verified; container/visual-console check not run)
- Verify the backend starts; the React frontend starts; MySQL connects; employee creation; task creation, assignment, status update and deletion; manager/employee permissions; filtering/search; dashboard statistics against database records; saved information retained after refresh; and no compilation, API-integration or major console errors. Do not claim a feature works if it could not be completed; report any remaining blocker. Prioritize essential functionality; do not add optional Python or claim unimplemented AI/ML. Keep source organized by frontend, backend, database, docs and project setup/configuration.
- Evidence: Spring Boot and Vite both start; managed MySQL health and demo sign-ins work; H2 integration tests cover employee/task CRUD, filters, status, authorization, deletion and relational business rules; the frontend mock-adapter test checks all write headers; final Maven package and Vite build pass. Container image/Compose execution and a browser-console inspection were not run in this sandbox, so those environment-level checks remain open. No public publication was requested or performed.
