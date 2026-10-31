package com.taskflow.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "task_activities", indexes = {
    @Index(name = "idx_activity_task", columnList = "task_id"),
    @Index(name = "idx_activity_created", columnList = "created_at")
})
public class TaskActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private Employee performedBy;

    @Column(nullable = false, length = 255)
    private String action;

    @Column(name = "previous_value", length = 255)
    private String previousValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TaskActivity() {}

    public TaskActivity(Task task, Employee performedBy, String action, String previousValue, String newValue) {
        this.task = task;
        this.performedBy = performedBy;
        this.action = action;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Employee getPerformedBy() { return performedBy; }
    public void setPerformedBy(Employee performedBy) { this.performedBy = performedBy; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

