package com.meridian.portal.recommendation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RecommendationQuotaEnforcer {
    private static final Logger log = LoggerFactory.getLogger(RecommendationQuotaEnforcer.class);

    String enforceLongTailQuota(
        LinkedHashMap<String, PickedCandidate> picked,
        List<ScoredCandidate> longTailCandidates,
        int longTailNeeded,
        Set<String> seenSkus,
        Set<String> cappedSkus,
        Map<String, Integer> dailyImpressions,
        int perRegionCap,
        String region,
        int limit,
        int longTailPercent
    ) {
        long currentLongTail = picked.values().stream().filter(PickedCandidate::longTail).count();
        if (currentLongTail >= longTailNeeded || longTailNeeded <= 0) {
            return null;
        }

        List<ScoredCandidate> eligibleLongTail = longTailCandidates.stream()
            .filter(c -> !seenSkus.contains(c.sku()))
            .filter(c -> !cappedSkus.contains(c.sku()))
            .filter(c -> dailyImpressions.getOrDefault(c.sku(), 0) < perRegionCap)
            .toList();

        List<String> removable = picked.values().stream()
            .filter(p -> !p.longTail())
            .map(PickedCandidate::sku)
            .toList();

        int removeIndex = removable.size() - 1;
        for (ScoredCandidate candidate : eligibleLongTail) {
            if (currentLongTail >= longTailNeeded || removeIndex < 0) {
                break;
            }
            if (picked.containsKey(candidate.sku())) {
                continue;
            }
            String replacedSku = removable.get(removeIndex--);
            picked.remove(replacedSku);
            picked.put(candidate.sku(), new PickedCandidate(
                candidate.sku(),
                candidate.name(),
                candidate.category(),
                candidate.price(),
                round2(candidate.score()),
                true,
                "exploration_long_tail,enforced=true,category=" + candidate.category()
            ));
            currentLongTail++;
        }

        if (currentLongTail < longTailNeeded) {
            List<String> promotableSkus = picked.values().stream()
                .filter(p -> !p.longTail())
                .map(PickedCandidate::sku)
                .toList();
            for (String sku : promotableSkus) {
                if (currentLongTail >= longTailNeeded) {
                    break;
                }
                PickedCandidate existing = picked.get(sku);
                picked.put(
                    sku,
                    new PickedCandidate(
                        existing.sku(),
                        existing.name(),
                        existing.category(),
                        existing.price(),
                        existing.score(),
                        true,
                        existing.reason() + ",enforced_long_tail=true"
                    )
                );
                currentLongTail++;
            }
        }

        if (currentLongTail < longTailNeeded) {
            int shortage = longTailNeeded - (int) currentLongTail;
            log.error(
                "event=recommendation_long_tail_invariant_failed region={} limit={} longTailPercent={} required={} actual={} shortage={}",
                region,
                limit,
                longTailPercent,
                longTailNeeded,
                currentLongTail,
                shortage
            );
            throw new IllegalStateException("Long-tail quota invariant failed after effective limit computation");
        }

        log.info(
            "event=recommendation_long_tail_quota_enforced region={} limit={} longTailPercent={} required={} actual={}",
            region,
            limit,
            longTailPercent,
            longTailNeeded,
            currentLongTail
        );
        return null;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
