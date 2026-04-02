package com.meridian.portal.recommendation.service;

final class RecommendationQuotaPolicy {

    private RecommendationQuotaPolicy() {}

    static int requiredLongTailCount(int limit, int longTailPercent) {
        return (int) Math.ceil(limit * (longTailPercent / 100.0d));
    }

    static String exceptionCodeForShortage(int required, int actual) {
        return actual < required ? "LONG_TAIL_SUPPLY_INSUFFICIENT" : null;
    }
}
