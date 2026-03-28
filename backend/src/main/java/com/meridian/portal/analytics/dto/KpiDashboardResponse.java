package com.meridian.portal.analytics.dto;

import java.time.Instant;
import java.util.List;

public record KpiDashboardResponse(
    Instant from,
    Instant to,
    double gmv,
    long orderVolume,
    double conversionRatePercent,
    double averageOrderValue,
    double repeatPurchaseRatePercent,
    double fulfillmentTimelinessPercent,
    List<CancellationReasonStat> cancellationReasons
) {}
