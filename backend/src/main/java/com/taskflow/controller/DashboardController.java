package com.taskflow.controller;

import com.taskflow.dto.ApiDtos.DashboardStats;
import com.taskflow.dto.ApiDtos.EmployeeProgress;
import com.taskflow.dto.ApiDtos.PageView;
import com.taskflow.dto.ApiDtos.TaskView;
import com.taskflow.entity.Employee;
import com.taskflow.entity.Role;
import com.taskflow.security.SessionAuthFilter;
import com.taskflow.service.DashboardService;
import com.taskflow.service.TaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    private final TaskService taskService;

    public DashboardController(DashboardService dashboard, TaskService taskService) {
        this.dashboard = dashboard;
        this.taskService = taskService;
    }

    @GetMapping("/stats")
    public DashboardStats stats() {
        Employee viewer = SessionAuthFilter.current();
        return dashboard.stats(viewer, viewer.getRole() == Role.MANAGER);
    }

    @GetMapping("/recent-tasks")
    public PageView<TaskView> recent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Employee viewer = SessionAuthFilter.current();
        return taskService.list(viewer, viewer.getRole() == Role.MANAGER,
                null, null, null, null, null, "updatedAt", "desc", page, size);
    }

    @GetMapping("/employee-progress")
    @PreAuthorize("hasRole('MANAGER')")
    public List<EmployeeProgress> progress() {
        return dashboard.progress();
    }
}
