package com.taskflow.controller;
import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @RequestMapping("/api/health") public class HealthController {@GetMapping public Map<String,String> health(){return Map.of("status","ok");}@GetMapping("/ready") public Map<String,String> ready(){return Map.of("status","ready");}}
