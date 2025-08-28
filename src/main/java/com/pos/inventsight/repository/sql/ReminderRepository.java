package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    
    List<Reminder> findByIsActiveTrue();
    
    List<Reminder> findByIsCompletedFalseAndIsActiveTrue();
    
    List<Reminder> findByReminderDateTimeBetween(LocalDateTime start, LocalDateTime end);
    
    List<Reminder> findByReminderType(String reminderType);
    
    List<Reminder> findByPriority(String priority);
    
    @Query("SELECT r FROM Reminder r WHERE r.reminderDateTime < :now AND r.isCompleted = false AND r.isActive = true")
    List<Reminder> findPastDueReminders(@Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM Reminder r WHERE DATE(r.reminderDateTime) = CURRENT_DATE AND r.isActive = true")
    List<Reminder> findTodaysReminders();
    
    @Query("SELECT r FROM Reminder r WHERE r.reminderDateTime BETWEEN :now AND :tomorrow AND r.isCompleted = false AND r.isActive = true")
    List<Reminder> findDueSoonReminders(@Param("now") LocalDateTime now, @Param("tomorrow") LocalDateTime tomorrow);
    
    @Query("SELECT r FROM Reminder r WHERE r.createdBy = :createdBy AND r.isActive = true ORDER BY r.reminderDateTime ASC")
    List<Reminder> findByCreatedBy(@Param("createdBy") String createdBy);
    
    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.isActive = true")
    long countActiveReminders();
    
    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.isCompleted = false AND r.isActive = true")
    long countPendingReminders();
    
    @Query("SELECT r FROM Reminder r WHERE r.relatedEntityType = :entityType AND r.relatedEntityId = :entityId AND r.isActive = true")
    List<Reminder> findByRelatedEntity(@Param("entityType") String entityType, @Param("entityId") String entityId);
}