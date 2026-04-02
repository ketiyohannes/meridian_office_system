package com.meridian.portal.discovery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "zip_reference")
public class ZipReference {

    @Id
    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_code", length = 10)
    private String stateCode;

    public String getZipCode() {
        return zipCode;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getCity() {
        return city;
    }

    public String getStateCode() {
        return stateCode;
    }
}
