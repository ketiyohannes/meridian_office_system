package com.meridian.portal.service;

import com.meridian.portal.dto.CreateExceptionRequest;
import com.meridian.portal.dto.DecisionExceptionRequest;
import com.meridian.portal.dto.ExceptionRequestResponse;
import com.meridian.portal.exception.NotFoundException;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.notification.dto.ApprovalOutcomeRequest;
import com.meridian.portal.notification.service.NotificationService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExceptionWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(ExceptionWorkflowService.class);

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;
    private final AdminAuditLogService adminAuditLogService;

    public ExceptionWorkflowService(
        JdbcTemplate jdbcTemplate,
        NotificationService notificationService,
        AdminAuditLogService adminAuditLogService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Transactional
    public ExceptionRequestResponse createExceptionRequest(CreateExceptionRequest request, Authentication auth) {
        if (request == null) {
            throw new ValidationException("Exception request payload is required");
        }
        String requester = auth.getName();
        String type = trimToNull(request.requestType());
        String details = trimToNull(request.details());
        if (type == null || details == null) {
            throw new ValidationException("requestType and details are required");
        }
        String requestKey = "EXC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        jdbcTemplate.update(
            """
            INSERT INTO exception_requests (request_key, requester_username, request_type, details, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'PENDING', ?, ?)
            """,
            requestKey,
            requester,
            type,
            details,
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now())
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        adminAuditLogService.log("EXCEPTION_REQUEST_CREATE", "EXCEPTION_REQUEST", id, requester, "type=" + type);
        log.info("event=exception_created requester={} requestId={} type={}", requester, id, type);
        return getExceptionById(id == null ? -1L : id);
    }

    @Transactional(readOnly = true)
    public List<ExceptionRequestResponse> myExceptions(Authentication auth) {
        return jdbcTemplate.query(
            """
            SELECT id, request_key, requester_username, request_type, details, status, decided_by, decision_comment, decided_at, created_at
              FROM exception_requests
             WHERE requester_username = ?
             ORDER BY created_at DESC
             LIMIT 100
            """,
            (rs, i) -> new ExceptionRequestResponse(
                rs.getLong("id"),
                rs.getString("request_key"),
                rs.getString("requester_username"),
                rs.getString("request_type"),
                rs.getString("details"),
                rs.getString("status"),
                rs.getString("decided_by"),
                rs.getString("decision_comment"),
                rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
            ),
            auth.getName()
        );
    }

    @Transactional(readOnly = true)
    public List<ExceptionRequestResponse> pendingExceptions() {
        return jdbcTemplate.query(
            """
            SELECT id, request_key, requester_username, request_type, details, status, decided_by, decision_comment, decided_at, created_at
              FROM exception_requests
             WHERE status = 'PENDING'
             ORDER BY created_at ASC
             LIMIT 200
            """,
            (rs, i) -> new ExceptionRequestResponse(
                rs.getLong("id"),
                rs.getString("request_key"),
                rs.getString("requester_username"),
                rs.getString("request_type"),
                rs.getString("details"),
                rs.getString("status"),
                rs.getString("decided_by"),
                rs.getString("decision_comment"),
                rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
            )
        );
    }

    @Transactional
    public ExceptionRequestResponse decideException(long id, DecisionExceptionRequest request, Authentication auth) {
        if (request == null) {
            throw new ValidationException("Decision payload is required");
        }
        ExceptionRequestResponse current = getExceptionById(id);
        if (!"PENDING".equalsIgnoreCase(current.status())) {
            throw new ValidationException("Only PENDING requests can be decided");
        }
        String status = request.approved() ? "APPROVED" : "REJECTED";
        String comment = trimToNull(request.comment());
        String actor = auth.getName();
        Instant now = Instant.now();
        int updated = jdbcTemplate.update(
            """
            UPDATE exception_requests
               SET status = ?, decided_by = ?, decision_comment = ?, decided_at = ?, updated_at = ?
             WHERE id = ?
               AND status = 'PENDING'
            """,
            status,
            actor,
            comment,
            Timestamp.from(now),
            Timestamp.from(now),
            id
        );
        if (updated == 0) {
            ExceptionRequestResponse latest = getExceptionById(id);
            if (!"PENDING".equalsIgnoreCase(latest.status())) {
                throw new ValidationException("Only PENDING requests can be decided");
            }
            throw new NotFoundException("Exception request not found");
        }
        notificationService.createApprovalOutcomeEvent(
            new ApprovalOutcomeRequest(current.requesterUsername(), "exception-" + id, request.approved(), comment)
        );
        adminAuditLogService.log("EXCEPTION_REQUEST_DECIDE", "EXCEPTION_REQUEST", id, current.requesterUsername(), "status=" + status);
        log.info(
            "event=exception_decided actor={} requestId={} requester={} status={}",
            actor,
            id,
            current.requesterUsername(),
            status
        );
        return getExceptionById(id);
    }

    private ExceptionRequestResponse getExceptionById(long id) {
        List<ExceptionRequestResponse> rows = jdbcTemplate.query(
            """
            SELECT id, request_key, requester_username, request_type, details, status, decided_by, decision_comment, decided_at, created_at
              FROM exception_requests
             WHERE id = ?
            """,
            (rs, i) -> new ExceptionRequestResponse(
                rs.getLong("id"),
                rs.getString("request_key"),
                rs.getString("requester_username"),
                rs.getString("request_type"),
                rs.getString("details"),
                rs.getString("status"),
                rs.getString("decided_by"),
                rs.getString("decision_comment"),
                rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
            ),
            id
        );
        if (rows.isEmpty()) {
            throw new NotFoundException("Exception request not found");
        }
        return rows.getFirst();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
