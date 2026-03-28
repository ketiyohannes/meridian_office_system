package com.meridian.portal.discovery.dto;

public record ProductSearchRequest(
    String keyword,
    String category,
    String minPrice,
    String maxPrice,
    String condition,
    String postedFrom,
    String postedTo,
    String zipCode,
    String distanceMiles,
    int page,
    int size
) {}
