package com.meridian.portal.service;

import com.meridian.portal.dto.CreateTaskRequest;
import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.dto.TaskResponse;
import com.meridian.portal.exception.NotFoundException;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.repository.UserAccountRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(TaskWorkflowService.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountRepository userAccountRepository;
    private final AdminAuditLogService adminAuditLogService;

    public TaskWorkflowService(
        JdbcTemplate jdbcTemplate,
        UserAccountRepository userAccountRepository,
        AdminAuditLogService adminAuditLogService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userAccountRepository = userAccountRepository;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> myTasks(Authentication auth, String statusRaw, int page, int size) {
        String username = auth.getName();
        ensureUser(username);
        String status = normalizeTaskStatus(statusRaw);

        Pageable pageable = normalizePage(page, size);
        long total = status == null
            ? count("SELECT COUNT(*) FROM user_tasks WHERE username = ?", username)
            : count("SELECT COUNT(*) FROM user_tasks WHERE username = ? AND status = ?", username, status);

        List<TaskResponse> tasks = status == null
            ? jdbcTemplate.query(
                """
                SELECT id, username, title, description, due_at, status, created_by, completed_at, created_at
                  FROM user_tasks
                 WHERE username = ?
                 ORDER BY
                    CASE status WHEN 'OPEN' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END,
                    due_at ASC,
                    created_at DESC
                 LIMIT ? OFFSET ?
                """,
                (rs, i) -> new TaskResponse(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getTimestamp("due_at") == null ? null : rs.getTimestamp("due_at").toInstant(),
                    rs.getString("status"),
                    rs.getString("created_by"),
                    rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                    rs.getTimestamp("created_at").toInstant()
                ),
                username,
                pageable.getPageSize(),
                pageable.getOffset()
            )
            : jdbcTemplate.query(
                """
                SELECT id, username, title, description, due_at, status, created_by, completed_at, created_at
                  FROM user_tasks
                 WHERE username = ? AND status = ?
                 ORDER BY due_at ASC, created_at DESC
                 LIMIT ? OFFSET ?
                """,
                (rs, i) -> new TaskResponse(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getTimestamp("due_at") == null ? null : rs.getTimestamp("due_at").toInstant(),
                    rs.getString("status"),
                    rs.getString("created_by"),
                    rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                    rs.getTimestamp("created_at").toInstant()
                ),
                username,
                status,
                pageable.getPageSize(),
                pageable.getOffset()
            );

        return toPaged(tasks, total, pageable);
    }

    @Transactional
    public TaskResponse assignTask(CreateTaskRequest request, Authentication auth) {
        if (request == null) {
            throw new ValidationException("Task payload is required");
        }
        String username = trimToNull(request.username());
        String title = trimToNull(request.title());
        if (username == null || title == null) {
            throw new ValidationException("username and title are required");
        }
        ensureUser(username);
        Instant dueAt = parseInstant(request.dueAt(), "dueAt", true);
        String actor = auth.getName();

        jdbcTemplate.update(
            """
            INSERT INTO user_tasks (username, title, description, due_at, status, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'OPEN', ?, ?, ?)
            """,
            username,
            title,
            trimToNull(request.description()),
            dueAt == null ? null : Timestamp.from(dueAt),
            actor,
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now())
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        adminAuditLogService.log("TASK_ASSIGN", "USER_TASK", id, username, "title=" + title);
        log.info("event=task_assigned actor={} targetUser={} taskId={}", actor, username, id);
        return getTask(id == null ? -1L : id, username);
    }

    @Transactional
    public TaskResponse completeTask(long taskId, Authentication auth) {
        String username = auth.getName();
        int updated = jdbcTemplate.update(
            """
            UPDATE user_tasks
               SET status = 'COMPLETED', completed_at = ?, updated_at = ?
             WHERE id = ? AND username = ? AND status <> 'COMPLETED'
            """,
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now()),
            taskId,
            username
        );
        if (updated == 0) {
            List<String> statusRows = jdbcTemplate.query(
                "SELECT status FROM user_tasks WHERE id = ? AND username = ?",
                (rs, i) -> rs.getString("status"),
                taskId,
                username
            );
            if (statusRows.isEmpty()) {
                throw new NotFoundException("Task not found");
            }
            if ("COMPLETED".equalsIgnoreCase(statusRows.getFirst())) {
                return getTask(taskId, username);
            }
            throw new ValidationException("Task cannot be completed from current state");
        }
        log.info("event=task_completed actor={} taskId={}", username, taskId);
        return getTask(taskId, username);
    }

    private TaskResponse getTask(long id, String username) {
        List<TaskResponse> rows = jdbcTemplate.query(
            """
            SELECT id, username, title, description, due_at, status, created_by, completed_at, created_at
              FROM user_tasks
             WHERE id = ? AND username = ?
            """,
            (rs, i) -> new TaskResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getTimestamp("due_at") == null ? null : rs.getTimestamp("due_at").toInstant(),
                rs.getString("status"),
                rs.getString("created_by"),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
            ),
            id,
            username
        );
        if (rows.isEmpty()) {
            throw new NotFoundException("Task not found");
        }
        return rows.getFirst();
    }

    private void ensureUser(String username) {
        if (userAccountRepository.findByUsername(username).isEmpty()) {
            throw new ValidationException("Unknown user: " + username);
        }
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

    private long count(String sql, Object... args) {
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

    private String normalizeTaskStatus(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        String normalized = value.toUpperCase();
        if (!"OPEN".equals(normalized) && !"IN_PROGRESS".equals(normalized) && !"COMPLETED".equals(normalized)) {
            throw new ValidationException("Invalid task status");
        }
        return normalized;
    }

    private Instant parseInstant(String raw, String field, boolean nullable) {
        String value = trimToNull(raw);
        if (value == null) {
            return nullable ? null : Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime local = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return local.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Invalid datetime for " + field);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
