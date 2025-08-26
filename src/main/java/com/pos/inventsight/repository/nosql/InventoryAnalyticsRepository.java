package com.pos.inventsight.repository.nosql;

import com.pos.inventsight.model.nosql.InventoryAnalytics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryAnalyticsRepository extends MongoRepository<InventoryAnalytics, String> {
    
    Optional<InventoryAnalytics> findByDateAndPeriod(LocalDate date, String period);
    
    List<InventoryAnalytics> findByPeriodOrderByDateDesc(String period);
    
    @Query("{ 'date' : { $gte: ?0, $lte: ?1 }, 'period': ?2 }")
    List<InventoryAnalytics> findByDateRangeAndPeriod(LocalDate startDate, LocalDate endDate, String period);
    
    List<InventoryAnalytics> findTop10ByPeriodOrderByTotalRevenueDesc(String period);
    
    @Query("{ 'period': 'DAILY', 'date': { $gte: ?0 } }")
    List<InventoryAnalytics> findDailyAnalyticsSince(LocalDate since);
    
    @Query("{ 'createdBy': ?0 }")
    List<InventoryAnalytics> findByCreatedBy(String createdBy);
}