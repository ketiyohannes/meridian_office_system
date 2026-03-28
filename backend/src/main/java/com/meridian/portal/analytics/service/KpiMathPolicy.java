package com.meridian.portal.analytics.service;

final class KpiMathPolicy {

    private KpiMathPolicy() {}

    static double averageOrderValue(double gmv, long orderVolume) {
        return orderVolume == 0 ? 0 : gmv / (double) orderVolume;
    }

    static double conversionRatePercent(long orderVolume, long visits) {
        return visits == 0 ? 0 : ((double) orderVolume * 100.0 / (double) visits);
    }

    static double repeatPurchaseRatePercent(long repeatBuyerCount, long buyerCount) {
        return buyerCount == 0 ? 0 : ((double) repeatBuyerCount * 100.0 / (double) buyerCount);
    }

    static double fulfillmentTimelinessPercent(long timelyFulfilled, long fulfilledCount) {
        return fulfilledCount == 0 ? 0 : ((double) timelyFulfilled * 100.0 / (double) fulfilledCount);
    }
}
