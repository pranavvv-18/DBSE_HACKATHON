package com.taskflow.service;

import com.taskflow.dto.ApiDtos.DashboardStats;
import com.taskflow.dto.ApiDtos.EmployeeProgress;
import com.taskflow.dto.ApiDtos.EmployeeWorkload;
import com.taskflow.dto.ApiDtos.StatusCount;
import com.taskflow.dto.ApiDtos.TaskSummary;
import com.taskflow.entity.Employee;
import com.taskflow.entity.Role;
import com.taskflow.entity.TaskStatus;
import com.taskflow.repository.EmployeeRepository;
import com.taskflow.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {
    private final TaskRepository tasks;
    private final EmployeeRepository employees;

    public DashboardService(TaskRepository tasks, EmployeeRepository employees) {
        this.tasks = tasks;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats(Employee viewer, boolean manager) {
        Long employeeId = manager ? null : viewer.getId();
        long total = manager ? tasks.count() : tasks.countByAssignedTo_Id(employeeId);
        long pending = count(employeeId, manager, TaskStatus.PENDING);
        long inProgress = count(employeeId, manager, TaskStatus.IN_PROGRESS);
        long completed = count(employeeId, manager, TaskStatus.COMPLETED);
        long overdue = manager
                ? tasks.countByDeadlineBeforeAndStatusNot(LocalDate.now(), TaskStatus.COMPLETED)
                : tasks.countByAssignedTo_IdAndDeadlineBeforeAndStatusNot(employeeId, LocalDate.now(), TaskStatus.COMPLETED);
        List<StatusCount> distribution = Arrays.stream(TaskStatus.values())
                .map(status -> new StatusCount(status, count(employeeId, manager, status)))
                .toList();
        long employeeCount = manager ? employees.countByActiveTrueAndRole(Role.EMPLOYEE) : 0;
        return new DashboardStats(total, pending, inProgress, completed, overdue, employeeCount, distribution);
    }

    @Transactional(readOnly = true)
    public List<EmployeeProgress> progress() {
        return tasks.employeeProgress().stream().map(row -> {
            long total = ((Number) row[3]).longValue();
            long done = ((Number) row[4]).longValue();
            double completionRate = total == 0 ? 0.0 : Math.round(done * 10000.0 / total) / 100.0;
            return new EmployeeProgress((Long) row[0], (String) row[1], (String) row[2], total, done, completionRate);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeWorkload> workload() {
        LocalDate today = LocalDate.now();
        List<Employee> emps = employees.findAll().stream()
                .filter(e -> e.isActive() && e.getRole() == Role.EMPLOYEE)
                .sorted(Comparator.comparing(Employee::getFullName))
                .toList();

        return emps.stream().map(e -> {
            long total = tasks.countByAssignedTo_Id(e.getId());
            long pending = tasks.countByAssignedTo_IdAndStatus(e.getId(), TaskStatus.PENDING);
            long inProgress = tasks.countByAssignedTo_IdAndStatus(e.getId(), TaskStatus.IN_PROGRESS);
            long completed = tasks.countByAssignedTo_IdAndStatus(e.getId(), TaskStatus.COMPLETED);
            long overdue = tasks.countByAssignedTo_IdAndDeadlineBeforeAndStatusNot(e.getId(), today, TaskStatus.COMPLETED);

            long active = pending + inProgress;
            String level;
            if (active >= 4 || overdue >= 2) {
                level = "HIGH";
            } else if (active >= 2 || overdue >= 1) {
                level = "MODERATE";
            } else {
                level = "LOW";
            }

            return new EmployeeWorkload(
                    e.getId(),
                    e.getFullName(),
                    e.getEmail(),
                    e.getDepartment(),
                    e.getDesignation(),
                    total,
                    pending,
                    inProgress,
                    completed,
                    overdue,
                    level
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public TaskSummary summary() {
        DashboardStats stats = stats(null, true);
        double completedPercentage = stats.totalTasks() == 0
                ? 0.0
                : Math.round(stats.completedTasks() * 10000.0 / stats.totalTasks()) / 100.0;
        return new TaskSummary(stats.totalTasks(), completedPercentage, stats.pendingTasks(),
                stats.inProgressTasks(), stats.completedTasks(), stats.overdueTasks());
    }

    private long count(Long employeeId, boolean manager, TaskStatus status) {
        return manager ? tasks.countByStatus(status) : tasks.countByAssignedTo_IdAndStatus(employeeId, status);
    }
}
