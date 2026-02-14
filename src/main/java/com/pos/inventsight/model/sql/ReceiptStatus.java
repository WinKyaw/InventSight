package com.pos.inventsight.model.sql;

/**
 * Status enum for receipts
 */
public enum ReceiptStatus {
    PENDING,      // Receipt is pending (not yet completed)
    COMPLETED,    // Receipt is completed (fulfilled)
    CANCELLED     // Receipt was cancelled
}
