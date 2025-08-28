package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByEmailAndUsedFalse(String email);
    
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.email = :email AND t.used = false AND t.expiresAt > :now")
    Optional<EmailVerificationToken> findValidTokenByEmail(@Param("email") String email, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now OR t.used = true")
    int deleteExpiredAndUsedTokens(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.email = :email")
    int deleteAllTokensByEmail(@Param("email") String email);
}