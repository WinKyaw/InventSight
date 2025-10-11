package com.pos.inventsight.repository;

import com.pos.inventsight.model.sql.MfaBackupCode;
import com.pos.inventsight.model.sql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, UUID> {
    
    List<MfaBackupCode> findByUser(User user);
    
    List<MfaBackupCode> findByUserAndUsed(User user, Boolean used);
    
    Optional<MfaBackupCode> findByCodeHash(String codeHash);
    
    void deleteByUser(User user);
}
