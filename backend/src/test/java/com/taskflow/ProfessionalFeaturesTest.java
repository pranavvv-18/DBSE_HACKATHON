package com.taskflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.ApiDtos.StatusUpdate;
import com.taskflow.dto.ApiDtos.TaskRequest;
import com.taskflow.entity.Employee;
import com.taskflow.entity.Priority;
import com.taskflow.entity.Role;
import com.taskflow.entity.TaskStatus;
import com.taskflow.repository.EmployeeRepository;
import com.taskflow.repository.TaskActivityRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfessionalFeaturesTest {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;
    @Autowired TaskRepository tasks;
    @Autowired TaskActivityRepository activities;
    @Autowired UserSessionRepository sessions;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    private Employee manager;
    private Employee employee;

    @BeforeEach
    void setUp() {
        activities.deleteAll();
        sessions.deleteAll();
        tasks.deleteAll();
        employees.deleteAll();

        manager = employees.save(new Employee("Morgan Lee", "manager@test.local", encoder.encode("password123"),
                "Operations", "Manager", Role.MANAGER, true));
        employee = employees.save(new Employee("Avery Chen", "avery@test.local", encoder.encode("password123"),
                "Engineering", "Developer", Role.EMPLOYEE, true));
    }

    private Cookie login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new com.taskflow.dto.ApiDtos.LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("taskflow_session");
    }

    @Test
    void testTaskActivityRecordedOnCreationAndStatusChange() throws Exception {
        Cookie managerCookie = login("manager@test.local", "password123");

        // 1. Create a task as manager
        TaskRequest createReq = new TaskRequest("Build Kanban Feature", "Implement Kanban drag-and-drop",
                employee.getId(), Priority.HIGH, TaskStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(5));

        String response = mvc.perform(post("/api/tasks")
                        .cookie(managerCookie)
                        .header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        Long taskId = json.readTree(response).get("id").asLong();

        // Verify activities for task creation & assignment
        mvc.perform(get("/api/tasks/" + taskId + "/activities")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[?(@.action == 'Task created')]").exists())
                .andExpect(jsonPath("$[?(@.action == 'Task assigned')]").exists());

        // 2. Change status as employee
        Cookie employeeCookie = login("avery@test.local", "password123");

        mvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .cookie(employeeCookie)
                        .header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new StatusUpdate(TaskStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Verify status change activity recorded
        mvc.perform(get("/api/tasks/" + taskId + "/activities")
                        .cookie(employeeCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].action").value("Status changed"))
                .andExpect(jsonPath("$[0].previousValue").value("PENDING"))
                .andExpect(jsonPath("$[0].newValue").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].performedBy.id").value(employee.getId()));
    }

    @Test
    void testEmployeeWorkloadReport() throws Exception {
        Cookie managerCookie = login("manager@test.local", "password123");

        // Create tasks for employee
        TaskRequest req1 = new TaskRequest("Task 1", "Desc 1", employee.getId(), Priority.HIGH,
                TaskStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(2));
        TaskRequest req2 = new TaskRequest("Task 2", "Desc 2", employee.getId(), Priority.MEDIUM,
                TaskStatus.IN_PROGRESS, LocalDate.now(), LocalDate.now().plusDays(3));

        mvc.perform(post("/api/tasks").cookie(managerCookie).header("X-TaskFlow-Request", "1")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req1))).andExpect(status().isCreated());
        mvc.perform(post("/api/tasks").cookie(managerCookie).header("X-TaskFlow-Request", "1")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req2))).andExpect(status().isCreated());

        mvc.perform(get("/api/reports/employee-workload").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value(employee.getId()))
                .andExpect(jsonPath("$[0].totalTasks").value(2))
                .andExpect(jsonPath("$[0].pendingTasks").value(1))
                .andExpect(jsonPath("$[0].inProgressTasks").value(1))
                .andExpect(jsonPath("$[0].completedTasks").value(0))
                .andExpect(jsonPath("$[0].workloadLevel").value("MODERATE"));
    }

    @Test
    void testTasksCsvExport() throws Exception {
        Cookie managerCookie = login("manager@test.local", "password123");

        TaskRequest req = new TaskRequest("CSV Test Task", "Export test", employee.getId(), Priority.HIGH,
                TaskStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(4));
        mvc.perform(post("/api/tasks").cookie(managerCookie).header("X-TaskFlow-Request", "1")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req))).andExpect(status().isCreated());

        mvc.perform(get("/api/reports/tasks/csv").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"taskflow-tasks-report.csv\"")))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().string(containsString("CSV Test Task")))
                .andExpect(content().string(containsString("Avery Chen")));
    }
}

