package com.taskflow.security;
import com.taskflow.entity.Employee;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;import java.util.*;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.web.filter.OncePerRequestFilter;
public class SessionAuthFilter extends OncePerRequestFilter {
 private final SessionService sessions;public SessionAuthFilter(SessionService s){sessions=s;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  if("OPTIONS".equalsIgnoreCase(req.getMethod())){chain.doFilter(req,res);return;}
  String token=null;if(req.getCookies()!=null)for(Cookie c:req.getCookies())if("taskflow_session".equals(c.getName())){token=c.getValue();break;}
  if(token!=null)sessions.resolve(token).ifPresent(e->{var auth=new UsernamePasswordAuthenticationToken(e,null,List.of(new SimpleGrantedAuthority("ROLE_"+e.getRole().name())));auth.setDetails(e);SecurityContextHolder.getContext().setAuthentication(auth);});
  boolean unsafe=Set.of("POST","PUT","PATCH","DELETE").contains(req.getMethod().toUpperCase(Locale.ROOT));boolean login="/api/auth/login".equals(req.getRequestURI());
  if(unsafe&&!login&&!"1".equals(req.getHeader("X-TaskFlow-Request"))){res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Missing X-TaskFlow-Request header\"}");return;}
  chain.doFilter(req,res);
 }
 public static Employee current(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?null:(Employee)a.getDetails();}
}
