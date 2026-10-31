package com.taskflow.repository;
import com.taskflow.entity.UserSession;import org.springframework.data.jpa.repository.*;import java.time.Instant;import java.util.*;
public interface UserSessionRepository extends JpaRepository<UserSession,Long>{Optional<UserSession> findByTokenHashAndExpiresAtAfter(String tokenHash,Instant now);void deleteByTokenHash(String hash);}
