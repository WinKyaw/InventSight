package com.pos.inventsight.model.sql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfer_requests")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransferRequest {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Company company;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_warehouse_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Warehouse fromWarehouse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_store_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Store toStore;
    
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User requestedBy;
    
    @Column(name = "requested_at")
    private LocalDateTime requestedAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    
    public Store getToStore() {
        return toStore;
    }
    
    public void setToStore(Store toStore) {
        this.toStore = toStore;
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
}
