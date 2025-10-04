package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyStoreUserRepository extends JpaRepository<CompanyStoreUser, UUID> {
    
    /**
     * Find active user-company relationships
     */
    List<CompanyStoreUser> findByUserAndIsActiveTrue(User user);
    
    /**
     * Find active users for a company
     */
    List<CompanyStoreUser> findByCompanyAndIsActiveTrue(Company company);
    
    /**
     * Find active users for a store
     */
    List<CompanyStoreUser> findByStoreAndIsActiveTrue(Store store);
    
    /**
     * Find specific user-company-store relationship
     */
    Optional<CompanyStoreUser> findByUserAndCompanyAndStoreAndIsActiveTrue(User user, Company company, Store store);
    
    /**
     * Find user-company relationship (company-level, no specific store)
     */
    Optional<CompanyStoreUser> findByUserAndCompanyAndStoreIsNullAndIsActiveTrue(User user, Company company);
    
    /**
     * Find users by role in company
     */
    List<CompanyStoreUser> findByCompanyAndRoleAndIsActiveTrue(Company company, CompanyRole role);
    
    /**
     * Find users by role in store
     */
    List<CompanyStoreUser> findByStoreAndRoleAndIsActiveTrue(Store store, CompanyRole role);
    
    /**
     * Find company founders
     */
    List<CompanyStoreUser> findByCompanyAndRoleAndStoreIsNullAndIsActiveTrue(Company company, CompanyRole role);
    
    /**
     * Check if user has access to company
     */
    boolean existsByUserAndCompanyAndIsActiveTrue(User user, Company company);
    
    /**
     * Check if user has access to store
     */
    boolean existsByUserAndStoreAndIsActiveTrue(User user, Store store);
    
    /**
     * Get user's companies
     */
    @Query("SELECT DISTINCT csu.company FROM CompanyStoreUser csu WHERE csu.user = :user AND csu.isActive = true")
    List<Company> findCompaniesByUser(@Param("user") User user);
    
    /**
     * Get user's stores within a company
     */
    @Query("SELECT csu.store FROM CompanyStoreUser csu WHERE csu.user = :user AND csu.company = :company AND csu.store IS NOT NULL AND csu.isActive = true")
    List<Store> findStoresByUserAndCompany(@Param("user") User user, @Param("company") Company company);
    
    /**
     * Get user's role in company (company-level)
     */
    @Query("SELECT csu.role FROM CompanyStoreUser csu WHERE csu.user = :user AND csu.company = :company AND csu.store IS NULL AND csu.isActive = true")
    Optional<CompanyRole> findUserRoleInCompany(@Param("user") User user, @Param("company") Company company);
    
    /**
     * Get user's role in specific store
     */
    @Query("SELECT csu.role FROM CompanyStoreUser csu WHERE csu.user = :user AND csu.store = :store AND csu.isActive = true")
    Optional<CompanyRole> findUserRoleInStore(@Param("user") User user, @Param("store") Store store);
    
    /**
     * Find all relationships for a user and company (both company-level and store-specific)
     */
    List<CompanyStoreUser> findByUserAndCompanyAndIsActiveTrue(User user, Company company);
    
    /**
     * Count active users in company
     */
    @Query("SELECT COUNT(DISTINCT csu.user) FROM CompanyStoreUser csu WHERE csu.company = :company AND csu.isActive = true")
    long countActiveUsersByCompany(@Param("company") Company company);
    
    /**
     * Count active users in store
     */
    @Query("SELECT COUNT(csu) FROM CompanyStoreUser csu WHERE csu.store = :store AND csu.isActive = true")
    long countActiveUsersByStore(@Param("store") Store store);
    
    /**
     * Find users with founder role in any company
     */
    @Query("SELECT csu FROM CompanyStoreUser csu WHERE csu.role = 'FOUNDER' AND csu.isActive = true")
    List<CompanyStoreUser> findFounders();
    
    /**
     * Check if user is founder of any company
     */
    @Query("SELECT COUNT(csu) > 0 FROM CompanyStoreUser csu WHERE csu.user = :user AND csu.role = 'FOUNDER' AND csu.isActive = true")
    boolean isFounder(@Param("user") User user);
    
    /**
     * Count active companies where user is founder
     */
    @Query("SELECT COUNT(DISTINCT csu.company) FROM CompanyStoreUser csu WHERE csu.user = :user AND csu.role = 'FOUNDER' AND csu.isActive = true")
    long countCompaniesByFounder(@Param("user") User user);
}