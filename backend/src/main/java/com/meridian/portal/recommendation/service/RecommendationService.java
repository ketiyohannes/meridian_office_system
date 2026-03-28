package com.meridian.portal.recommendation.service;

import com.meridian.portal.exception.NotFoundException;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.recommendation.dto.RecommendationConfigResponse;
import com.meridian.portal.recommendation.dto.RecommendationEventRequest;
import com.meridian.portal.recommendation.dto.RecommendationExplainResponse;
import com.meridian.portal.recommendation.dto.RecommendationExposureStatResponse;
import com.meridian.portal.recommendation.dto.RecommendationResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private static final int ABSOLUTE_MAX_LIMIT = 50;
    private static final int MIN_LIMIT = 1;

    private final JdbcTemplate jdbcTemplate;
    private final RecommendationCandidateService recommendationCandidateService;
    private final RecommendationSelectionEngine recommendationSelectionEngine;
    private final RecommendationQuotaEnforcer recommendationQuotaEnforcer;
    private final RecommendationExposureService recommendationExposureService;

    public RecommendationService(
        JdbcTemplate jdbcTemplate,
        RecommendationCandidateService recommendationCandidateService,
        RecommendationSelectionEngine recommendationSelectionEngine,
        RecommendationQuotaEnforcer recommendationQuotaEnforcer,
        RecommendationExposureService recommendationExposureService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.recommendationCandidateService = recommendationCandidateService;
        this.recommendationSelectionEngine = recommendationSelectionEngine;
        this.recommendationQuotaEnforcer = recommendationQuotaEnforcer;
        this.recommendationExposureService = recommendationExposureService;
    }

    @Transactional
    public List<RecommendationResponse> recommend(Authentication auth, String surfaceRaw, Integer limitRaw, String regionRaw) {
        return recommendWithMeta(auth, surfaceRaw, limitRaw, regionRaw).items();
    }

    @Transactional
    public RecommendationApiResult recommendWithMeta(Authentication auth, String surfaceRaw, Integer limitRaw, String regionRaw) {
        RecommendationBuildResult buildResult = buildRecommendations(auth, surfaceRaw, limitRaw, regionRaw);
        List<RecommendationResponse> items = buildResult.candidates().stream()
            .map(p -> new RecommendationResponse(
                p.sku(),
                p.name(),
                p.category(),
                p.price(),
                p.score(),
                p.longTail(),
                buildResult.reasonCode()
            ))
            .toList();
        return new RecommendationApiResult(items, buildResult.reasonCode(), buildResult.requestedLimit(), buildResult.servedLimit());
    }

    @Transactional(readOnly = true)
    public List<RecommendationExplainResponse> explain(Authentication auth, String surfaceRaw, Integer limitRaw, String regionRaw) {
        List<PickedCandidate> picked = buildRecommendationsReadOnly(auth, surfaceRaw, limitRaw, regionRaw);
        return picked.stream()
            .map(p -> new RecommendationExplainResponse(p.sku(), p.name(), p.category(), p.score(), p.longTail(), p.reason()))
            .toList();
    }

    @Transactional
    public void trackEvent(Authentication auth, RecommendationEventRequest request) {
        String username = auth.getName();
        String eventType = parseEventType(request.eventType());
        String region = resolveRegion(username, request.region());
        String query = trimToNull(request.queryText());
        String sku = trimToNull(request.sku());
        String category = trimToNull(request.categoryName());

        if (("VIEW".equals(eventType) || "ADD_TO_CART".equals(eventType) || "PURCHASE".equals(eventType)) && sku == null) {
            throw new ValidationException("sku is required for eventType " + eventType);
        }
        if ("SEARCH".equals(eventType) && query == null && category == null) {
            throw new ValidationException("queryText or categoryName is required for SEARCH events");
        }

        jdbcTemplate.update(
            """
            INSERT INTO recommendation_events (username, event_type, query_text, sku, category_name, region_code)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            username,
            eventType,
            query,
            sku,
            category,
            region
        );
    }

    @Transactional(readOnly = true)
    public List<RecommendationConfigResponse> configs() {
        return jdbcTemplate.query(
            """
            SELECT config_key, config_value
              FROM recommendation_config
             ORDER BY config_key
            """,
            (rs, i) -> new RecommendationConfigResponse(rs.getString("config_key"), rs.getString("config_value"))
        );
    }

    @Transactional
    public RecommendationConfigResponse updateConfig(String key, String value) {
        String configKey = key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
        if (configKey.isBlank()) {
            throw new ValidationException("config key is required");
        }
        if (value == null || value.trim().isBlank()) {
            throw new ValidationException("config value is required");
        }

        int updated = jdbcTemplate.update(
            "UPDATE recommendation_config SET config_value = ? WHERE config_key = ?",
            value.trim(),
            configKey
        );
        if (updated == 0) {
            throw new NotFoundException("Recommendation config not found: " + configKey);
        }
        return new RecommendationConfigResponse(configKey, value.trim());
    }

    @Transactional(readOnly = true)
    public List<RecommendationExposureStatResponse> exposureStats(Integer daysRaw, String regionRaw, String surfaceRaw) {
        int days = daysRaw == null ? 7 : daysRaw;
        if (days < 1 || days > 90) {
            throw new ValidationException("days must be between 1 and 90");
        }
        String region = trimToNull(regionRaw);
        String surface = trimToNull(surfaceRaw);

        StringBuilder sql = new StringBuilder(
            """
            SELECT sku, region_code, surface, COUNT(*) AS impressions
              FROM recommendation_exposures
             WHERE created_at >= ?
            """
        );
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.from(Instant.now().minusSeconds(days * 24L * 60L * 60L)));

        if (region != null) {
            sql.append(" AND region_code = ? ");
            params.add(region.toUpperCase(Locale.ROOT));
        }
        if (surface != null) {
            sql.append(" AND surface = ? ");
            params.add(surface.toUpperCase(Locale.ROOT));
        }
        sql.append(" GROUP BY sku, region_code, surface ORDER BY impressions DESC LIMIT 200");

        return jdbcTemplate.query(
            sql.toString(),
            (rs, i) -> new RecommendationExposureStatResponse(
                rs.getString("sku"),
                rs.getString("region_code"),
                rs.getString("surface"),
                rs.getLong("impressions")
            ),
            params.toArray()
        );
    }

    @Scheduled(cron = "0 20 1 * * *")
    @Transactional
    public void cleanupAgedRecommendationData() {
        Instant eventsCutoff = Instant.now().minusSeconds(90L * 24 * 60 * 60);
        Instant exposuresCutoff = Instant.now().minusSeconds(120L * 24 * 60 * 60);
        LocalDate impressionsCutoff = LocalDate.now().minusDays(180);

        jdbcTemplate.update("DELETE FROM recommendation_events WHERE created_at < ?", Timestamp.from(eventsCutoff));
        jdbcTemplate.update("DELETE FROM recommendation_exposures WHERE created_at < ?", Timestamp.from(exposuresCutoff));
        jdbcTemplate.update("DELETE FROM recommendation_daily_impressions WHERE day_bucket < ?", impressionsCutoff);
    }

    private RecommendationBuildResult buildRecommendations(Authentication auth, String surfaceRaw, Integer limitRaw, String regionRaw) {
        RecommendationRuntimeConfig cfg = loadConfig();
        String username = auth.getName();
        String surface = parseSurface(surfaceRaw);
        int limit = normalizeLimit(limitRaw, cfg.maxLimit());
        String region = resolveRegion(username, regionRaw);

        RecommendationBuildResult buildResult = buildRecommendationList(username, region, limit, cfg);
        List<PickedCandidate> picked = buildResult.candidates();
        List<PickedCandidate> served = picked;
        if (!picked.isEmpty()) {
            served = recommendationExposureService.recordExposureWithinCap(
                username,
                region,
                surface,
                picked,
                cfg.maxDailyImpressionsPerRegion()
            );
        }
        return new RecommendationBuildResult(
            served,
            buildResult.reasonCode(),
            buildResult.requestedLimit(),
            served.size()
        );
    }

    private List<PickedCandidate> buildRecommendationsReadOnly(Authentication auth, String surfaceRaw, Integer limitRaw, String regionRaw) {
        RecommendationRuntimeConfig cfg = loadConfig();
        String username = auth.getName();
        int limit = normalizeLimit(limitRaw, cfg.maxLimit());
        String region = resolveRegion(username, regionRaw);
        return buildRecommendationList(username, region, limit, cfg).candidates();
    }

    private RecommendationBuildResult buildRecommendationList(String username, String region, int limit, RecommendationRuntimeConfig cfg) {
        Instant dedupeCutoff = recommendationExposureService.dedupeCutoff(cfg.dedupeHours());
        Set<String> seenSkus = new HashSet<>(jdbcTemplate.query(
            """
            SELECT DISTINCT sku
              FROM recommendation_exposures
             WHERE username = ?
               AND created_at >= ?
            """,
            (rs, i) -> rs.getString("sku"),
            username,
            Timestamp.from(dedupeCutoff)
        ));

        Map<String, Integer> dailyImpressions = recommendationExposureService.dailyImpressionMap(region);
        Set<String> cappedSkus = new HashSet<>();
        dailyImpressions.forEach((sku, count) -> {
            if (count >= cfg.maxDailyImpressionsPerRegion()) {
                cappedSkus.add(sku);
            }
        });

        List<ScoredCandidate> scoredCandidates = recommendationCandidateService.scoredCandidates(username, cfg.scoringWindowDays());
        List<ScoredCandidate> coldStartCandidates = recommendationCandidateService.coldStartCandidates(cfg.coldStartCategoryLimit());
        List<ScoredCandidate> longTailCandidates = recommendationCandidateService.longTailCandidates(region, cfg.longTailWindowDays());
        List<ScoredCandidate> newest = recommendationCandidateService.newestProducts();

        int effectiveLimit = computeEffectiveLimit(
            limit,
            cfg.longTailPercent(),
            seenSkus,
            cappedSkus,
            dailyImpressions,
            cfg.maxDailyImpressionsPerRegion(),
            longTailCandidates,
            scoredCandidates,
            coldStartCandidates,
            newest
        );
        if (effectiveLimit <= 0) {
            return new RecommendationBuildResult(List.of(), null, limit, 0);
        }

        int longTailNeeded = RecommendationQuotaPolicy.requiredLongTailCount(effectiveLimit, cfg.longTailPercent());
        LinkedHashMap<String, PickedCandidate> picked = new LinkedHashMap<>();
        Set<String> categoriesCovered = new HashSet<>();

        recommendationSelectionEngine.pickFromList(
            longTailCandidates,
            longTailNeeded,
            picked,
            seenSkus,
            cappedSkus,
            dailyImpressions,
            cfg.maxDailyImpressionsPerRegion(),
            true,
            "exploration_long_tail",
            categoriesCovered,
            cfg.minCategoryDiversity()
        );

        recommendationSelectionEngine.pickFromList(
            scoredCandidates,
            effectiveLimit - picked.size(),
            picked,
            seenSkus,
            cappedSkus,
            dailyImpressions,
            cfg.maxDailyImpressionsPerRegion(),
            false,
            "behavioral_scoring",
            categoriesCovered,
            cfg.minCategoryDiversity()
        );

        recommendationSelectionEngine.pickFromList(
            coldStartCandidates,
            effectiveLimit - picked.size(),
            picked,
            seenSkus,
            cappedSkus,
            dailyImpressions,
            cfg.maxDailyImpressionsPerRegion(),
            false,
            "cold_start_category_popularity",
            categoriesCovered,
            cfg.minCategoryDiversity()
        );

        recommendationSelectionEngine.pickFromList(
            newest,
            effectiveLimit - picked.size(),
            picked,
            seenSkus,
            cappedSkus,
            dailyImpressions,
            cfg.maxDailyImpressionsPerRegion(),
            false,
            "fallback_recent_products",
            categoriesCovered,
            cfg.minCategoryDiversity()
        );

        String reasonCode = recommendationQuotaEnforcer.enforceLongTailQuota(
            picked,
            longTailCandidates,
            longTailNeeded,
            seenSkus,
            cappedSkus,
            dailyImpressions,
            cfg.maxDailyImpressionsPerRegion(),
            region,
            effectiveLimit,
            cfg.longTailPercent()
        );

        return new RecommendationBuildResult(new ArrayList<>(picked.values()), reasonCode, limit, picked.size());
    }

    private int computeEffectiveLimit(
        int requestedLimit,
        int longTailPercent,
        Set<String> seenSkus,
        Set<String> cappedSkus,
        Map<String, Integer> dailyImpressions,
        int perRegionCap,
        List<ScoredCandidate> longTailCandidates,
        List<ScoredCandidate> scoredCandidates,
        List<ScoredCandidate> coldStartCandidates,
        List<ScoredCandidate> newestCandidates
    ) {
        Set<String> eligibleLongTailSkus = eligibleSkus(
            longTailCandidates,
            seenSkus,
            cappedSkus,
            dailyImpressions,
            perRegionCap
        );
        int eligibleLongTailCount = eligibleLongTailSkus.size();

        LinkedHashSet<String> eligibleTotalSkus = new LinkedHashSet<>();
        eligibleTotalSkus.addAll(eligibleLongTailSkus);
        eligibleTotalSkus.addAll(eligibleSkus(scoredCandidates, seenSkus, cappedSkus, dailyImpressions, perRegionCap));
        eligibleTotalSkus.addAll(eligibleSkus(coldStartCandidates, seenSkus, cappedSkus, dailyImpressions, perRegionCap));
        eligibleTotalSkus.addAll(eligibleSkus(newestCandidates, seenSkus, cappedSkus, dailyImpressions, perRegionCap));
        int eligibleTotalCount = eligibleTotalSkus.size();

        int maxRequestedBySupply = Math.min(requestedLimit, eligibleTotalCount);
        for (int candidateLimit = maxRequestedBySupply; candidateLimit >= 1; candidateLimit--) {
            int requiredLongTail = RecommendationQuotaPolicy.requiredLongTailCount(candidateLimit, longTailPercent);
            if (requiredLongTail <= eligibleLongTailCount) {
                return candidateLimit;
            }
        }
        return 0;
    }

    private Set<String> eligibleSkus(
        List<ScoredCandidate> candidates,
        Set<String> seenSkus,
        Set<String> cappedSkus,
        Map<String, Integer> dailyImpressions,
        int perRegionCap
    ) {
        LinkedHashSet<String> skus = new LinkedHashSet<>();
        for (ScoredCandidate candidate : candidates) {
            if (!RecommendationSelectionPolicy.isEligibleCandidate(
                candidate.sku(),
                Set.of(),
                seenSkus,
                cappedSkus,
                dailyImpressions,
                perRegionCap
            )) {
                continue;
            }
            skus.add(candidate.sku());
        }
        return skus;
    }

    private String resolveRegion(String username, String preferredRegion) {
        String normalized = trimToNull(preferredRegion);
        if (normalized != null) {
            return normalized.toUpperCase(Locale.ROOT);
        }

        List<String> fromSearch = jdbcTemplate.query(
            """
            SELECT COALESCE(z.state_code, 'GLOBAL') AS region
              FROM search_query_events s
              LEFT JOIN zip_reference z ON z.zip_code = s.zip_code
             WHERE s.username = ?
               AND s.zip_code IS NOT NULL
             ORDER BY s.created_at DESC
             LIMIT 1
            """,
            (rs, i) -> rs.getString("region"),
            username
        );

        if (!fromSearch.isEmpty() && fromSearch.get(0) != null) {
            return fromSearch.get(0).toUpperCase(Locale.ROOT);
        }
        return "GLOBAL";
    }

    private RecommendationRuntimeConfig loadConfig() {
        Map<String, String> kv = new HashMap<>();
        jdbcTemplate.query(
            "SELECT config_key, config_value FROM recommendation_config",
            (rs, i) -> {
                kv.put(rs.getString("config_key"), rs.getString("config_value"));
                return null;
            }
        );
        return new RecommendationRuntimeConfig(
            readInt(kv, "DEDUPE_HOURS", 24, 1, 168),
            readInt(kv, "LONG_TAIL_PERCENT", 20, 5, 80),
            readInt(kv, "MAX_DAILY_IMPRESSIONS_PER_REGION", 200, 10, 5000),
            readInt(kv, "MAX_LIMIT", 30, 5, ABSOLUTE_MAX_LIMIT),
            readInt(kv, "SCORING_WINDOW_DAYS", 30, 1, 180),
            readInt(kv, "LONG_TAIL_WINDOW_DAYS", 7, 1, 60),
            readInt(kv, "COLD_START_CATEGORY_LIMIT", 8, 1, 20),
            readInt(kv, "MIN_CATEGORY_DIVERSITY", 3, 1, 10)
        );
    }

    private int readInt(Map<String, String> kv, String key, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(kv.getOrDefault(key, String.valueOf(fallback)));
            if (value < min || value > max) {
                return fallback;
            }
            return value;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int normalizeLimit(Integer raw, int configMaxLimit) {
        int value = raw == null ? Math.min(configMaxLimit, 10) : raw;
        int upper = Math.min(Math.max(configMaxLimit, MIN_LIMIT), ABSOLUTE_MAX_LIMIT);
        if (value < MIN_LIMIT || value > upper) {
            throw new ValidationException("limit must be between " + MIN_LIMIT + " and " + upper);
        }
        return value;
    }

    private String parseSurface(String raw) {
        if (raw == null || raw.isBlank()) {
            return "HOME";
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("HOME") && !normalized.equals("SEARCH")) {
            throw new ValidationException("surface must be HOME or SEARCH");
        }
        return normalized;
    }

    private String parseEventType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("eventType is required");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        Set<String> allowed = Set.of("SEARCH", "VIEW", "ADD_TO_CART", "PURCHASE");
        if (!allowed.contains(normalized)) {
            throw new ValidationException("eventType must be one of SEARCH, VIEW, ADD_TO_CART, PURCHASE");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private record RecommendationRuntimeConfig(
        int dedupeHours,
        int longTailPercent,
        int maxDailyImpressionsPerRegion,
        int maxLimit,
        int scoringWindowDays,
        int longTailWindowDays,
        int coldStartCategoryLimit,
        int minCategoryDiversity
    ) {}

    public record RecommendationApiResult(
        List<RecommendationResponse> items,
        String reasonCode,
        int requestedLimit,
        int servedLimit
    ) {}
}
