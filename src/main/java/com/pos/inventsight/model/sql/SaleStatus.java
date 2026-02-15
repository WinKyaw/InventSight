package com.pos.inventsight.model.sql;

public enum SaleStatus {
    PENDING,          // Initial state
    PAID,            // Payment completed, awaiting fulfillment
    READY_FOR_PICKUP, // Fulfilled, type=PICKUP
    OUT_FOR_DELIVERY, // Fulfilled, type=DELIVERY
    DELIVERED,       // Delivery completed
    COMPLETED,       // Final state
    CANCELLED,
    REFUNDED
}