package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.UserNavigationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserNavigationPreference entity operations.
 */
@Repository
public interface UserNavigationPreferenceRepository extends JpaRepository<UserNavigationPreference, UUID> {
    
    /**
     * Find navigation preferences by user ID.
     * 
     * @param userId the user's UUID
     * @return optional user navigation preferences
     */
    Optional<UserNavigationPreference> findByUserId(UUID userId);
    
    /**
     * Check if navigation preferences exist for a user.
     * 
     * @param userId the user's UUID
     * @return true if preferences exist, false otherwise
     */
    boolean existsByUserId(UUID userId);
}
