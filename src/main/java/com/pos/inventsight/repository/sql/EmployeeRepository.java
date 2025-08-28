package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Employee;
import com.pos.inventsight.model.sql.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    Optional<Employee> findByEmail(String email);
    
    List<Employee> findByStatus(EmployeeStatus status);
    List<Employee> findByIsCheckedInTrue();
    List<Employee> findByTitle(String title);
    List<Employee> findByDepartment(String department);
    
    @Query("SELECT e FROM Employee e WHERE e.firstName LIKE %:searchTerm% OR e.lastName LIKE %:searchTerm% OR e.email LIKE %:searchTerm%")
    List<Employee> searchEmployees(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = 'ACTIVE'")
    long countActiveEmployees();
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.isCheckedIn = true")
    long countCheckedInEmployees();
    
    @Query("SELECT DISTINCT e.title FROM Employee e WHERE e.status = 'ACTIVE'")
    List<String> findAllTitles();
    
    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.status = 'ACTIVE' AND e.department IS NOT NULL")
    List<String> findAllDepartments();
    
    @Query("SELECT e FROM Employee e WHERE e.createdBy = :createdBy AND e.status = 'ACTIVE' ORDER BY e.createdAt DESC")
    List<Employee> findByCreatedBy(@Param("createdBy") String createdBy);
    
    Optional<Employee> findByUserId(Long userId);
    
    boolean existsByEmail(String email);
}