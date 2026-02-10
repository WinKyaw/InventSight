package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.TransferLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TransferLocation entities.
 * Updated in V39 to support route-based transfer locations.
 */
@Repository
public interface TransferLocationRepository extends JpaRepository<TransferLocation, UUID> {

    /**
     * Find transfer route by from and to locations
     */
    @Query("SELECT tl FROM TransferLocation tl " +
           "WHERE tl.fromId = :fromId " +
           "AND tl.fromLocationType = :fromType " +
           "AND tl.toId = :toId " +
           "AND tl.toLocationType = :toType")
    Optional<TransferLocation> findByFromAndTo(
        @Param("fromId") UUID fromId,
        @Param("fromType") String fromType,
        @Param("toId") UUID toId,
        @Param("toType") String toType
    );

    /**
     * Find all routes originating from a specific location
     */
    @Query("SELECT tl FROM TransferLocation tl " +
           "WHERE tl.fromLocationType = :type " +
           "AND tl.fromId = :id")
    List<TransferLocation> findByFromLocationTypeAndFromId(
        @Param("type") String type,
        @Param("id") UUID id
    );

    /**
     * Find all routes going to a specific location
     */
    @Query("SELECT tl FROM TransferLocation tl " +
           "WHERE tl.toLocationType = :type " +
           "AND tl.toId = :id")
    List<TransferLocation> findByToLocationTypeAndToId(
        @Param("type") String type,
        @Param("id") UUID id
    );

    /**
     * Find all routes involving a specific location (either from or to)
     */
    @Query("SELECT tl FROM TransferLocation tl " +
           "WHERE (tl.fromLocationType = :type AND tl.fromId = :id) " +
           "OR (tl.toLocationType = :type AND tl.toId = :id)")
    List<TransferLocation> findByLocation(
        @Param("type") String type,
        @Param("id") UUID id
    );

    // Legacy methods for backward compatibility (will be removed in future version)
    /**
     * @deprecated Use findByFromAndTo or findByLocation instead
     */
    @Deprecated
    @Query("SELECT tl FROM TransferLocation tl WHERE tl.fromId = :warehouseId AND tl.fromLocationType = 'WAREHOUSE'")
    Optional<TransferLocation> findByWarehouseId(@Param("warehouseId") UUID warehouseId);

    /**
     * @deprecated Use findByFromAndTo or findByLocation instead
     */
    @Deprecated
    @Query("SELECT tl FROM TransferLocation tl WHERE tl.fromId = :storeId AND tl.fromLocationType = 'STORE'")
    Optional<TransferLocation> findByStoreId(@Param("storeId") UUID storeId);
}
