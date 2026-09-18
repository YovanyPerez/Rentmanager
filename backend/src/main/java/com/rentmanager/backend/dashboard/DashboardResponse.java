package com.rentmanager.backend.dashboard;

import java.math.BigDecimal;

public record DashboardResponse(
    long totalProperties,
    long availableProperties,
    long rentedProperties,
    long activeContracts,
    long pendingPayments,
    long overduePayments,
    long openMaintenanceRequests,
    BigDecimal monthlyIncome) {}
