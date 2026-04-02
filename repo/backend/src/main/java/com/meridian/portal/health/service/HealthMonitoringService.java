package com.meridian.portal.health.service;

import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.exception.NotFoundException;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.health.dto.HealthAlertResponse;
import com.meridian.portal.health.dto.HealthErrorLogResponse;
import com.meridian.portal.health.dto.HealthSummaryResponse;
import com.meridian.portal.health.dto.HealthThresholdResponse;
import com.meridian.portal.health.dto.UpdateHealthThresholdRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthMonitoringService {

    private static final String HTTP_5XX_RATE = "HTTP_5XX_RATE";

    private final JdbcTemplate jdbcTemplate;

    public HealthMonitoringService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void recordHttpStatus(int statusCode, String requestPath, Instant occurredAt) {
        if (requestPath == null || requestPath.startsWith("/css/") || requestPath.startsWith("/js/") || requestPath.startsWith("/images/")) {
            return;
        }

        LocalDateTime bucket = LocalDateTime.ofInstant(occurredAt, ZoneId.systemDefault())
            .withSecond(0)
            .withNano(0);

        int errorCount = statusCode >= 500 ? 1 : 0;

        jdbcTemplate.update(
            """
            INSERT INTO health_minute_metrics (bucket_start, total_requests, error_5xx_requests)
            VALUES (?, 1, ?)
            ON DUPLICATE KEY UPDATE
              total_requests = total_requests + 1,
              error_5xx_requests = error_5xx_requests + VALUES(error_5xx_requests)
            """,
            Timestamp.valueOf(bucket),
            errorCount
        );

        evaluateHttp5xxThreshold(bucket);
    }

    @Transactional
    public void logError(int statusCode, String requestPath, String message, String detailsJson, Instant occurredAt) {
        jdbcTemplate.update(
            """
            INSERT INTO health_error_logs (status_code, request_path, message, details_json, occurred_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            statusCode,
            truncate(requestPath, 255),
            truncate(message, 500),
            detailsJson,
            Timestamp.from(occurredAt)
        );
    }

    @Transactional(readOnly = true)
    public HealthSummaryResponse summary() {
        Threshold threshold = getThreshold(HTTP_5XX_RATE);
        Aggregate aggregate = aggregateForWindow(threshold.windowMinutes());
        double actualPercent = percentage(aggregate.errors(), aggregate.total());

        return new HealthSummaryResponse(
            HTTP_5XX_RATE,
            threshold.windowMinutes(),
            threshold.thresholdPercent(),
            aggregate.total(),
            aggregate.errors(),
            actualPercent,
            threshold.enabled() && actualPercent > threshold.thresholdPercent(),
            Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<HealthThresholdResponse> thresholds() {
        return jdbcTemplate.query(
            """
            SELECT metric_code, window_minutes, threshold_percent, enabled
            FROM health_thresholds
            ORDER BY metric_code
            """,
            (rs, i) -> new HealthThresholdResponse(
                rs.getString("metric_code"),
                rs.getInt("window_minutes"),
                rs.getDouble("threshold_percent"),
                rs.getBoolean("enabled")
            )
        );
    }

    @Transactional
    public HealthThresholdResponse updateThreshold(String metricCode, UpdateHealthThresholdRequest request) {
        int updated = jdbcTemplate.update(
            """
            UPDATE health_thresholds
               SET window_minutes = ?, threshold_percent = ?, enabled = ?
             WHERE metric_code = ?
            """,
            request.windowMinutes(),
            request.thresholdPercent(),
            request.enabled(),
            metricCode
        );
        if (updated == 0) {
            throw new NotFoundException("Health threshold not found for metric: " + metricCode);
        }
        return thresholds().stream()
            .filter(t -> t.metricCode().equals(metricCode))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Health threshold not found for metric: " + metricCode));
    }

    @Transactional(readOnly = true)
    public PagedResponse<HealthAlertResponse> alerts(Boolean resolved, int page, int size) {
        Pageable pageable = normalizePage(page, size);
        String whereClause = resolved == null ? "" : "WHERE resolved = ?";

        long total = resolved == null
            ? queryForLong("SELECT COUNT(*) FROM health_alerts")
            : queryForLong("SELECT COUNT(*) FROM health_alerts WHERE resolved = ?", resolved ? 1 : 0);

        List<HealthAlertResponse> content = resolved == null
            ? jdbcTemplate.query(
                """
                SELECT id, metric_code, window_minutes, threshold_percent, actual_percent,
                       bucket_start, resolved, resolved_at, created_at
                  FROM health_alerts
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
                """,
                (rs, i) -> new HealthAlertResponse(
                    rs.getLong("id"),
                    rs.getString("metric_code"),
                    rs.getInt("window_minutes"),
                    rs.getDouble("threshold_percent"),
                    rs.getDouble("actual_percent"),
                    rs.getTimestamp("bucket_start").toInstant(),
                    rs.getBoolean("resolved"),
                    rs.getTimestamp("resolved_at") == null ? null : rs.getTimestamp("resolved_at").toInstant(),
                    rs.getTimestamp("created_at").toInstant()
                ),
                pageable.getPageSize(),
                pageable.getOffset()
            )
            : jdbcTemplate.query(
                """
                SELECT id, metric_code, window_minutes, threshold_percent, actual_percent,
                       bucket_start, resolved, resolved_at, created_at
                  FROM health_alerts
                """ + whereClause + """
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
                """,
                (rs, i) -> new HealthAlertResponse(
                    rs.getLong("id"),
                    rs.getString("metric_code"),
                    rs.getInt("window_minutes"),
                    rs.getDouble("threshold_percent"),
                    rs.getDouble("actual_percent"),
                    rs.getTimestamp("bucket_start").toInstant(),
                    rs.getBoolean("resolved"),
                    rs.getTimestamp("resolved_at") == null ? null : rs.getTimestamp("resolved_at").toInstant(),
                    rs.getTimestamp("created_at").toInstant()
                ),
                resolved ? 1 : 0,
                pageable.getPageSize(),
                pageable.getOffset()
            );

        return toPaged(content, total, pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<HealthErrorLogResponse> errors(int page, int size) {
        Pageable pageable = normalizePage(page, size);
        long total = queryForLong("SELECT COUNT(*) FROM health_error_logs");

        List<HealthErrorLogResponse> content = jdbcTemplate.query(
            """
            SELECT id, status_code, request_path, message, details_json, occurred_at
              FROM health_error_logs
             ORDER BY occurred_at DESC
             LIMIT ? OFFSET ?
            """,
            (rs, i) -> new HealthErrorLogResponse(
                rs.getLong("id"),
                rs.getInt("status_code"),
                rs.getString("request_path"),
                rs.getString("message"),
                rs.getString("details_json"),
                rs.getTimestamp("occurred_at").toInstant()
            ),
            pageable.getPageSize(),
            pageable.getOffset()
        );
        return toPaged(content, total, pageable);
    }

    @Transactional
    public void resolveAlert(long alertId) {
        int updated = jdbcTemplate.update(
            """
            UPDATE health_alerts
               SET resolved = 1, resolved_at = CURRENT_TIMESTAMP(6)
             WHERE id = ? AND resolved = 0
            """,
            alertId
        );
        if (updated == 0) {
            throw new NotFoundException("Alert not found or already resolved");
        }
    }

    private void evaluateHttp5xxThreshold(LocalDateTime bucket) {
        Threshold threshold = getThreshold(HTTP_5XX_RATE);
        if (!threshold.enabled()) {
            return;
        }

        Aggregate aggregate = aggregateForWindow(threshold.windowMinutes());
        if (aggregate.total() == 0) {
            return;
        }

        double actualPercent = percentage(aggregate.errors(), aggregate.total());
        if (actualPercent <= threshold.thresholdPercent()) {
            return;
        }

        jdbcTemplate.update(
            """
            INSERT IGNORE INTO health_alerts (
                metric_code, window_minutes, threshold_percent, actual_percent, bucket_start, details_json
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            HTTP_5XX_RATE,
            threshold.windowMinutes(),
            threshold.thresholdPercent(),
            actualPercent,
            Timestamp.valueOf(bucket),
            "{\"totalRequests\":" + aggregate.total() + ",\"errorRequests\":" + aggregate.errors() + "}"
        );
    }

    private Threshold getThreshold(String metricCode) {
        try {
            return jdbcTemplate.queryForObject(
                """
                SELECT metric_code, window_minutes, threshold_percent, enabled
                  FROM health_thresholds
                 WHERE metric_code = ?
                """,
                (rs, i) -> new Threshold(
                    rs.getString("metric_code"),
                    rs.getInt("window_minutes"),
                    rs.getDouble("threshold_percent"),
                    rs.getBoolean("enabled")
                ),
                metricCode
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new NotFoundException("Health threshold not found for metric: " + metricCode);
        }
    }

    private Aggregate aggregateForWindow(int windowMinutes) {
        LocalDateTime from = LocalDateTime.now().minusMinutes(windowMinutes);
        return jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(SUM(total_requests), 0) AS total_requests,
                   COALESCE(SUM(error_5xx_requests), 0) AS error_5xx_requests
              FROM health_minute_metrics
             WHERE bucket_start >= ?
            """,
            (rs, i) -> new Aggregate(rs.getLong("total_requests"), rs.getLong("error_5xx_requests")),
            Timestamp.valueOf(from)
        );
    }

    private long queryForLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private <T> PagedResponse<T> toPaged(List<T> content, long total, Pageable pageable) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return new PagedResponse<>(
            content,
            pageable.getPageNumber(),
            pageable.getPageSize(),
            total,
            totalPages,
            pageable.getPageNumber() == 0,
            pageable.getPageNumber() >= Math.max(totalPages - 1, 0)
        );
    }

    private Pageable normalizePage(int page, int size) {
        if (page < 0) {
            throw new ValidationException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new ValidationException("size must be between 1 and 100");
        }
        return PageRequest.of(page, size);
    }

    private double percentage(long part, long whole) {
        if (whole <= 0) {
            return 0.0;
        }
        return Math.round(((double) part * 10000.0 / (double) whole)) / 100.0;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record Threshold(String metricCode, int windowMinutes, double thresholdPercent, boolean enabled) {}

    private record Aggregate(long total, long errors) {}
}
