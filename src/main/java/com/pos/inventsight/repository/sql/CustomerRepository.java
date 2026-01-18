package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.Customer;
import com.pos.inventsight.model.sql.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    /**
     * Find all active customers for a company with eager loading
     */
    @Query("SELECT c FROM Customer c WHERE c.company = :company AND c.isActive = true ORDER BY c.name ASC")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "store", "createdByUser"})
    Page<Customer> findByCompanyAndIsActiveTrueOrderByNameAsc(@Param("company") Company company, Pageable pageable);
    
    /**
     * Find all active customers for a company and store with eager loading
     */
    @Query("SELECT c FROM Customer c WHERE c.company = :company AND c.store = :store AND c.isActive = true ORDER BY c.name ASC")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "store", "createdByUser"})
    Page<Customer> findByCompanyAndStoreAndIsActiveTrueOrderByNameAsc(@Param("company") Company company, @Param("store") Store store, Pageable pageable);
    
    /**
     * Find customer by ID and company (for tenant isolation) with eager loading
     */
    @Query("SELECT c FROM Customer c WHERE c.id = :id AND c.company = :company AND c.isActive = true")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "store", "createdByUser"})
    Optional<Customer> findByIdAndCompanyAndIsActiveTrue(@Param("id") UUID id, @Param("company") Company company);
    
    /**
     * Find customer by phone number
     */
    Optional<Customer> findByCompanyAndPhoneNumberAndIsActiveTrue(Company company, String phoneNumber);
    
    /**
     * Find customer by email
     */
    Optional<Customer> findByCompanyAndEmailAndIsActiveTrue(Company company, String email);
    
    /**
     * Check if customer exists by phone number
     */
    boolean existsByCompanyAndPhoneNumberAndIsActiveTrue(Company company, String phoneNumber);
    
    /**
     * Check if customer exists by email
     */
    boolean existsByCompanyAndEmailAndIsActiveTrue(Company company, String email);
    
    /**
     * Search customers by name, phone, or email with eager loading
     */
    @Query("SELECT c FROM Customer c WHERE c.company = :company AND c.isActive = true " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.name ASC")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "store", "createdByUser"})
    Page<Customer> searchCustomers(@Param("company") Company company, 
                                   @Param("searchTerm") String searchTerm, 
                                   Pageable pageable);
    
    /**
     * Find customers by customer type with eager loading
     */
    @Query("SELECT c FROM Customer c WHERE c.company = :company AND c.customerType = :customerType AND c.isActive = true ORDER BY c.name ASC")
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "store", "createdByUser"})
    Page<Customer> findByCompanyAndCustomerTypeAndIsActiveTrueOrderByNameAsc(
        @Param("company") Company company, @Param("customerType") Customer.CustomerType customerType, Pageable pageable);
    
    /**
     * Count active customers for a company
     */
    long countByCompanyAndIsActiveTrue(Company company);
}
