package com.taskflow.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="user_sessions",indexes={@Index(name="idx_sessions_expiry",columnList="expires_at"),@Index(name="idx_sessions_employee",columnList="employee_id")})
public class UserSession {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id",nullable=false) private Employee employee;
 @Column(name="expires_at",nullable=false) private Instant expiresAt;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void created(){if(createdAt==null)createdAt=Instant.now();}
 protected UserSession(){}
 public UserSession(String hash,Employee employee,Instant expiresAt){this.tokenHash=hash;this.employee=employee;this.expiresAt=expiresAt;}
 public Long getId(){return id;}public String getTokenHash(){return tokenHash;}public Employee getEmployee(){return employee;}public Instant getExpiresAt(){return expiresAt;}
}
