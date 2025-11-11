package com.pos.inventsight.repository;

import com.pos.inventsight.model.sql.OtpCode;
import com.pos.inventsight.model.sql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    
    /**
     * Find the most recent valid (not verified, not expired) OTP code for a user
     */
    @Query("SELECT o FROM OtpCode o WHERE o.user = :user " +
           "AND o.verified = false " +
           "AND o.expiresAt > :now " +
           "ORDER BY o.createdAt DESC")
    Optional<OtpCode> findLatestValidOtpByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * Find all OTP codes for a user created within a time window
     */
    @Query("SELECT o FROM OtpCode o WHERE o.user = :user " +
           "AND o.createdAt > :since " +
           "ORDER BY o.createdAt DESC")
    List<OtpCode> findOtpsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);
    
    /**
     * Count OTP codes sent to a user within a time window
     */
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.user = :user " +
           "AND o.createdAt > :since")
    long countOtpsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);
    
    /**
     * Delete expired OTP codes (cleanup job)
     */
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
    
    /**
     * Find all verified OTP codes for audit purposes
     */
    List<OtpCode> findByUserAndVerifiedTrue(User user);
    
    /**
     * Find all OTP codes by user and delivery method
     */
    List<OtpCode> findByUserAndDeliveryMethod(User user, OtpCode.DeliveryMethod deliveryMethod);
}
