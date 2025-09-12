package com.pos.inventsight.model.sql;

public enum UserRole {
    OWNER,
    CO_OWNER,
    MANAGER,
    EMPLOYEE,
    CUSTOMER,
    // Legacy roles for backward compatibility
    USER,
    ADMIN,
    CASHIER
}