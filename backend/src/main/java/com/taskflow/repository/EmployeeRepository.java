package com.taskflow.repository;
import com.taskflow.entity.Employee; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface EmployeeRepository extends JpaRepository<Employee,Long>,JpaSpecificationExecutor<Employee>{
 Optional<Employee> findByEmailIgnoreCase(String email); boolean existsByEmailIgnoreCase(String email); long countByActiveTrueAndRole(com.taskflow.entity.Role role);
 @Query("select e from Employee e where e.role=com.taskflow.entity.Role.EMPLOYEE and (:q is null or lower(e.fullName) like lower(concat('%',:q,'%')) or lower(e.email) like lower(concat('%',:q,'%')) or lower(e.department) like lower(concat('%',:q,'%')) or lower(e.designation) like lower(concat('%',:q,'%')))") org.springframework.data.domain.Page<Employee> searchActive(@Param("q") String q,org.springframework.data.domain.Pageable p);
}
