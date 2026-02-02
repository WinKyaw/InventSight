package com.pos.inventsight.model.sql;

public enum TransferRequestStatus {
    PENDING,           // Requested, awaiting approval
    APPROVED,          // Approved, ready to ship
    REJECTED,          // Request denied
    PREPARING,         // Items being prepared for shipment
    READY,             // Alias for PREPARING - packed and ready for pickup
    IN_TRANSIT,        // Currently being transported
    DELIVERED,         // Arrived at destination, awaiting receipt confirmation
    RECEIVED,          // Confirmed received
    PARTIALLY_RECEIVED, // Some items received
    COMPLETED,         // Fully processed and stocked
    CANCELLED,         // Cancelled before shipping
    DAMAGED,           // Arrived damaged
    LOST               // Lost in transit
}
