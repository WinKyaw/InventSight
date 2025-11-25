package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Event;
import com.pos.inventsight.model.sql.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    // Find events by creator
    List<Event> findByCreatedByIdAndStatusOrderByStartDateTimeAsc(UUID userId, EventStatus status);
    
    // Find events where user is attendee or creator
    @Query("SELECT e FROM Event e WHERE (e.createdBy.id = :userId OR :userId IN (SELECT a.id FROM e.attendees a)) AND e.status = :status ORDER BY e.startDateTime ASC")
    List<Event> findEventsByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") EventStatus status);
    
    // Find events in date range
    @Query("SELECT e FROM Event e WHERE e.startDateTime >= :startDate AND e.endDateTime <= :endDate AND e.status = :status ORDER BY e.startDateTime ASC")
    List<Event> findByDateRangeAndStatus(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate, 
                                       @Param("status") EventStatus status);
    
    // Find events for specific month and year
    @Query("SELECT e FROM Event e WHERE YEAR(e.startDateTime) = :year AND MONTH(e.startDateTime) = :month AND (e.createdBy.id = :userId OR :userId IN (SELECT a.id FROM e.attendees a)) AND e.status = 'ACTIVE' ORDER BY e.startDateTime ASC")
    List<Event> findEventsByYearMonthAndUser(@Param("year") int year, 
                                           @Param("month") int month, 
                                           @Param("userId") UUID userId);
    
    // Find upcoming events for user
    @Query("SELECT e FROM Event e WHERE e.startDateTime > :now AND (e.createdBy.id = :userId OR :userId IN (SELECT a.id FROM e.attendees a)) AND e.status = 'ACTIVE' ORDER BY e.startDateTime ASC")
    List<Event> findUpcomingEventsByUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    // Find overlapping events
    @Query("SELECT e FROM Event e WHERE e.startDateTime < :endDateTime AND e.endDateTime > :startDateTime AND e.status = 'ACTIVE' AND e.id != :excludeEventId")
    List<Event> findOverlappingEvents(@Param("startDateTime") LocalDateTime startDateTime, 
                                    @Param("endDateTime") LocalDateTime endDateTime, 
                                    @Param("excludeEventId") Long excludeEventId);
    
    // Find events by type
    @Query("SELECT e FROM Event e WHERE e.type = :eventType AND (e.createdBy.id = :userId OR :userId IN (SELECT a.id FROM e.attendees a)) AND e.status = 'ACTIVE' ORDER BY e.startDateTime ASC")
    List<Event> findEventsByTypeAndUser(@Param("eventType") com.pos.inventsight.model.sql.EventType eventType, 
                                      @Param("userId") UUID userId);
    
    // Count events by user
    @Query("SELECT COUNT(e) FROM Event e WHERE (e.createdBy.id = :userId OR :userId IN (SELECT a.id FROM e.attendees a)) AND e.status = 'ACTIVE'")
    long countActiveEventsByUser(@Param("userId") UUID userId);
    
    // Find today's events - using date function to get start of day and end of day
    @Query("SELECT e FROM Event e WHERE e.startDateTime >= CURRENT_DATE AND e.startDateTime < FUNCTION('DATE_ADD', CURRENT_DATE, 1, 'DAY') AND (e.createdBy.id = :userId OR :userId IN (SELECT a.id FROM e.attendees a)) AND e.status = 'ACTIVE' ORDER BY e.startDateTime ASC")
    List<Event> findTodaysEventsByUser(@Param("userId") UUID userId);
    
    // Search events by title or description
    @Query("SELECT e FROM Event e WHERE (LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND (e.createdBy.id = :userId OR :userId IN (SELECT a.id FROM e.attendees a)) AND e.status = 'ACTIVE' ORDER BY e.startDateTime ASC")
    List<Event> searchEventsByUser(@Param("searchTerm") String searchTerm, @Param("userId") UUID userId);
    
    // Find events in date range (for calendar service)
    @Query("SELECT e FROM Event e WHERE e.startDateTime >= :startDate AND e.endDateTime <= :endDate AND e.status = 'ACTIVE' ORDER BY e.startDateTime ASC")
    List<Event> findEventsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}