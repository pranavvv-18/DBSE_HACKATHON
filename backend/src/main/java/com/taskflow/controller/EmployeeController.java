package com.taskflow.controller;
import com.taskflow.dto.ApiDtos.*;import com.taskflow.service.EmployeeService;import jakarta.validation.Valid;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/employees") @PreAuthorize("hasRole('MANAGER')") public class EmployeeController {
 private final EmployeeService service;public EmployeeController(EmployeeService s){service=s;}
 @GetMapping public PageView<EmployeeView> list(@RequestParam(required=false)String search,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){if(page<0||size<1||size>100)throw new com.taskflow.exception.BadRequestException("page must be >= 0 and size must be 1..100");return service.list(search,page,size);}
 @GetMapping("/{id}") public EmployeeView get(@PathVariable long id){return service.get(id);}
 @PostMapping public org.springframework.http.ResponseEntity<EmployeeView> create(@Valid @RequestBody EmployeeCreate x){EmployeeView created=service.create(x);return org.springframework.http.ResponseEntity.created(java.net.URI.create("/api/employees/"+created.id())).body(created);}
 @PutMapping("/{id}") public EmployeeView update(@PathVariable long id,@Valid @RequestBody EmployeeUpdate x){return service.update(id,x);}
 @DeleteMapping("/{id}") public org.springframework.http.ResponseEntity<Void> deactivate(@PathVariable long id){service.deactivate(id);return org.springframework.http.ResponseEntity.noContent().build();}
}
