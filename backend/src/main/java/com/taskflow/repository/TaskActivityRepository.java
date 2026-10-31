package com.taskflow.repository;

import com.taskflow.entity.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
    @Query("select a from TaskActivity a left join fetch a.performedBy where a.task.id = :taskId order by a.createdAt desc")
    List<TaskActivity> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId);
}

