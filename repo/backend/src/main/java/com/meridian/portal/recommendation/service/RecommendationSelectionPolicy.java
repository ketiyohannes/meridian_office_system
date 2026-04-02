package com.meridian.portal.recommendation.service;

import java.util.Map;
import java.util.Set;

final class RecommendationSelectionPolicy {

    private RecommendationSelectionPolicy() {}

    static boolean shouldUseNewestFallback(java.util.List<String> popularCategories) {
        return popularCategories == null || popularCategories.isEmpty();
    }

    static boolean isEligibleCandidate(
        String sku,
        Set<String> alreadyPicked,
        Set<String> seenSkus,
        Set<String> cappedSkus,
        Map<String, Integer> dailyImpressions,
        int perRegionCap
    ) {
        if (alreadyPicked.contains(sku) || seenSkus.contains(sku) || cappedSkus.contains(sku)) {
            return false;
        }
        return dailyImpressions.getOrDefault(sku, 0) < perRegionCap;
    }

    static boolean shouldHoldForCategoryDiversity(Set<String> categoriesCovered, String category, int minCategoryDiversity) {
        return categoriesCovered.size() < minCategoryDiversity && categoriesCovered.contains(category);
    }
}
