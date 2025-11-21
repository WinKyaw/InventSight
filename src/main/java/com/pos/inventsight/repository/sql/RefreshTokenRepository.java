package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.RefreshToken;
import com.pos.inventsight.model.sql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Find all valid refresh tokens for a user
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revoked = false " +
           "AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(@Param("userId") Long userId, 
                                             @Param("now") LocalDateTime now);
    
    /**
     * Find all refresh tokens for a user
     */
    List<RefreshToken> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Delete all tokens for a user
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * Revoke all tokens for a user
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now " +
           "WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllUserTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * Check if token exists and is valid
     */
    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.token = :token " +
           "AND rt.revoked = false " +
           "AND rt.expiresAt > :now")
    boolean existsByTokenAndIsValid(@Param("token") String token, @Param("now") LocalDateTime now);
}
