package com.meridian.portal.recommendation.service;

import java.util.List;

record RecommendationBuildResult(
    List<PickedCandidate> candidates,
    String reasonCode,
    int requestedLimit,
    int servedLimit
) {}
