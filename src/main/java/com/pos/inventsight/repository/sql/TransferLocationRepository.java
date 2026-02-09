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
 * Provides methods to find transfer locations by warehouse or store IDs.
 */
@Repository
public interface TransferLocationRepository extends JpaRepository<TransferLocation, UUID> {

    /**
     * Find transfer location by warehouse ID
     */
    @Query("SELECT tl FROM TransferLocation tl WHERE tl.warehouse.id = :warehouseId AND tl.locationType = 'WAREHOUSE'")
    Optional<TransferLocation> findByWarehouseId(@Param("warehouseId") UUID warehouseId);

    /**
     * Find transfer location by store ID
     */
    @Query("SELECT tl FROM TransferLocation tl WHERE tl.store.id = :storeId AND tl.locationType = 'STORE'")
    Optional<TransferLocation> findByStoreId(@Param("storeId") UUID storeId);

    /**
     * Find all transfer locations by location type
     */
    @Query("SELECT tl FROM TransferLocation tl WHERE tl.locationType = :locationType")
    List<TransferLocation> findByLocationType(@Param("locationType") TransferLocation.LocationType locationType);
}
