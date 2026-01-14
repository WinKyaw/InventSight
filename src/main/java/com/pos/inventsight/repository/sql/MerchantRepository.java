package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    
    /**
     * Find all merchants for a specific company
     */
    @Query("SELECT m FROM Merchant m WHERE m.company.id = :companyId")
    List<Merchant> findByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find active merchants for a specific company
     */
    @Query("SELECT m FROM Merchant m WHERE m.company.id = :companyId AND m.isActive = true")
    List<Merchant> findActiveByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find merchant by name and company
     */
    @Query("SELECT m FROM Merchant m WHERE m.name = :name AND m.company.id = :companyId")
    List<Merchant> findByNameAndCompanyId(@Param("name") String name, @Param("companyId") UUID companyId);
}
