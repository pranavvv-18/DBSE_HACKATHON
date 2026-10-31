# TaskFlow — Employee Task Management Platform

TaskFlow is a modern, enterprise-grade employee task management system built with **React/Vite**, **Java 21/Spring Boot**, **Spring Security**, **Spring Data JPA/Hibernate**, and **MySQL 8**.

Managers allocate work, balance workloads, monitor deadlines across interactive Kanban boards and calendars, download CSV reports, and review complete task activity audit trails. Employees view and update only their assigned tasks with strict server-side role enforcement.

---

## 🚀 Key Features

### 1. Kanban Task Board (`/kanban`)
- **Three Workflow Columns**: Pending, In Progress, Completed.
- **Interactive Drag & Drop**: Move tasks smoothly across columns with optimistic UI updates and instant fallback on network failure.
- **Card Metadata**: Displays task title, priority badge, assigned employee avatar, and target deadline.
- **Overdue Indicator**: Visual highlight and red accent for overdue items.
- **Multi-Filter Bar**: Real-time search, priority filter, and employee filter.
- **Quick Action Chips**: One-click status transitions for mobile/touch screens.
- **Role-Based Access**: Managers manage all team cards; employees move only their own tasks.

### 2. Task Activity Timeline & Audit Trail (`/tasks/:id`)
- **`TaskActivity` Database Entity**: Tracks the complete audit history of each task in MySQL.
- **Tracked Events**:
  - Task Creation
  - Task Assignment / Reassignment
  - Status Transitions (Pending ➔ In Progress ➔ Completed)
  - Detail Updates (Title, Priority, Deadline modifications)
  - Task Completion
- **Stored Attributes**: `id`, `taskId`, `performedBy`, `action`, `previousValue`, `newValue`, `timestamp`.
- **Interactive UI**: Displayed as a clean vertical timeline in the task details page with before/after diff pills (`<del>old</del> → <strong>new</strong>`).

### 3. Deadline Calendar (`/calendar`)
- **Monthly Grid View**: Visual representation of tasks by deadline date.
- **Visual Status Differentiation**:
  - 🔴 **Overdue**: Red highlight and alert badge for uncompleted past-due items.
  - 🔵 **Upcoming**: Blue/teal pills showing scheduled commitments.
  - 🟢 **Completed**: Strikethrough style with green checkmark indicator.
- **Quick Navigation**: Month switcher, previous/next controls, and "Today" shortcut button.
- **Top Metrics Strip**: Monthly deadline volume, total overdue, and total completed.
- **Filter Controls**: Filter by search text, priority, status, and assignee.

### 4. Employee Workload Management (`/reports`)
- **Capacity Balancing Table**:
  - Total Assigned Tasks
  - Pending Tasks
  - In-Progress Tasks
  - Completed Tasks
  - Overdue Tasks
  - Computed **Workload Level** (`HIGH`, `MODERATE`, `LOW`)
- **High-Workload Flag**: Automatically flags employees exceeding capacity thresholds (>5 active tasks or overdue deadlines).
- **Highlight Toggle**: Instant filter checkbox to isolate high-workload and overdue team members.

### 5. CSV Report Export (`/reports`)
- **RFC 4180 Compliant**: Standards-compliant CSV stream with UTF-8 BOM for perfect rendering in Microsoft Excel, Google Sheets, and LibreOffice.
- **Columns Included**: `Task ID`, `Task Title`, `Assigned Employee`, `Department`, `Priority`, `Status`, `Start Date`, `Deadline`, `Created At`, `Overdue`.
- **Manager-Only Permission**: Protected by Spring Security and backend service authorization.

### 6. Engineering & Quality Standards
- **DTOs & Layered Architecture**: Clean separation between Controllers, Services, JPA Entities, and DTO Records.
- **Pagination & Sorting**: Spring Data `Pageable` on all listings.
- **Database Transactions**: `@Transactional` boundaries on all multi-entity persistence operations.
- **Robust Security**: BCrypt password hashing, session tokens in MySQL, `HttpOnly` cookies, CSRF request-guard headers (`X-TaskFlow-Request: 1`).
- **Automated Backend & Frontend Tests**: Full JUnit/Mockito tests covering security, task operations, workload, and CSV export; Node test suite for frontend request guards.
- **Postman Collection Included**: Complete API collection in `taskflow.postman_collection.json`.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | React 18, Vite, React Router 6, Axios, Lucide React, CSS3 Variables |
| **Backend** | Java 21 / 25, Spring Boot 3.4.1, Spring Security, Spring Data JPA / Hibernate |
| **Database** | MySQL 8.0 / InnoDB with foreign key constraints & cascading rules |
| **Containerization** | Docker, Docker Compose multi-stage build |

---

## 📋 Default Demo Credentials

When `TASKFLOW_SEED_DEMO=true` is set (default in dev), the application pre-seeds the following credentials:

| Role | Name | Email | Password |
|---|---|---|---|
| **Manager** | Morgan Lee | `manager@taskflow.demo` | `TaskFlowDemo2026!` |
| **Employee** | Avery Chen | `avery@taskflow.demo` | `TaskFlowDemo2026!` |
| **Employee** | Jordan Patel | `jordan@taskflow.demo` | `TaskFlowDemo2026!` |
| **Employee** | Sam Rivera | `sam@taskflow.demo` | `TaskFlowDemo2026!` |

---

## 💻 Local Setup & Running

### Prerequisites
- **Node.js** 20+ and **pnpm** (or `corepack enable && pnpm`)
- **Java** 21+ (`JAVA_HOME` configured)
- **MySQL 8.0** running locally on port `3306` (or Docker)

