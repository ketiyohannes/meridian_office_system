package com.meridian.portal.recommendation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class RecommendationSelectionEngine {

    void pickFromList(
        List<ScoredCandidate> source,
        int needed,
        LinkedHashMap<String, PickedCandidate> picked,
        Set<String> seenSkus,
        Set<String> cappedSkus,
        Map<String, Integer> dailyImpressions,
        int perRegionCap,
        boolean longTail,
        String reason,
        Set<String> categoriesCovered,
        int minCategoryDiversity
    ) {
        if (needed <= 0) {
            return;
        }

        int targetSize = picked.size() + needed;
        for (ScoredCandidate c : source) {
            if (picked.size() >= targetSize) {
                break;
            }
            if (!RecommendationSelectionPolicy.isEligibleCandidate(
                c.sku(),
                picked.keySet(),
                seenSkus,
                cappedSkus,
                dailyImpressions,
                perRegionCap
            )) {
                continue;
            }

            if (RecommendationSelectionPolicy.shouldHoldForCategoryDiversity(categoriesCovered, c.category(), minCategoryDiversity)) {
                continue;
            }

            picked.put(c.sku(), new PickedCandidate(
                c.sku(),
                c.name(),
                c.category(),
                c.price(),
                round2(c.score()),
                longTail,
                reason + ",category=" + c.category()
            ));
            categoriesCovered.add(c.category());
        }

        if (picked.size() < targetSize) {
            for (ScoredCandidate c : source) {
                if (picked.size() >= targetSize) {
                    break;
                }
                if (!RecommendationSelectionPolicy.isEligibleCandidate(
                    c.sku(),
                    picked.keySet(),
                    seenSkus,
                    cappedSkus,
                    dailyImpressions,
                    perRegionCap
                )) {
                    continue;
                }
                picked.put(c.sku(), new PickedCandidate(
                    c.sku(),
                    c.name(),
                    c.category(),
                    c.price(),
                    round2(c.score()),
                    longTail,
                    reason + ",backfill=true,category=" + c.category()
                ));
                categoriesCovered.add(c.category());
            }
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
