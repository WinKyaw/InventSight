package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.EmployeeRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface EmployeeRelationshipRepository extends JpaRepository<EmployeeRelationship, Long> {
    List<EmployeeRelationship> findByEmployeeId(Long employeeId);
    List<EmployeeRelationship> findByEmployerId(UUID employerId);
    List<EmployeeRelationship> findByStoreId(UUID storeId);
    List<EmployeeRelationship> findByCompanyId(UUID companyId);
}
