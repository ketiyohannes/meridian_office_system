package com.meridian.portal.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RecommendationQuotaPolicyUnitTest {

    @Test
    void requiredLongTailCountRoundsUpByLimit() {
        assertEquals(1, RecommendationQuotaPolicy.requiredLongTailCount(5, 20));
        assertEquals(2, RecommendationQuotaPolicy.requiredLongTailCount(10, 20));
        assertEquals(5, RecommendationQuotaPolicy.requiredLongTailCount(25, 20));
    }

    @Test
    void shortageCodeReturnedOnlyWhenActualBelowRequired() {
        assertEquals("LONG_TAIL_SUPPLY_INSUFFICIENT", RecommendationQuotaPolicy.exceptionCodeForShortage(3, 2));
        assertNull(RecommendationQuotaPolicy.exceptionCodeForShortage(3, 3));
    }
}
