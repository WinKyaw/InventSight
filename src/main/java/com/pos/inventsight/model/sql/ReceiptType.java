package com.pos.inventsight.model.sql;

/**
 * Receipt type enum for sales/receipts
 */
public enum ReceiptType {
    IN_STORE,    // Immediate purchase at store
    DELIVERY,    // Requires delivery
    PICKUP       // Customer will pick up
}
