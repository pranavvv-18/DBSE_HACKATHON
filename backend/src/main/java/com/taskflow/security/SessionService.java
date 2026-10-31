package com.taskflow.security;
import com.taskflow.entity.*;import com.taskflow.repository.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.nio.charset.StandardCharsets;import java.security.*;import java.time.*;import java.util.*;
@Service public class SessionService {
 private final EmployeeRepository employees;private final UserSessionRepository sessions;private final PasswordEncoder encoder;private final int days;
 public SessionService(EmployeeRepository e,UserSessionRepository s,PasswordEncoder p,@Value("${taskflow.session.days:7}") int days){employees=e;sessions=s;encoder=p;this.days=days;}
 @Transactional public Issued login(String email,String password){Employee e=employees.findByEmailIgnoreCase(email.trim()).filter(Employee::isActive).orElseThrow(()->new org.springframework.security.authentication.BadCredentialsException("Invalid email or password"));if(!encoder.matches(password,e.getPasswordHash()))throw new org.springframework.security.authentication.BadCredentialsException("Invalid email or password");byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);sessions.save(new UserSession(hash(token),e,Instant.now().plus(days,java.time.temporal.ChronoUnit.DAYS)));return new Issued(token,e);}
 @Transactional(readOnly=true) public Optional<Employee> resolve(String token){if(token==null||token.isBlank())return Optional.empty();return sessions.findByTokenHashAndExpiresAtAfter(hash(token),Instant.now()).map(UserSession::getEmployee).filter(Employee::isActive);}
 @Transactional public void revoke(String token){if(token!=null&&!token.isBlank())sessions.deleteByTokenHash(hash(token));}
 private String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
 public record Issued(String token,Employee employee){}
}
