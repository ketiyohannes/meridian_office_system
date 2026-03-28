package com.meridian.portal.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationSelectionEngineUnitTest {

    private final RecommendationSelectionEngine engine = new RecommendationSelectionEngine();

    @Test
    void fillsNeededRecommendationsWithEligibleCandidates() {
        LinkedHashMap<String, PickedCandidate> picked = new LinkedHashMap<>();
        engine.pickFromList(
            List.of(
                new ScoredCandidate("A", "A", "CAT-1", BigDecimal.ONE, 0.6),
                new ScoredCandidate("B", "B", "CAT-2", BigDecimal.TEN, 0.5)
            ),
            2,
            picked,
            Set.of(),
            Set.of(),
            Map.of(),
            200,
            false,
            "behavioral",
            new java.util.HashSet<>(),
            1
        );

        assertEquals(2, picked.size());
        assertTrue(picked.containsKey("A"));
        assertTrue(picked.containsKey("B"));
    }

    @Test
    void skipsSeenOrCappedCandidates() {
        LinkedHashMap<String, PickedCandidate> picked = new LinkedHashMap<>();
        engine.pickFromList(
            List.of(
                new ScoredCandidate("A", "A", "CAT-1", BigDecimal.ONE, 0.6),
                new ScoredCandidate("B", "B", "CAT-2", BigDecimal.TEN, 0.5),
                new ScoredCandidate("C", "C", "CAT-3", BigDecimal.TEN, 0.4)
            ),
            2,
            picked,
            Set.of("A"),
            Set.of("B"),
            Map.of("C", 200),
            200,
            false,
            "behavioral",
            new java.util.HashSet<>(),
            1
        );

        assertEquals(0, picked.size());
    }
}
