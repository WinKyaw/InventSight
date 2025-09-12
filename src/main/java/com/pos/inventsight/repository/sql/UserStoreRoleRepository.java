package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.UserStoreRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStoreRoleRepository extends JpaRepository<UserStoreRole, Long> {
    
    List<UserStoreRole> findByUserAndIsActiveTrue(User user);
    
    List<UserStoreRole> findByStoreAndIsActiveTrue(Store store);
    
    Optional<UserStoreRole> findByUserAndStoreAndIsActiveTrue(User user, Store store);
    
    List<UserStoreRole> findByRoleAndIsActiveTrue(UserRole role);
    
    List<UserStoreRole> findByStoreAndRoleAndIsActiveTrue(Store store, UserRole role);
    
    @Query("SELECT usr FROM UserStoreRole usr WHERE usr.user = :user AND usr.store = :store")
    List<UserStoreRole> findAllByUserAndStore(@Param("user") User user, @Param("store") Store store);
    
    @Query("SELECT usr FROM UserStoreRole usr WHERE usr.user = :user AND usr.role IN :roles AND usr.isActive = true")
    List<UserStoreRole> findByUserAndRolesAndActiveTrue(@Param("user") User user, @Param("roles") List<UserRole> roles);
    
    @Query("SELECT usr FROM UserStoreRole usr WHERE usr.store = :store AND usr.role IN :roles AND usr.isActive = true")
    List<UserStoreRole> findByStoreAndRolesAndActiveTrue(@Param("store") Store store, @Param("roles") List<UserRole> roles);
    
    @Query("SELECT COUNT(usr) FROM UserStoreRole usr WHERE usr.store = :store AND usr.isActive = true")
    long countActiveUsersByStore(@Param("store") Store store);
    
    @Query("SELECT COUNT(usr) FROM UserStoreRole usr WHERE usr.store = :store AND usr.role = :role AND usr.isActive = true")
    long countActiveUsersByStoreAndRole(@Param("store") Store store, @Param("role") UserRole role);
    
    @Query("SELECT DISTINCT usr.store FROM UserStoreRole usr WHERE usr.user = :user AND usr.isActive = true")
    List<Store> findStoresByUser(@Param("user") User user);
    
    @Query("SELECT usr.role FROM UserStoreRole usr WHERE usr.user = :user AND usr.store = :store AND usr.isActive = true")
    Optional<UserRole> findRoleByUserAndStore(@Param("user") User user, @Param("store") Store store);
    
    boolean existsByUserAndStoreAndIsActiveTrue(User user, Store store);
    
    @Query("SELECT usr FROM UserStoreRole usr WHERE usr.user = :user AND usr.role IN ('OWNER', 'CO_OWNER') AND usr.isActive = true")
    List<UserStoreRole> findOwnerRolesByUser(@Param("user") User user);
}