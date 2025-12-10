package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserPreferences entity operations.
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
    
    /**
     * Find user preferences by user ID.
     * 
     * @param userId the user's UUID
     * @return optional user preferences
     */
    Optional<UserPreferences> findByUserId(UUID userId);
    
    /**
     * Check if preferences exist for a user.
     * 
     * @param userId the user's UUID
     * @return true if preferences exist, false otherwise
     */
    boolean existsByUserId(UUID userId);
}
