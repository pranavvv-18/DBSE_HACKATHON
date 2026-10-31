package com.taskflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/dashboard", "/employees", "/tasks", "/tasks/{id}", "/my-tasks", "/reports", "/settings", "/kanban", "/calendar"})
    public String spa() {
        return "forward:/index.html";
    }
}
