package com.taskflow.repository;

import com.taskflow.entity.Employee;
import com.taskflow.entity.Role;
import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    long countByStatus(TaskStatus status);

    long countByDeadlineBeforeAndStatusNot(LocalDate date, TaskStatus status);

    long countByAssignedTo_Id(Long employeeId);

    long countByAssignedTo_IdAndStatus(Long employeeId, TaskStatus status);

    long countByAssignedTo_IdAndDeadlineBeforeAndStatusNot(Long employeeId, LocalDate date, TaskStatus status);

    @Query("select e.id, e.fullName, e.department, count(t), coalesce(sum(case when t.status=com.taskflow.entity.TaskStatus.COMPLETED then 1 else 0 end),0) from Employee e left join Task t on t.assignedTo=e where e.active=true and e.role=com.taskflow.entity.Role.EMPLOYEE group by e.id,e.fullName,e.department order by e.fullName")
    List<Object[]> employeeProgress();
}
