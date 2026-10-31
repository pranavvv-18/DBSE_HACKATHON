package com.taskflow.controller;

import com.taskflow.dto.ApiDtos.*;
import com.taskflow.entity.Employee;
import com.taskflow.entity.Priority;
import com.taskflow.entity.Role;
import com.taskflow.entity.TaskStatus;
import com.taskflow.security.SessionAuthFilter;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService s) {
        service = s;
    }

    @GetMapping
    public PageView<TaskView> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Employee u = SessionAuthFilter.current();
        boolean mgr = u.getRole() == Role.MANAGER;
        return service.list(u, mgr, search, status, priority, employeeId, overdue, sortBy, sortDirection, page, size);
    }

    @GetMapping("/{id}")
    public TaskView get(@PathVariable long id) {
        Employee u = SessionAuthFilter.current();
        return service.get(id, u, u.getRole() == Role.MANAGER);
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TaskView> create(@Valid @RequestBody TaskRequest x) {
        Employee u = SessionAuthFilter.current();
        TaskView created = service.create(x, u);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public TaskView update(@PathVariable long id, @Valid @RequestBody TaskRequest x) {
        Employee u = SessionAuthFilter.current();
        return service.update(id, x, u);
    }

    @PatchMapping("/{id}/status")
    public TaskView updateStatus(@PathVariable long id, @Valid @RequestBody StatusUpdate x) {
        Employee u = SessionAuthFilter.current();
        return service.changeStatus(id, x.status(), u, u.getRole() == Role.MANAGER);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/activities")
    public List<TaskActivityView> activities(@PathVariable long id) {
        Employee u = SessionAuthFilter.current();
        return service.activities(id, u, u.getRole() == Role.MANAGER);
    }
}
