package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUuid(String uuid);
    
    List<User> findByIsActiveTrue();
    
    List<User> findByRole(UserRole role);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);
    
    long countByIsActiveTrue();
    
    long countByRole(UserRole role);
    
    default long countActiveUsers() {
        return countByIsActiveTrue();
    }
    
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.createdAt >= :since")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);
}