package com.meridian.portal.discovery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "search_query_events")
public class SearchQueryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "query_text", length = 255)
    private String queryText;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "min_price", precision = 10, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "condition_status", length = 30)
    private String conditionStatus;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "distance_miles")
    private Integer distanceMiles;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public void setConditionStatus(String conditionStatus) {
        this.conditionStatus = conditionStatus;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public void setDistanceMiles(Integer distanceMiles) {
        this.distanceMiles = distanceMiles;
    }
}
