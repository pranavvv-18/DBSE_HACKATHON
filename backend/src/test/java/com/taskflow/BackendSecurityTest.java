package com.taskflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.entity.Employee;
import com.taskflow.entity.Role;
import com.taskflow.repository.EmployeeRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;
    @Autowired UserSessionRepository sessions;
    @Autowired TaskRepository tasks;
    @Autowired com.taskflow.repository.TaskActivityRepository activities;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    @BeforeEach
    void clean() {
        activities.deleteAll();
        sessions.deleteAll();
        tasks.deleteAll();
        employees.deleteAll();
        employees.save(new Employee("Test Manager", "manager@test.local", encoder.encode("test-password-123"),
                "Ops", "Manager", Role.MANAGER, true));
    }

    @Test
    void healthIsPublicAndManagerApisRequireLogin() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginIssuesOpaqueSessionAndRequiresRequestHeaderForWrites() throws Exception {
        Cookie cookie = login("manager@test.local", "test-password-123");
        mvc.perform(post("/api/employees").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("a@test.local")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/employees").cookie(cookie).header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson("a@test.local")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void employeesOnlySeeTheirTasksAndStatusUpdatesFeedReports() throws Exception {
        Cookie manager = login("manager@test.local", "test-password-123");
        long firstEmployee = createEmployee(manager, "staff@test.local");
        long firstTask = createTask(manager, firstEmployee, "Review release notes");
        long secondEmployee = createEmployee(manager, "second@test.local");
        createTask(manager, secondEmployee, "Prepare quarterly plan");

        mvc.perform(get("/api/dashboard/stats").cookie(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(2))
                .andExpect(jsonPath("$.totalEmployees").value(2));
        mvc.perform(get("/api/reports/employee-performance").cookie(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalTasks").value(1));

        Cookie employee = login("staff@test.local", "another-password");
        mvc.perform(get("/api/tasks").cookie(employee).param("employeeId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstTask));
        mvc.perform(get("/api/dashboard/stats").cookie(employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.totalEmployees").value(0));
        mvc.perform(get("/api/dashboard/recent-tasks").cookie(employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstTask));
        mvc.perform(get("/api/dashboard/employee-progress").cookie(employee))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/employees").cookie(employee)).andExpect(status().isForbidden());

        mvc.perform(patch("/api/tasks/" + firstTask + "/status").cookie(employee)
                        .header("X-TaskFlow-Request", "1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/reports/task-summary").cookie(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedPercentage").value(50.0))
                .andExpect(jsonPath("$.completedTasks").value(1));
    }

    @Test
    void managerCanFilterAndDeleteTasksWhileEmployeesCannotEditTaskFields() throws Exception {
        Cookie manager = login("manager@test.local", "test-password-123");
        long employeeId = createEmployee(manager, "filter@test.local");
        long taskId = createTask(manager, employeeId, "Searchable launch plan");
        createTask(manager, employeeId, "Separate team notes");

        mvc.perform(get("/api/tasks").cookie(manager)
                        .param("search", "Searchable")
                        .param("priority", "HIGH")
                        .param("status", "PENDING")
                        .param("employeeId", String.valueOf(employeeId))
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(taskId));

        Cookie employee = login("filter@test.local", "another-password");
        mvc.perform(put("/api/tasks/" + taskId).cookie(employee).header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson(employeeId, "Changed by employee")))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/tasks/" + taskId).cookie(manager).header("X-TaskFlow-Request", "1"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tasks/" + taskId).cookie(manager)).andExpect(status().isNotFound());
    }

    @Test
    void managerCanEditDeactivateAndReactivateEmployeesAndInactiveEmployeesCannotBeAssigned() throws Exception {
        Cookie manager = login("manager@test.local", "test-password-123");
        long employeeId = createEmployee(manager, "directory@test.local");

        mvc.perform(put("/api/employees/" + employeeId).cookie(manager).header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeUpdateJson(employeeId, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/employees").cookie(manager).param("search", "directory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].active").value(false));
        mvc.perform(post("/api/tasks").cookie(manager).header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson(employeeId, "Should not assign")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/employees/" + employeeId).cookie(manager).header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeUpdateJson(employeeId, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        mvc.perform(delete("/api/employees/" + employeeId).cookie(manager).header("X-TaskFlow-Request", "1"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/" + employeeId).cookie(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    private long createEmployee(Cookie manager, String email) throws Exception {
        String response = mvc.perform(post("/api/employees").cookie(manager).header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = json.readTree(response);
        return body.get("id").asLong();
    }

    private long createTask(Cookie manager, long employeeId, String title) throws Exception {
        String response = mvc.perform(post("/api/tasks").cookie(manager).header("X-TaskFlow-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(taskJson(employeeId, title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private String taskJson(long employeeId, String title) {
        LocalDate start = LocalDate.now();
        LocalDate deadline = start.plusDays(7);
        return "{\"title\":\"" + title + "\",\"description\":\"Check the final details\","
                + "\"assignedEmployeeId\":" + employeeId + ",\"priority\":\"HIGH\","
                + "\"status\":\"PENDING\",\"startDate\":\"" + start + "\",\"deadline\":\"" + deadline + "\"}";
    }

    private Cookie login(String email, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("taskflow_session");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        return cookie;
    }

    private String employeeJson(String email) {
        return "{\"fullName\":\"A Test\",\"email\":\"" + email + "\","
                + "\"password\":\"another-password\",\"department\":\"Engineering\",\"designation\":\"Engineer\"}";
    }

    private String employeeUpdateJson(long employeeId, boolean active) {
        return "{\"fullName\":\"Updated Test " + employeeId + "\",\"email\":\"directory@test.local\","
                + "\"department\":\"Design\",\"designation\":\"Researcher\",\"active\":" + active + "}";
    }
}