### 1. Database Setup
Create the MySQL database:
```sql
CREATE DATABASE taskflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Ensure `.env` contains your MySQL credentials:
```env
PORT=8081
DB_HOST=localhost
DB_PORT=3306
DB_NAME=taskflow
DB_USERNAME=root
DB_PASSWORD=YourPasswordHere
SPRING_PROFILES_ACTIVE=dev
TASKFLOW_COOKIE_SECURE=false
TASKFLOW_CORS_ALLOWED_ORIGINS=http://localhost:3000
TASKFLOW_SEED_DEMO=true
TASKFLOW_DEMO_PASSWORD=TaskFlowDemo2026!
```

### 3. Start the Backend
From the project root:
```bash
# On Linux/macOS:
./mvnw -f backend/pom.xml spring-boot:run

# On Windows (PowerShell):
$env:PORT = "8081"
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "taskflow"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "YourPasswordHere"
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:TASKFLOW_COOKIE_SECURE = "false"
$env:TASKFLOW_CORS_ALLOWED_ORIGINS = "http://localhost:3000"
$env:TASKFLOW_SEED_DEMO = "true"
$env:TASKFLOW_DEMO_PASSWORD = "TaskFlowDemo2026!"
java -jar backend/target/taskflow-backend-1.0.0.jar
```
Backend health check: `GET http://localhost:8081/api/health`

### 4. Start the Frontend
In another terminal:
```bash
cd frontend
pnpm install
pnpm dev
```
Open **`http://localhost:3000`** in your browser.

---

## 🧪 Testing

### Backend Unit & Integration Tests
```bash
# Run all 9 tests covering authentication, RBAC, task operations, workload calculation, and CSV generation:
mvn -f backend/pom.xml test
```

### Frontend Regression Tests
```bash
cd frontend
pnpm test:api
```

### Frontend Production Build
```bash
cd frontend
pnpm build
```

---

## 📮 Postman Collection

Import `taskflow.postman_collection.json` into Postman. It includes:
1. **Authentication**: Manager Login, Employee Login, Session check (`/api/auth/me`), Logout.
2. **Employees**: List, Create, Update, Deactivate.
3. **Tasks & Kanban**: Paged list, Details, Create task, Update status (`PATCH /status`), Edit, Delete.
4. **Task Activities**: Audit trail (`GET /api/tasks/:id/activities`).
5. **Reports & Workload**: Workload capacity analysis (`GET /api/reports/employee-workload`) and CSV export (`GET /api/reports/tasks/csv`).
6. **Dashboard**: Metrics, Recent tasks feed, Employee progress.
7. **Health**: Service health check.

*Note: Ensure `baseUrl` environment variable is set to `http://localhost:8081`.*

---

## ✅ Acceptance Criteria Verification

| Criterion | Status | Verification Method |
|---|---|---|
| 1. Manager can log in successfully | **PASSED** | Authenticated via `POST /api/auth/login` with `manager@taskflow.demo` |
| 2. Manager can create an employee | **PASSED** | `POST /api/employees` creates active employee with BCrypt password |
| 3. Manager can create and assign a task | **PASSED** | `POST /api/tasks` assigns task and logs `Task created` and `Task assigned` |
| 4. Assigned employee sees that task | **PASSED** | Employee `sam@taskflow.demo` logs in; receives their assigned tasks |
| 5. Employee updates task status | **PASSED** | `PATCH /api/tasks/:id/status` changes status and records audit event |
| 6. Manager sees updated status immediately | **PASSED** | Immediate refresh on Kanban board, Tasks list, and Calendar |
| 7. Dashboard statistics reflect actual DB records | **PASSED** | Real-time SQL aggregations via `DashboardService.stats()` |
| 8. Unauthorized users cannot access restricted data | **PASSED** | Employees receive `403 Forbidden` on `/reports/employee-workload` & `/reports/tasks/csv` |
| 9. Task activity must be recorded | **PASSED** | `TaskActivity` persists creation, assignment, status change, and completion |
| 10. Saved information retained after restart | **PASSED** | MySQL 8 InnoDB persistence with schema migration |
| 11. Important buttons and forms work | **PASSED** | Modal forms, drag-and-drop, filter bars, CSV download tested |
| 12. Complete project runs using setup instructions | **PASSED** | Both backend (8081) and frontend (3000) running and verified live |

---

## 📁 Project Structure

```text
taskflow/
├── backend/                       # Spring Boot 3.4.1 Java Application
│   ├── src/main/java/com/taskflow/
│   │   ├── config/                # SecurityConfig, DemoSeedConfig, WebConfig
│   │   ├── controller/            # Auth, Employee, Task, Dashboard, Reports, Spa
│   │   ├── dto/                   # ApiDtos records
│   │   ├── entity/                # Employee, Task, TaskActivity, UserSession
│   │   ├── repository/            # Spring Data JPA repositories
│   │   └── service/               # Business logic, workload calculation, CSV export
│   └── src/test/java/             # Security & feature unit tests
├── frontend/                      # React 18 + Vite SPA
│   ├── src/
│   │   ├── components/            # UI kit, TaskFormModal, EmployeeModal
│   │   ├── context/               # AuthContext, ToastContext
│   │   ├── layouts/               # AppLayout (Sidebar, Topbar, Footer)
│   │   ├── pages/                 # KanbanPage, CalendarPage, TaskDetailsPage, ReportsPage...
│   │   └── services/api.js        # Centralized Axios client with request guards
│   └── src/styles.css             # Unified CSS styles & animations
├── database/                      # MySQL schema DDL
├── docs/                          # API documentation, architecture & ER diagrams
├── taskflow.postman_collection.json # Postman 2.1 collection
└── README.md                      # System documentation & setup guide
```
