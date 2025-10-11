package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.SyncChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SyncChangeRepository extends JpaRepository<SyncChange, UUID> {
    
    Page<SyncChange> findByTenantIdAndChangedAtAfterOrderByChangedAtAsc(
        UUID tenantId, LocalDateTime since, Pageable pageable);
    
    Page<SyncChange> findByTenantIdOrderByChangedAtDesc(UUID tenantId, Pageable pageable);
}
