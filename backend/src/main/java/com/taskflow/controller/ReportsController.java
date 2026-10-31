package com.taskflow.controller;

import com.taskflow.dto.ApiDtos.EmployeeProgress;
import com.taskflow.dto.ApiDtos.EmployeeWorkload;
import com.taskflow.dto.ApiDtos.TaskSummary;
import com.taskflow.dto.ApiDtos.TaskView;
import com.taskflow.service.DashboardService;
import com.taskflow.service.TaskService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('MANAGER')")
public class ReportsController {
    private final DashboardService dashboardService;
    private final TaskService taskService;

    public ReportsController(DashboardService dashboardService, TaskService taskService) {
        this.dashboardService = dashboardService;
        this.taskService = taskService;
    }

    @GetMapping("/task-summary")
    public TaskSummary summary() {
        return dashboardService.summary();
    }

    @GetMapping("/employee-performance")
    public List<EmployeeProgress> performance() {
        return dashboardService.progress();
    }

    @GetMapping("/employee-workload")
    public List<EmployeeWorkload> workload() {
        return dashboardService.workload();
    }

    @GetMapping(value = "/tasks/csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportTasksCsv() {
        List<TaskView> list = taskService.allForExport();

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // UTF-8 BOM for Excel compatibility
        csv.append("\"Task ID\",\"Task Title\",\"Assigned Employee\",\"Department\",\"Priority\",\"Status\",\"Start Date\",\"Deadline\",\"Created At\",\"Overdue\"\n");

        for (TaskView t : list) {
            csv.append(t.id()).append(',');
            csv.append(escapeCsv(t.title())).append(',');
            csv.append(escapeCsv(t.assignedTo() != null ? t.assignedTo().fullName() : "Unassigned")).append(',');
            csv.append(escapeCsv(t.assignedTo() != null ? t.assignedTo().department() : "")).append(',');
            csv.append(escapeCsv(t.priority().name())).append(',');
            csv.append(escapeCsv(t.status().name())).append(',');
            csv.append(escapeCsv(t.startDate().toString())).append(',');
            csv.append(escapeCsv(t.deadline().toString())).append(',');
            csv.append(escapeCsv(t.createdAt() != null ? t.createdAt().toString() : "")).append(',');
            csv.append(t.overdue() ? "\"YES\"" : "\"NO\"").append('\n');
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"taskflow-tasks-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    private static String escapeCsv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
