package com.pos.inventsight.model.sql;

/**
 * Payment status enum for receipts
 */
public enum PaymentStatus {
    UNPAID,           // Payment has not been made
    PAID,             // Payment is complete
    PARTIALLY_PAID,   // Partial payment received
    REFUNDED          // Payment was refunded
}
