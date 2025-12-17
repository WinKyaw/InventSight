package com.pos.inventsight.model.sql;

public enum UserRole {
    OWNER,
    FOUNDER,
    CO_OWNER,
    MANAGER,
    EMPLOYEE,
    CUSTOMER,
    MERCHANT,
    PARTNER,
    // Legacy roles for backward compatibility
    USER,
    ADMIN,
    CASHIER
}