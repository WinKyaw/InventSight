package com.pos.inventsight.repository.nosql;

import com.pos.inventsight.model.nosql.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    
    List<ActivityLog> findByUserIdOrderByTimestampDesc(String userId);
    
    List<ActivityLog> findByActionOrderByTimestampDesc(String action);
    
    List<ActivityLog> findByEntityTypeOrderByTimestampDesc(String entityType);
    
    List<ActivityLog> findByModuleOrderByTimestampDesc(String module);
    
    @Query("{ 'timestamp' : { $gte: ?0, $lte: ?1 } }")
    List<ActivityLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'userId': ?0, 'timestamp' : { $gte: ?1, $lte: ?2 } }")
    List<ActivityLog> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);
    
    List<ActivityLog> findBySeverityOrderByTimestampDesc(String severity);
    
    @Query("{ 'username': ?0 }")
    List<ActivityLog> findByUsernameOrderByTimestampDesc(String username);
    
    long countByUserId(String userId);
    long countByUsername(String username);
}