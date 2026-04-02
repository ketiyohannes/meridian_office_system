package com.meridian.portal.discovery.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductSummaryResponse(
    Long id,
    String sku,
    String name,
    String category,
    BigDecimal price,
    String condition,
    Instant postedAt,
    String zipCode,
    Double distanceMiles
) {}
