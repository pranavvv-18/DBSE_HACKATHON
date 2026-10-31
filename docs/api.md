# TaskFlow REST API

Base path: `/api`. JSON request/response bodies use `camelCase`; dates use `YYYY-MM-DD`; timestamps are ISO-8601. The browser sends same-origin requests with credentials. Login issues an HttpOnly `taskflow_session` cookie; logout revokes it. Responses never include password hashes or session tokens. Task enums are `PENDING`, `IN_PROGRESS`, `COMPLETED`; priorities are `LOW`, `MEDIUM`, `HIGH`; roles are `MANAGER`, `EMPLOYEE`.

## Authentication

| Method and path | Access | Purpose |
|---|---|---|
| `POST /api/auth/login` | Public | Body `{ "email": "...", "password": "..." }`; verifies BCrypt and starts a session; returns the authenticated employee. |
| `GET /api/auth/me` | Authenticated | Returns the current user `{id,fullName,email,role,department,designation,active}`. |
| `POST /api/auth/logout` | Authenticated | Revokes the current session and clears the cookie; returns `204`. |

## Employees (manager only)

| Method and path | Purpose |
|---|---|
| `GET /api/employees?search=&page=0&size=25` | Search name/email/department; returns `{content,totalElements,totalPages,page,size}`. |
| `GET /api/employees/{id}` | Read employee details without credentials. |
| `POST /api/employees` | Create an active employee. Body `{fullName,email,password,department,designation}`. Role is assigned as `EMPLOYEE`. |
| `PUT /api/employees/{id}` | Update profile/account state. Body `{fullName,email,department,designation,active,password?}`. Omit `password` to retain it. |
| `DELETE /api/employees/{id}` | Deactivate rather than hard-delete, preserving task history and preventing future assignment; returns `204`. |

## Tasks

| Method and path | Access | Purpose |
|---|---|---|
| `GET /api/tasks` | Authenticated | Returns a page `{content,totalElements,totalPages,page,size}`. Filters: `search`, `status`, `priority`, manager-only `employeeId`, `overdue`, `sortBy=deadline|createdAt|updatedAt`, `sortDirection=asc|desc`, `page`, `size`. Employee results are always scoped to the signed-in employee. |
| `GET /api/tasks/{id}` | Authenticated | Returns task details; employees receive only their own tasks. |
| `POST /api/tasks` | Manager | Creates a task. Body `{title,description,assignedEmployeeId,priority,status,startDate,deadline}`. |
| `PUT /api/tasks/{id}` | Manager | Updates the task fields above. |
| `PATCH /api/tasks/{id}/status` | Manager or assigned employee | Body `{status}`. Employees can modify only status and only for their own assigned task. Automatically logs activity. |
| `GET /api/tasks/{id}/activities` | Authenticated | Returns chronological audit trail of all task activity: `{id, taskId, taskTitle, performedBy, action, previousValue, newValue, timestamp}`. Scoped to employee ownership. |
| `DELETE /api/tasks/{id}` | Manager | Deletes a task; returns `204`. Cascades activity history. |

Task responses contain `{id,title,description,assignedTo:{id,fullName,email,department,designation,active},priority,status,startDate,deadline,createdAt,updatedAt,overdue}`. `overdue` is true only when the deadline is before the current date and the task is not completed. Start date cannot be later than deadline. Only active employees can be newly assigned.

## Dashboard and reports

| Method and path | Access | Purpose |
|---|---|---|
| `GET /api/dashboard/stats` | Authenticated | Actual database counts: `{totalTasks,pendingTasks,inProgressTasks,completedTasks,overdueTasks,totalEmployees,statusDistribution:[{status,count}]}`. Employee counts cover the signed-in employee; only a manager receives the organization employee count. |
| `GET /api/dashboard/recent-tasks` | Authenticated | Most recently updated tasks, respecting employee ownership scope; page response. |
| `GET /api/dashboard/employee-progress` | Manager | Employee rows `{employeeId,fullName,department,totalTasks,completedTasks,completionRate}`. |
| `GET /api/reports/task-summary` | Manager | `{totalTasks,completedPercentage,pendingTasks,inProgressTasks,completedTasks,overdueTasks}` derived from persisted records. |
| `GET /api/reports/employee-performance` | Manager | Employee completion rows as above. |
| `GET /api/reports/employee-workload` | Manager | Capacity analysis: list of `{employeeId, fullName, email, department, designation, totalTasks, pendingTasks, inProgressTasks, completedTasks, overdueTasks, workloadLevel}`. `workloadLevel` is `HIGH` (>5 active tasks or overdue), `MODERATE` (3-5), or `LOW` (<=2). |
| `GET /api/reports/tasks/csv` | Manager | Streams RFC 4180 CSV with UTF-8 BOM containing `Task ID`, `Task Title`, `Assigned Employee`, `Department`, `Priority`, `Status`, `Start Date`, `Deadline`, `Created At`, `Overdue`. |
| `GET /api/health` | Public | Unauthenticated `200` readiness response for deployment. |

## Errors and safeguards

Validation failures return `400` with a structured error body. Missing/unauthorized records return `404` or `403` as appropriate; unauthenticated calls return `401`; duplicate email returns `409`. Cookie-authenticated state-changing calls require `X-TaskFlow-Request: 1`. CORS uses explicit origins with credentials rather than a wildcard. The API enforces role/ownership checks independently of frontend navigation.
