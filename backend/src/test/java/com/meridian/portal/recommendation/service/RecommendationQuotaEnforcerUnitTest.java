package com.meridian.portal.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationQuotaEnforcerUnitTest {

    private final RecommendationQuotaEnforcer enforcer = new RecommendationQuotaEnforcer();

    @Test
    void throwsWhenQuotaInvariantCannotBeMet() {
        LinkedHashMap<String, PickedCandidate> picked = new LinkedHashMap<>();
        assertThrows(IllegalStateException.class, () -> enforcer.enforceLongTailQuota(
            picked,
            List.of(),
            2,
            Set.of(),
            Set.of(),
            Map.of(),
            200,
            "GLOBAL",
            10,
            20
        ));
    }

    @Test
    void enforcesQuotaByPromotingExistingCandidateWhenNeeded() {
        LinkedHashMap<String, PickedCandidate> picked = new LinkedHashMap<>();
        picked.put("SKU-1", new PickedCandidate("SKU-1", "n1", "c1", BigDecimal.ONE, 0.1, false, "r1"));
        picked.put("SKU-2", new PickedCandidate("SKU-2", "n2", "c2", BigDecimal.TEN, 0.2, false, "r2"));

        String reason = enforcer.enforceLongTailQuota(
            picked,
            List.of(),
            1,
            Set.of(),
            Set.of(),
            Map.of(),
            200,
            "GLOBAL",
            5,
            20
        );

        assertNull(reason);
        long longTailCount = picked.values().stream().filter(PickedCandidate::longTail).count();
        assertEquals(1L, longTailCount);
    }
}
