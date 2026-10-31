package com.taskflow.config;

import com.taskflow.entity.*;
import com.taskflow.repository.EmployeeRepository;
import com.taskflow.repository.TaskActivityRepository;
import com.taskflow.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Configuration
@Profile({"dev", "local"})
public class DemoSeedConfig {
    private static final Logger log = LoggerFactory.getLogger(DemoSeedConfig.class);

    @Component
    public static class DemoSeeder {
        private final EmployeeRepository employees;
        private final TaskRepository tasks;
        private final TaskActivityRepository activities;
        private final PasswordEncoder encoder;

        public DemoSeeder(EmployeeRepository employees, TaskRepository tasks,
                          TaskActivityRepository activities, PasswordEncoder encoder) {
            this.employees = employees;
            this.tasks = tasks;
            this.activities = activities;
            this.encoder = encoder;
        }

        @Transactional
        public void execute(String password) {
            if (employees.count() > 0) {
                if (activities.count() == 0 && tasks.count() > 0) {
                    employees.findByEmailIgnoreCase("manager@taskflow.demo").ifPresent(mgr -> {
                        tasks.findAll().forEach(t -> {
                            String assigneeName = t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : "Employee";
                            activities.save(new TaskActivity(t, mgr, "Task created", null, t.getStatus().name()));
                            activities.save(new TaskActivity(t, mgr, "Task assigned", null, assigneeName));
                            if (t.getStatus() == TaskStatus.IN_PROGRESS) {
                                activities.save(new TaskActivity(t, t.getAssignedTo(), "Status changed", "PENDING", "IN_PROGRESS"));
                            } else if (t.getStatus() == TaskStatus.COMPLETED) {
                                activities.save(new TaskActivity(t, t.getAssignedTo(), "Task completed", "IN_PROGRESS", "COMPLETED"));
                            }
                        });
                        log.info("TaskFlow sample task activities populated.");
                    });
                }
                log.info("TaskFlow development sample seed check completed.");
                return;
            }

            Employee manager = employees.save(new Employee("Morgan Lee", "manager@taskflow.demo", encoder.encode(password), "Operations", "Team Manager", Role.MANAGER, true));
            Employee avery = employees.save(new Employee("Avery Chen", "avery@taskflow.demo", encoder.encode(password), "Design", "Product Designer", Role.EMPLOYEE, true));
            Employee jordan = employees.save(new Employee("Jordan Patel", "jordan@taskflow.demo", encoder.encode(password), "Engineering", "Software Engineer", Role.EMPLOYEE, true));
            Employee sam = employees.save(new Employee("Sam Rivera", "sam@taskflow.demo", encoder.encode(password), "Marketing", "Campaign Lead", Role.EMPLOYEE, true));
            LocalDate today = LocalDate.now();

            Task t1 = tasks.save(new Task("Finalize Q4 launch brief", "Align launch scope and stakeholder messaging.", avery, Priority.HIGH, TaskStatus.IN_PROGRESS, today.minusDays(3), today.plusDays(2)));
            activities.save(new TaskActivity(t1, manager, "Task created", null, "PENDING"));
            activities.save(new TaskActivity(t1, manager, "Task assigned", null, avery.getFullName()));
            activities.save(new TaskActivity(t1, avery, "Status changed", "PENDING", "IN_PROGRESS"));

            Task t2 = tasks.save(new Task("Review API integration", "Validate employee and task API contracts.", jordan, Priority.HIGH, TaskStatus.PENDING, today.minusDays(1), today.plusDays(4)));
            activities.save(new TaskActivity(t2, manager, "Task created", null, "PENDING"));
            activities.save(new TaskActivity(t2, manager, "Task assigned", null, jordan.getFullName()));

            Task t3 = tasks.save(new Task("Publish campaign results", "Compile outcomes and share the campaign summary.", sam, Priority.MEDIUM, TaskStatus.COMPLETED, today.minusDays(12), today.minusDays(2)));
            activities.save(new TaskActivity(t3, manager, "Task created", null, "PENDING"));
            activities.save(new TaskActivity(t3, manager, "Task assigned", null, sam.getFullName()));
            activities.save(new TaskActivity(t3, sam, "Task completed", "IN_PROGRESS", "COMPLETED"));

            Task t4 = tasks.save(new Task("Refresh onboarding guide", "Update the team onboarding checklist.", avery, Priority.LOW, TaskStatus.PENDING, today.minusDays(7), today.minusDays(1)));
            activities.save(new TaskActivity(t4, manager, "Task created", null, "PENDING"));
            activities.save(new TaskActivity(t4, manager, "Task assigned", null, avery.getFullName()));

            Task t5 = tasks.save(new Task("Operations weekly report", "Summarize team workload and next steps.", jordan, Priority.MEDIUM, TaskStatus.IN_PROGRESS, today, today.plusDays(3)));
            activities.save(new TaskActivity(t5, manager, "Task created", null, "PENDING"));
            activities.save(new TaskActivity(t5, manager, "Task assigned", null, jordan.getFullName()));
            activities.save(new TaskActivity(t5, jordan, "Status changed", "PENDING", "IN_PROGRESS"));

            log.info("TaskFlow development sample data initialized.");
        }
    }

    @Bean
    ApplicationRunner demoSeed(
            DemoSeeder seeder,
            @Value("${taskflow.seed-demo:false}") boolean enabled,
            @Value("${TASKFLOW_DEMO_PASSWORD:}") String password) {
        return args -> {
            if (!enabled) return;
            if (password == null || password.isBlank()) {
                throw new IllegalStateException("TASKFLOW_DEMO_PASSWORD is required when TASKFLOW_SEED_DEMO=true in a dev/local profile");
            }
            seeder.execute(password);
        };
    }
}
