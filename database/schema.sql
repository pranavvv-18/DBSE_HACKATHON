-- Reference schema for MySQL 8; Hibernate manages runtime schema with ddl-auto=update.
CREATE TABLE employees (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(160) NOT NULL,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  department VARCHAR(120) NOT NULL,
  designation VARCHAR(120) NOT NULL,
  role VARCHAR(20) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT chk_employee_role CHECK (role IN ('MANAGER','EMPLOYEE'))
);
CREATE TABLE tasks (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  assigned_to BIGINT NOT NULL,
  priority VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  start_date DATE NOT NULL,
  deadline DATE NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_task_assignee FOREIGN KEY (assigned_to) REFERENCES employees(id),
  CONSTRAINT chk_task_priority CHECK (priority IN ('HIGH','MEDIUM','LOW')),
  CONSTRAINT chk_task_status CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED')),
  CONSTRAINT chk_task_dates CHECK (deadline >= start_date),
  INDEX idx_tasks_assignee (assigned_to), INDEX idx_tasks_status (status), INDEX idx_tasks_deadline (deadline)
);
CREATE TABLE user_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  token_hash CHAR(64) NOT NULL UNIQUE,
  employee_id BIGINT NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_session_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
  INDEX idx_sessions_expiry (expires_at), INDEX idx_sessions_employee (employee_id)
);
CREATE TABLE task_activities (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  performed_by_id BIGINT,
  action VARCHAR(255) NOT NULL,
  previous_value VARCHAR(255),
  new_value VARCHAR(255),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_activity_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
  CONSTRAINT fk_activity_performed_by FOREIGN KEY (performed_by_id) REFERENCES employees(id),
  INDEX idx_activity_task (task_id),
  INDEX idx_activity_created (created_at)
);
