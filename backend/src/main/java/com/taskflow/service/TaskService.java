package com.taskflow.service;

import com.taskflow.dto.ApiDtos.*;
import com.taskflow.entity.*;
import com.taskflow.exception.*;
import com.taskflow.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final EmployeeRepository employees;
    private final TaskActivityRepository activities;

    public TaskService(TaskRepository t, EmployeeRepository e, TaskActivityRepository a) {
        this.tasks = t;
        this.employees = e;
        this.activities = a;
    }

    @Transactional(readOnly = true)
    public PageView<TaskView> list(Employee viewer, boolean manager, String search, TaskStatus status,
                                   Priority priority, Long employeeId, Boolean overdue,
                                   String sortBy, String sortDirection, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("page must be >= 0 and size must be between 1 and 100");
        }
        String field = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Map<String, String> fields = Map.of(
                "createdAt", "createdAt",
                "deadline", "deadline",
                "startDate", "startDate",
                "title", "title",
                "updatedAt", "updatedAt"
        );
        if (!fields.containsKey(field)) {
            throw new BadRequestException("sortBy must be one of: createdAt, deadline, startDate, title, updatedAt");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable p = PageRequest.of(page, size, Sort.by(direction, fields.get(field)));

        Specification<Task> spec = (root, q, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((r, q, cb) -> cb.or(
                    cb.like(cb.lower(r.get("title")), pattern),
                    cb.like(cb.lower(r.get("description")), pattern)
            ));
        }
        if (status != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("priority"), priority));
        }
        Long assigned = manager ? employeeId : viewer.getId();
        if (assigned != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("assignedTo").get("id"), assigned));
        }
        if (Boolean.TRUE.equals(overdue)) {
            spec = spec.and((r, q, cb) -> cb.and(
                    cb.lessThan(r.get("deadline"), LocalDate.now()),
                    cb.notEqual(r.get("status"), TaskStatus.COMPLETED)
            ));
        } else if (Boolean.FALSE.equals(overdue)) {
            spec = spec.and((r, q, cb) -> cb.or(
                    cb.greaterThanOrEqualTo(r.get("deadline"), LocalDate.now()),
                    cb.equal(r.get("status"), TaskStatus.COMPLETED)
            ));
        }

        Page<Task> result = tasks.findAll(spec, p);
        return new PageView<>(
                result.getContent().stream().map(TaskService::view).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public TaskView get(long id, Employee viewer, boolean manager) {
        Task t = task(id);
        if (!manager && !t.getAssignedTo().getId().equals(viewer.getId())) {
            throw new AccessDeniedException("You may only access your assigned tasks");
        }
        return view(t);
    }

    @Transactional
    public TaskView create(TaskRequest x, Employee creator) {
        validateDates(x.startDate(), x.deadline());
        Employee a = assignee(x.assignedEmployeeId());
        Task saved = tasks.save(new Task(x.title().trim(), x.description(), a, x.priority(), x.status(), x.startDate(), x.deadline()));
        activities.save(new TaskActivity(saved, creator, "Task created", null, saved.getStatus().name()));
        activities.save(new TaskActivity(saved, creator, "Task assigned", null, a.getFullName()));
        return view(saved);
    }

    @Transactional
    public TaskView update(long id, TaskRequest x, Employee updater) {
        validateDates(x.startDate(), x.deadline());
        Task t = task(id);
        Employee oldAssignee = t.getAssignedTo();
        TaskStatus oldStatus = t.getStatus();
        Priority oldPriority = t.getPriority();
        LocalDate oldDeadline = t.getDeadline();
        String oldTitle = t.getTitle();

        Employee newAssignee = assignee(x.assignedEmployeeId());
        t.setTitle(x.title().trim());
        t.setDescription(x.description());
        t.setAssignedTo(newAssignee);
        t.setPriority(x.priority());
        t.setStatus(x.status());
        t.setStartDate(x.startDate());
        t.setDeadline(x.deadline());

        // Track specific changes
        if (!oldTitle.equals(t.getTitle())) {
            activities.save(new TaskActivity(t, updater, "Title updated", oldTitle, t.getTitle()));
        }
        if (!oldAssignee.getId().equals(newAssignee.getId())) {
            activities.save(new TaskActivity(t, updater, "Task reassigned", oldAssignee.getFullName(), newAssignee.getFullName()));
        }
        if (oldPriority != t.getPriority()) {
            activities.save(new TaskActivity(t, updater, "Priority updated", oldPriority.name(), t.getPriority().name()));
        }
        if (!oldDeadline.equals(t.getDeadline())) {
            activities.save(new TaskActivity(t, updater, "Deadline updated", oldDeadline.toString(), t.getDeadline().toString()));
        }
        if (oldStatus != t.getStatus()) {
            String action = (t.getStatus() == TaskStatus.COMPLETED) ? "Task completed" : "Status changed";
            activities.save(new TaskActivity(t, updater, action, oldStatus.name(), t.getStatus().name()));
        }

        return view(t);
    }

    @Transactional
    public TaskView changeStatus(long id, TaskStatus status, Employee viewer, boolean manager) {
        Task t = task(id);
        if (!manager && !t.getAssignedTo().getId().equals(viewer.getId())) {
            throw new AccessDeniedException("You may only update your assigned tasks");
        }
        TaskStatus prev = t.getStatus();
        if (prev != status) {
            t.setStatus(status);
            String action = (status == TaskStatus.COMPLETED) ? "Task completed" : "Status changed";
            activities.save(new TaskActivity(t, viewer, action, prev.name(), status.name()));
        }
        return view(t);
    }

    @Transactional
    public void delete(long id) {
        Task t = task(id);
        tasks.delete(t);
    }

    @Transactional(readOnly = true)
    public List<TaskActivityView> activities(long taskId, Employee viewer, boolean manager) {
        Task t = task(taskId);
        if (!manager && !t.getAssignedTo().getId().equals(viewer.getId())) {
            throw new AccessDeniedException("You may only view activity for your assigned tasks");
        }
        return activities.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(a -> {
            Assignee performedBy = null;
            if (a.getPerformedBy() != null) {
                Employee p = a.getPerformedBy();
                performedBy = new Assignee(p.getId(), p.getFullName(), p.getEmail(), p.getDepartment(), p.getDesignation(), p.isActive());
            }
            return new TaskActivityView(
                    a.getId(),
                    t.getId(),
                    t.getTitle(),
                    performedBy,
                    a.getAction(),
                    a.getPreviousValue(),
                    a.getNewValue(),
                    a.getCreatedAt()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskView> recent(Employee viewer, boolean manager) {
        PageView<TaskView> p = list(viewer, manager, null, null, null, null, null, "createdAt", "desc", 0, 10);
        return p.content();
    }

    @Transactional(readOnly = true)
    public List<TaskView> allForExport() {
        return tasks.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(TaskService::view).toList();
    }

    private Task task(long id) {
        return tasks.findById(id).orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private Employee assignee(Long id) {
        Employee e = employees.findById(id).orElseThrow(() -> new NotFoundException("Assigned employee not found"));
        if (!e.isActive() || e.getRole() != Role.EMPLOYEE) {
            throw new BadRequestException("Tasks can only be assigned to active employees");
        }
        return e;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BadRequestException("deadline cannot be before startDate");
        }
    }

    public static TaskView view(Task t) {
        Employee e = t.getAssignedTo();
        return new TaskView(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                new Assignee(e.getId(), e.getFullName(), e.getEmail(), e.getDepartment(), e.getDesignation(), e.isActive()),
                t.getPriority(),
                t.getStatus(),
                t.getStartDate(),
                t.getDeadline(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getDeadline().isBefore(LocalDate.now()) && t.getStatus() != TaskStatus.COMPLETED
        );
    }
}
