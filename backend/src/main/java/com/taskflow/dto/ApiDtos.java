package com.taskflow.dto;

import com.taskflow.entity.*;import jakarta.validation.constraints.*;import java.time.*;import java.util.*;
public final class ApiDtos {private ApiDtos(){}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
 public record AuthUser(Long id,String fullName,String email,Role role,String department,String designation,boolean active){}
 public record EmployeeCreate(@NotBlank @Size(max=160) String fullName,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=100) String password,@NotBlank @Size(max=120) String department,@NotBlank @Size(max=120) String designation){}
 public record EmployeeUpdate(@NotBlank @Size(max=160) String fullName,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=120) String department,@NotBlank @Size(max=120) String designation,boolean active,@Size(max=100) @Pattern(regexp="^$|(?s)^.{8,100}$") String password){}
 public record EmployeeView(Long id,String fullName,String email,String department,String designation,Role role,boolean active,Instant createdAt){}
 public record Assignee(Long id,String fullName,String email,String department,String designation,boolean active){}
 public record TaskRequest(@NotBlank @Size(max=200) String title,@Size(max=10000) String description,@NotNull Long assignedEmployeeId,@NotNull Priority priority,@NotNull TaskStatus status,@NotNull LocalDate startDate,@NotNull LocalDate deadline){}
 public record StatusUpdate(@NotNull TaskStatus status){}
 public record TaskView(Long id,String title,String description,Assignee assignedTo,Priority priority,TaskStatus status,LocalDate startDate,LocalDate deadline,Instant createdAt,Instant updatedAt,boolean overdue){}
 public record PageView<T>(List<T> content,long totalElements,int totalPages,int page,int size){}
 public record StatusCount(TaskStatus status,long count){}
 public record DashboardStats(long totalTasks,long pendingTasks,long inProgressTasks,long completedTasks,long overdueTasks,long totalEmployees,List<StatusCount> statusDistribution){}
 public record EmployeeProgress(Long employeeId,String fullName,String department,long totalTasks,long completedTasks,double completionRate){}
 public record TaskSummary(long totalTasks,double completedPercentage,long pendingTasks,long inProgressTasks,long completedTasks,long overdueTasks){}
 public record TaskActivityView(Long id,Long taskId,String taskTitle,Assignee performedBy,String action,String previousValue,String newValue,Instant timestamp){}
 public record EmployeeWorkload(Long employeeId,String fullName,String email,String department,String designation,long totalTasks,long pendingTasks,long inProgressTasks,long completedTasks,long overdueTasks,String workloadLevel){}
 public record ApiError(String timestamp,int status,String error,String message,String path,Map<String,String> details){}
}
