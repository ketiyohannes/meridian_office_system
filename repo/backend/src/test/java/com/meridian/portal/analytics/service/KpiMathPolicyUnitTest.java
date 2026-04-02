package com.meridian.portal.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KpiMathPolicyUnitTest {

    @Test
    void computesExpectedKpiRatesForNormalInputs() {
        assertEquals(25.0, KpiMathPolicy.averageOrderValue(100.0, 4));
        assertEquals(20.0, KpiMathPolicy.conversionRatePercent(10, 50));
        assertEquals(40.0, KpiMathPolicy.repeatPurchaseRatePercent(2, 5));
        assertEquals(75.0, KpiMathPolicy.fulfillmentTimelinessPercent(3, 4));
    }

    @Test
    void returnsZeroForZeroDenominatorEdgeCases() {
        assertEquals(0.0, KpiMathPolicy.averageOrderValue(100.0, 0));
        assertEquals(0.0, KpiMathPolicy.conversionRatePercent(10, 0));
        assertEquals(0.0, KpiMathPolicy.repeatPurchaseRatePercent(1, 0));
        assertEquals(0.0, KpiMathPolicy.fulfillmentTimelinessPercent(1, 0));
    }
}
