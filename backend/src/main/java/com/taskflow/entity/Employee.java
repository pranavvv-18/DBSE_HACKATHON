package com.taskflow.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="employees", indexes=@Index(name="idx_employee_active", columnList="active"))
public class Employee {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(name="full_name", nullable=false, length=160) private String fullName;
  @Column(nullable=false, unique=true, length=254) private String email;
  @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
  @Column(nullable=false, length=120) private String department;
  @Column(nullable=false, length=120) private String designation;
  @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Role role;
  @Column(nullable=false) private boolean active=true;
  @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
  @PrePersist void created() { if (createdAt==null) createdAt=Instant.now(); }
  protected Employee() {}
  public Employee(String fullName,String email,String passwordHash,String department,String designation,Role role,boolean active) { this.fullName=fullName;this.email=email;this.passwordHash=passwordHash;this.department=department;this.designation=designation;this.role=role;this.active=active; }
  public Long getId(){return id;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
  public String getEmail(){return email;} public void setEmail(String v){email=v;}
  public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
  public String getDepartment(){return department;} public void setDepartment(String v){department=v;}
  public String getDesignation(){return designation;} public void setDesignation(String v){designation=v;}
  public Role getRole(){return role;} public void setRole(Role v){role=v;}
  public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
  public Instant getCreatedAt(){return createdAt;}
}
