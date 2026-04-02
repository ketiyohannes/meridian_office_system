package com.meridian.portal.recommendation.service;

import java.math.BigDecimal;

record PickedCandidate(
    String sku,
    String name,
    String category,
    BigDecimal price,
    double score,
    boolean longTail,
    String reason
) {}
