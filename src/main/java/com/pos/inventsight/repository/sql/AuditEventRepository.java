package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    
    Page<AuditEvent> findByTenantIdOrderByEventAtDesc(UUID tenantId, Pageable pageable);
    
    Page<AuditEvent> findByCompanyIdOrderByEventAtDesc(UUID companyId, Pageable pageable);
    
    Page<AuditEvent> findByActorOrderByEventAtDesc(String actor, Pageable pageable);
    
    Page<AuditEvent> findByActionOrderByEventAtDesc(String action, Pageable pageable);
    
    Page<AuditEvent> findByEntityTypeAndEntityIdOrderByEventAtDesc(String entityType, String entityId, Pageable pageable);
    
    @Query("SELECT e FROM AuditEvent e WHERE e.tenantId = :tenantId AND e.eventAt >= :since ORDER BY e.eventAt DESC")
    Page<AuditEvent> findByTenantIdAndEventAtAfter(UUID tenantId, LocalDateTime since, Pageable pageable);
    
    @Query("SELECT e FROM AuditEvent e ORDER BY e.createdAt DESC")
    List<AuditEvent> findLatestEvent(Pageable pageable);
}
