package com.rentmanager.backend.dashboard;

import java.util.List;

public record DashboardResponse(
    long totalProperties,
    long availableProperties,
    long rentedProperties,
    long activeContracts,
    long pendingPayments,
    long overduePayments,
    long openMaintenanceRequests,
    List<CurrencyTotal> monthlyIncome) {}
