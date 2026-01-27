package com.pos.inventsight.model.sql;

public enum TransferRequestStatus {
    PENDING,           // Initial request created
    APPROVED,          // GM+ approved the request
    REJECTED,          // GM+ rejected the request
    PREPARING,         // Items being prepared for shipment
    IN_TRANSIT,        // Items shipped with carrier
    DELIVERED,         // Items delivered (not yet received)
    RECEIVED,          // Items received and confirmed
    PARTIALLY_RECEIVED, // Some items received
    CANCELLED,         // Request cancelled
    COMPLETED          // Transfer fully completed
}
