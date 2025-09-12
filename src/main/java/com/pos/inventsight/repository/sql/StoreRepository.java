package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    
    Optional<Store> findByStoreName(String storeName);
    
    List<Store> findByIsActiveTrue();
    
    List<Store> findByCountry(String country);
    
    List<Store> findByStateAndCountry(String state, String country);
    
    @Query("SELECT s FROM Store s WHERE s.storeName LIKE %:searchTerm% OR s.city LIKE %:searchTerm% OR s.state LIKE %:searchTerm%")
    List<Store> searchStores(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(s) FROM Store s WHERE s.isActive = true")
    long countActiveStores();
    
    @Query("SELECT DISTINCT s.country FROM Store s WHERE s.isActive = true ORDER BY s.country")
    List<String> findAllCountries();
    
    @Query("SELECT DISTINCT s.state FROM Store s WHERE s.country = :country AND s.isActive = true ORDER BY s.state")
    List<String> findStatesByCountry(@Param("country") String country);
    
    @Query("SELECT s FROM Store s WHERE s.createdBy = :createdBy AND s.isActive = true ORDER BY s.createdAt DESC")
    List<Store> findByCreatedBy(@Param("createdBy") String createdBy);
    
    boolean existsByStoreName(String storeName);
    
    boolean existsByEmail(String email);
}