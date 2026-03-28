package com.meridian.portal.discovery.service;

import com.meridian.portal.discovery.dto.ActiveDiscoveryRule;
import com.meridian.portal.discovery.dto.DiscoveryRuleResponse;
import com.meridian.portal.discovery.dto.UpsertDiscoveryRuleRequest;
import com.meridian.portal.exception.NotFoundException;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.service.AdminAuditLogService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoveryRuleService {

    private static final Set<String> SUPPORTED_RULES = Set.of("EXCLUDE_SKU", "PIN_SKU");

    private final JdbcTemplate jdbcTemplate;
    private final AdminAuditLogService adminAuditLogService;

    public DiscoveryRuleService(JdbcTemplate jdbcTemplate, AdminAuditLogService adminAuditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Transactional(readOnly = true)
    public List<DiscoveryRuleResponse> listRules() {
        return jdbcTemplate.query(
            """
            SELECT id, name, rule_type, match_value, target_value, priority, active, created_by, created_at, updated_at
              FROM discovery_rules
             ORDER BY active DESC, priority ASC, id ASC
            """,
            (rs, i) -> new DiscoveryRuleResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("rule_type"),
                rs.getString("match_value"),
                rs.getString("target_value"),
                rs.getInt("priority"),
                rs.getBoolean("active"),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            )
        );
    }

    @Transactional(readOnly = true)
    public List<ActiveDiscoveryRule> activeRules() {
        return jdbcTemplate.query(
            """
            SELECT rule_type, match_value, target_value, priority
              FROM discovery_rules
             WHERE active = 1
             ORDER BY priority ASC, id ASC
            """,
            (rs, i) -> new ActiveDiscoveryRule(
                rs.getString("rule_type"),
                rs.getString("match_value"),
                rs.getString("target_value"),
                rs.getInt("priority")
            )
        );
    }

    @Transactional
    public DiscoveryRuleResponse createRule(UpsertDiscoveryRuleRequest request, Authentication auth) {
        ValidatedRule rule = validate(request);
        String actor = auth.getName();
        Instant now = Instant.now();

        jdbcTemplate.update(
            """
            INSERT INTO discovery_rules (name, rule_type, match_value, target_value, priority, active, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            rule.name(),
            rule.ruleType(),
            rule.matchValue(),
            rule.targetValue(),
            rule.priority(),
            rule.active() ? 1 : 0,
            actor,
            Timestamp.from(now),
            Timestamp.from(now)
        );

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        adminAuditLogService.log(
            "DISCOVERY_RULE_CREATE",
            "DISCOVERY_RULE",
            id,
            null,
            "ruleType=" + rule.ruleType() + ",target=" + rule.targetValue()
        );
        return getRuleById(id == null ? -1L : id);
    }

    @Transactional
    public DiscoveryRuleResponse updateRule(long id, UpsertDiscoveryRuleRequest request) {
        ValidatedRule rule = validate(request);

        int updated = jdbcTemplate.update(
            """
            UPDATE discovery_rules
               SET name = ?, rule_type = ?, match_value = ?, target_value = ?, priority = ?, active = ?
             WHERE id = ?
            """,
            rule.name(),
            rule.ruleType(),
            rule.matchValue(),
            rule.targetValue(),
            rule.priority(),
            rule.active() ? 1 : 0,
            id
        );
        if (updated == 0) {
            throw new NotFoundException("Discovery rule not found");
        }
        adminAuditLogService.log(
            "DISCOVERY_RULE_UPDATE",
            "DISCOVERY_RULE",
            id,
            null,
            "ruleType=" + rule.ruleType() + ",target=" + rule.targetValue()
        );
        return getRuleById(id);
    }

    @Transactional
    public void deleteRule(long id) {
        int deleted = jdbcTemplate.update("DELETE FROM discovery_rules WHERE id = ?", id);
        if (deleted == 0) {
            throw new NotFoundException("Discovery rule not found");
        }
        adminAuditLogService.log("DISCOVERY_RULE_DELETE", "DISCOVERY_RULE", id, null, "deleted=true");
    }

    private DiscoveryRuleResponse getRuleById(long id) {
        List<DiscoveryRuleResponse> rows = jdbcTemplate.query(
            """
            SELECT id, name, rule_type, match_value, target_value, priority, active, created_by, created_at, updated_at
              FROM discovery_rules
             WHERE id = ?
            """,
            (rs, i) -> new DiscoveryRuleResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("rule_type"),
                rs.getString("match_value"),
                rs.getString("target_value"),
                rs.getInt("priority"),
                rs.getBoolean("active"),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            ),
            id
        );
        if (rows.isEmpty()) {
            throw new NotFoundException("Discovery rule not found");
        }
        return rows.getFirst();
    }

    private ValidatedRule validate(UpsertDiscoveryRuleRequest request) {
        if (request == null) {
            throw new ValidationException("Rule payload is required");
        }
        String name = trimToNull(request.name());
        String ruleType = trimToNull(request.ruleType());
        String matchValue = trimToNull(request.matchValue());
        String targetValue = trimToNull(request.targetValue());
        if (name == null || ruleType == null || targetValue == null) {
            throw new ValidationException("name, ruleType, and targetValue are required");
        }
        String normalizedRuleType = ruleType.toUpperCase(Locale.ROOT);
        if (!SUPPORTED_RULES.contains(normalizedRuleType)) {
            throw new ValidationException("Unsupported ruleType. Supported: EXCLUDE_SKU, PIN_SKU");
        }
        return new ValidatedRule(name, normalizedRuleType, matchValue, targetValue, request.priority(), request.active());
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private record ValidatedRule(
        String name,
        String ruleType,
        String matchValue,
        String targetValue,
        int priority,
        boolean active
    ) {}
}
