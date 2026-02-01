package com.pos.inventsight.model.sql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TransferRequest entity representing inventory transfer requests between locations.
 * 
 * Note on EAGER Fetch Strategy:
 * All @ManyToOne relationships use EAGER fetch to ensure nested objects (Warehouse, Store, User, Company)
 * are fully loaded and serialized in API responses. This provides complete object details (names, addresses, etc.)
 * instead of just IDs in JSON responses.
 * 
 * Performance Consideration:
 * EAGER fetch may cause N+1 queries when loading lists of TransferRequests. For high-volume operations,
 * consider using @EntityGraph, query-specific fetch joins, or DTOs with selective field projection.
 * The current approach prioritizes API response completeness for typical use cases.
 */
@Entity
@Table(name = "transfer_requests")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransferRequest {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "users",
        "stores",
        "products",
        "createdAt",
        "updatedAt"
    })
    private Company company;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_warehouse_id")
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "company",
        "createdAt",
        "updatedAt",
        "createdBy",
        "updatedBy"
    })
    private Warehouse fromWarehouse;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_store_id")
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "company",
        "createdAt",
        "updatedAt"
    })
    private Store fromStore;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_store_id")
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "company",
        "createdAt",
        "updatedAt"
    })
    private Store toStore;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_warehouse_id")
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "company",
        "createdAt",
        "updatedAt",
        "createdBy",
        "updatedBy"
    })
    private Warehouse toWarehouse;
    
    @NotNull
    @Min(1)
    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;
    
    @Min(0)
    @Column(name = "approved_quantity")
    private Integer approvedQuantity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TransferRequestStatus status = TransferRequestStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private TransferRequestPriority priority = TransferRequestPriority.MEDIUM;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by", nullable = false)
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "password",
        "role",
        "subscriptionLevel",
        "tenantId",
        "defaultTenantId",
        "createdAt",
        "updatedAt",
        "lastLogin"
    })
    private User requestedBy;
    
    @Column(name = "requested_at")
    private LocalDateTime requestedAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by")
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "password",
        "role",
        "subscriptionLevel",
        "tenantId",
        "defaultTenantId",
        "createdAt",
        "updatedAt",
        "lastLogin"
    })
    private User approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Enhanced location fields - support any combination of Store/Warehouse
    @Column(name = "from_location_type", length = 20)
    private String fromLocationType; // "STORE" or "WAREHOUSE"
    
    @Column(name = "from_location_id")
    private UUID fromLocationId;
    
    @Column(name = "to_location_type", length = 20)
    private String toLocationType; // "STORE" or "WAREHOUSE"
    
    @Column(name = "to_location_id")
    private UUID toLocationId;
    
    // Item details
    @Column(name = "item_name")
    private String itemName;
    
    @Column(name = "item_sku", length = 100)
    private String itemSku;
    
    // Carrier and delivery tracking
    @Column(name = "carrier_name", length = 200)
    private String carrierName;
    
    @Column(name = "carrier_phone", length = 20)
    private String carrierPhone;
    
    @Column(name = "carrier_vehicle", length = 100)
    private String carrierVehicle;
    
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
    
    @Column(name = "estimated_delivery_at")
    private LocalDateTime estimatedDeliveryAt;
    
    // Receipt tracking
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "received_by_user_id")
    @JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler",
        "password",
        "role",
        "subscriptionLevel",
        "tenantId",
        "defaultTenantId",
        "createdAt",
        "updatedAt",
        "lastLogin"
    })
    private User receivedByUser;
    
    @Column(name = "receiver_name", length = 200)
    private String receiverName;
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    
    @Column(name = "received_quantity")
    private Integer receivedQuantity;
    
    @Column(name = "receipt_notes", columnDefinition = "TEXT")
    private String receiptNotes;
    
    @Column(name = "is_receipt_confirmed")
    private Boolean isReceiptConfirmed = false;
    
    // === MULTI-TENANT COMPANY TRACKING ===
    @Column(name = "from_company_id")
    private UUID fromCompanyId;
    
    @Column(name = "to_company_id")
    private UUID toCompanyId;
    
    // === ADDITIONAL LOCATION TRACKING ===
    @Column(name = "from_store_id")
    private UUID fromStoreId;
    
    @Column(name = "to_warehouse_id")
    private UUID toWarehouseId;
    
    // === PEOPLE TRACKING ===
    @Column(name = "requested_by_user_id")
    private UUID requestedByUserId;
    
    @Column(name = "requested_by_name")
    private String requestedByName;
    
    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;
    
    @Column(name = "approved_by_name")
    private String approvedByName;
    
    @Column(name = "carrier_user_id")
    private UUID carrierUserId;
    
    @Column(name = "receiver_user_id")
    private UUID receiverUserId;
    
    @Column(name = "handler_user_id")
    private UUID handlerUserId;
    
    @Column(name = "handler_name")
    private String handlerName;
    
    // === PRODUCT INFO ===
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "product_sku")
    private String productSku;
    
    @Column(name = "damaged_quantity")
    private Integer damagedQuantity;
    
    // === TRANSPORT & STATUS ===
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_method")
    private TransportMethod transportMethod;
    
    // === PROOF & SIGNATURE ===
    @Column(name = "receiver_signature_url", length = 500)
    private String receiverSignatureUrl;
    
    @Column(name = "proof_of_delivery_url", length = 500)
    private String proofOfDeliveryUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_on_arrival")
    private ConditionStatus conditionOnArrival;
    
    // === AUDIT ===
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    // Constructors
    public TransferRequest() {
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    public Warehouse getFromWarehouse() {
        return fromWarehouse;
    }
    
    public void setFromWarehouse(Warehouse fromWarehouse) {
        this.fromWarehouse = fromWarehouse;
    }
    
    public Store getFromStore() {
        return fromStore;
    }
    
    public void setFromStore(Store fromStore) {
        this.fromStore = fromStore;
    }
    
    public Store getToStore() {
        return toStore;
    }
    
    public void setToStore(Store toStore) {
        this.toStore = toStore;
    }
    
    public Warehouse getToWarehouse() {
        return toWarehouse;
    }
    
    public void setToWarehouse(Warehouse toWarehouse) {
        this.toWarehouse = toWarehouse;
    }
    
    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }
    
    public Integer getApprovedQuantity() {
        return approvedQuantity;
    }
    
    public void setApprovedQuantity(Integer approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }
    
    public TransferRequestStatus getStatus() {
        return status;
    }
    
    public void setStatus(TransferRequestStatus status) {
        this.status = status;
    }
    
    public TransferRequestPriority getPriority() {
        return priority;
    }
    
    public void setPriority(TransferRequestPriority priority) {
        this.priority = priority;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public User getRequestedBy() {
        return requestedBy;
    }
    
    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public User getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Enhanced location fields getters and setters
    public String getFromLocationType() {
        return fromLocationType;
    }
    
    public void setFromLocationType(String fromLocationType) {
        this.fromLocationType = fromLocationType;
    }
    
    public UUID getFromLocationId() {
        return fromLocationId;
    }
    
    public void setFromLocationId(UUID fromLocationId) {
        this.fromLocationId = fromLocationId;
    }
    
    public String getToLocationType() {
        return toLocationType;
    }
    
    public void setToLocationType(String toLocationType) {
        this.toLocationType = toLocationType;
    }
    
    public UUID getToLocationId() {
        return toLocationId;
    }
    
    public void setToLocationId(UUID toLocationId) {
        this.toLocationId = toLocationId;
    }
    
    // Item details getters and setters
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getItemSku() {
        return itemSku;
    }
    
    public void setItemSku(String itemSku) {
        this.itemSku = itemSku;
    }
    
    // Carrier tracking getters and setters
    public String getCarrierName() {
        return carrierName;
    }
    
    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }
    
    public String getCarrierPhone() {
        return carrierPhone;
    }
    
    public void setCarrierPhone(String carrierPhone) {
        this.carrierPhone = carrierPhone;
    }
    
    public String getCarrierVehicle() {
        return carrierVehicle;
    }
    
    public void setCarrierVehicle(String carrierVehicle) {
        this.carrierVehicle = carrierVehicle;
    }
    
    public LocalDateTime getShippedAt() {
        return shippedAt;
    }
    
    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }
    
    public LocalDateTime getEstimatedDeliveryAt() {
        return estimatedDeliveryAt;
    }
    
    public void setEstimatedDeliveryAt(LocalDateTime estimatedDeliveryAt) {
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }
    
    // Receipt tracking getters and setters
    public User getReceivedByUser() {
        return receivedByUser;
    }
    
    public void setReceivedByUser(User receivedByUser) {
        this.receivedByUser = receivedByUser;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
    
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    
    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
    
    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }
    
    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }
    
    public String getReceiptNotes() {
        return receiptNotes;
    }
    
    public void setReceiptNotes(String receiptNotes) {
        this.receiptNotes = receiptNotes;
    }
    
    public Boolean getIsReceiptConfirmed() {
        return isReceiptConfirmed;
    }
    
    public void setIsReceiptConfirmed(Boolean isReceiptConfirmed) {
        this.isReceiptConfirmed = isReceiptConfirmed;
    }
    
    // Multi-tenant company tracking getters and setters
    public UUID getFromCompanyId() {
        return fromCompanyId;
    }
    
    public void setFromCompanyId(UUID fromCompanyId) {
        this.fromCompanyId = fromCompanyId;
    }
    
    public UUID getToCompanyId() {
        return toCompanyId;
    }
    
    public void setToCompanyId(UUID toCompanyId) {
        this.toCompanyId = toCompanyId;
    }
    
    // Additional location tracking getters and setters
    public UUID getFromStoreId() {
        return fromStoreId;
    }
    
    public void setFromStoreId(UUID fromStoreId) {
        this.fromStoreId = fromStoreId;
    }
    
    public UUID getToWarehouseId() {
        return toWarehouseId;
    }
    
    public void setToWarehouseId(UUID toWarehouseId) {
        this.toWarehouseId = toWarehouseId;
    }
    
    // People tracking getters and setters
    public UUID getRequestedByUserId() {
        return requestedByUserId;
    }
    
    public void setRequestedByUserId(UUID requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }
    
    public String getRequestedByName() {
        return requestedByName;
    }
    
    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }
    
    public UUID getApprovedByUserId() {
        return approvedByUserId;
    }
    
    public void setApprovedByUserId(UUID approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }
    
    public String getApprovedByName() {
        return approvedByName;
    }
    
    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }
    
    public UUID getCarrierUserId() {
        return carrierUserId;
    }
    
    public void setCarrierUserId(UUID carrierUserId) {
        this.carrierUserId = carrierUserId;
    }
    
    public UUID getReceiverUserId() {
        return receiverUserId;
    }
    
    public void setReceiverUserId(UUID receiverUserId) {
        this.receiverUserId = receiverUserId;
    }
    
    public UUID getHandlerUserId() {
        return handlerUserId;
    }
    
    public void setHandlerUserId(UUID handlerUserId) {
        this.handlerUserId = handlerUserId;
    }
    
    public String getHandlerName() {
        return handlerName;
    }
    
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
    
    // Product info getters and setters
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductSku() {
        return productSku;
    }
    
    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }
    
    public Integer getDamagedQuantity() {
        return damagedQuantity;
    }
    
    public void setDamagedQuantity(Integer damagedQuantity) {
        this.damagedQuantity = damagedQuantity;
    }
    
    // Transport & status getters and setters
    public TransportMethod getTransportMethod() {
        return transportMethod;
    }
    
    public void setTransportMethod(TransportMethod transportMethod) {
        this.transportMethod = transportMethod;
    }
    
    // Proof & signature getters and setters
    public String getReceiverSignatureUrl() {
        return receiverSignatureUrl;
    }
    
    public void setReceiverSignatureUrl(String receiverSignatureUrl) {
        this.receiverSignatureUrl = receiverSignatureUrl;
    }
    
    public String getProofOfDeliveryUrl() {
        return proofOfDeliveryUrl;
    }
    
    public void setProofOfDeliveryUrl(String proofOfDeliveryUrl) {
        this.proofOfDeliveryUrl = proofOfDeliveryUrl;
    }
    
    public ConditionStatus getConditionOnArrival() {
        return conditionOnArrival;
    }
    
    public void setConditionOnArrival(ConditionStatus conditionOnArrival) {
        this.conditionOnArrival = conditionOnArrival;
    }
    
    // Audit getters and setters
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    // Calculated field
    @Transient
    public Long getTransitTimeMinutes() {
        if (shippedAt != null && receivedAt != null) {
            return java.time.temporal.ChronoUnit.MINUTES.between(shippedAt, receivedAt);
        }
        return null;
    }
}
