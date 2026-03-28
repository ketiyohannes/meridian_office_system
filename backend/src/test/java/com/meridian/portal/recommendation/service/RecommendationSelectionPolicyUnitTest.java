package com.meridian.portal.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationSelectionPolicyUnitTest {

    @Test
    void newestFallbackTriggersOnlyWhenPopularCategoriesMissing() {
        assertTrue(RecommendationSelectionPolicy.shouldUseNewestFallback(List.of()));
        assertFalse(RecommendationSelectionPolicy.shouldUseNewestFallback(List.of("Electronics")));
    }

    @Test
    void eligibilityRejectsDedupedAndCappedAndOverCap() {
        Map<String, Integer> daily = new HashMap<>();
        daily.put("SKU-1", 200);

        assertFalse(RecommendationSelectionPolicy.isEligibleCandidate(
            "SKU-1",
            Set.of(),
            Set.of(),
            Set.of(),
            daily,
            200
        ));
        assertFalse(RecommendationSelectionPolicy.isEligibleCandidate(
            "SKU-2",
            Set.of("SKU-2"),
            Set.of(),
            Set.of(),
            Map.of(),
            200
        ));
        assertFalse(RecommendationSelectionPolicy.isEligibleCandidate(
            "SKU-3",
            Set.of(),
            Set.of("SKU-3"),
            Set.of(),
            Map.of(),
            200
        ));
        assertFalse(RecommendationSelectionPolicy.isEligibleCandidate(
            "SKU-4",
            Set.of(),
            Set.of(),
            Set.of("SKU-4"),
            Map.of(),
            200
        ));
    }

    @Test
    void eligibilityPassesForUnseenBelowCapSku() {
        assertTrue(RecommendationSelectionPolicy.isEligibleCandidate(
            "SKU-9",
            Set.of(),
            Set.of(),
            Set.of(),
            Map.of("SKU-9", 199),
            200
        ));
    }

    @Test
    void categoryDiversityHoldOnlyAppliesBeforeTargetDiversityReached() {
        assertTrue(RecommendationSelectionPolicy.shouldHoldForCategoryDiversity(Set.of("Electronics"), "Electronics", 2));
        assertFalse(RecommendationSelectionPolicy.shouldHoldForCategoryDiversity(Set.of("Electronics", "Home"), "Electronics", 2));
    }
}
