package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.UserActiveStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserActiveStoreRepository extends JpaRepository<UserActiveStore, UUID> {
    
    Optional<UserActiveStore> findByUserId(UUID userId);
    
    void deleteByUserId(UUID userId);
}
