package com.taskflow.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name="tasks", indexes={@Index(name="idx_tasks_assignee",columnList="assigned_to"),@Index(name="idx_tasks_status",columnList="status"),@Index(name="idx_tasks_deadline",columnList="deadline")})
public class Task {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false,length=200) private String title;
  @Column(columnDefinition="text") private String description;
  @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="assigned_to",nullable=false,foreignKey=@ForeignKey(name="fk_task_assignee")) private Employee assignedTo;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Priority priority;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TaskStatus status;
  @Column(name="start_date",nullable=false) private LocalDate startDate;
  @Column(nullable=false) private LocalDate deadline;
  @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  @OneToMany(mappedBy="task", cascade=CascadeType.ALL, orphanRemoval=true)
  private java.util.List<TaskActivity> activities = new java.util.ArrayList<>();
  @PrePersist void created(){Instant now=Instant.now();if(createdAt==null)createdAt=now;updatedAt=now;}
  @PreUpdate void updated(){updatedAt=Instant.now();}
  protected Task(){}
  public Task(String title,String description,Employee assignedTo,Priority priority,TaskStatus status,LocalDate startDate,LocalDate deadline){this.title=title;this.description=description;this.assignedTo=assignedTo;this.priority=priority;this.status=status;this.startDate=startDate;this.deadline=deadline;}
  public Long getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
  public Employee getAssignedTo(){return assignedTo;} public void setAssignedTo(Employee v){assignedTo=v;} public Priority getPriority(){return priority;} public void setPriority(Priority v){priority=v;}
  public TaskStatus getStatus(){return status;} public void setStatus(TaskStatus v){status=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
  public LocalDate getDeadline(){return deadline;} public void setDeadline(LocalDate v){deadline=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
