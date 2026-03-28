package com.meridian.portal.recommendation.service;

import java.math.BigDecimal;

record ScoredCandidate(String sku, String name, String category, BigDecimal price, double score) {}
