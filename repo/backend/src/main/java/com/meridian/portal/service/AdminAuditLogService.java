package com.meridian.portal.service;

import com.meridian.portal.domain.AdminAuditLog;
import com.meridian.portal.dto.AdminAuditLogResponse;
import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.repository.AdminAuditLogRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminAuditLogService(AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @Transactional
    public void log(String action, String targetType, Long targetId, String targetUsername, String details) {
        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.setActorUsername(resolveActorUsername());
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setTargetUsername(targetUsername);
        auditLog.setDetails(details);
        adminAuditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminAuditLogResponse> search(
        String actor,
        String action,
        String targetUsername,
        String targetType,
        String from,
        String to,
        int page,
        int size
    ) {
        int normalizedPage = validatePage(page);
        int normalizedSize = validatePageSize(size);

        Instant fromInstant = parseInstant(from, false);
        Instant toInstant = parseInstant(to, true);

        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new ValidationException("from must be earlier than or equal to to");
        }

        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AdminAuditLog> spec = Specification.where(null);

        if (hasText(actor)) {
            String actorPattern = "%" + actor.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("actorUsername")), actorPattern));
        }

        if (hasText(action)) {
            String actionPattern = "%" + action.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("action")), actionPattern));
        }

        if (hasText(targetUsername)) {
            String targetUserPattern = "%" + targetUsername.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("targetUsername")), targetUserPattern));
        }

        if (hasText(targetType)) {
            String targetTypePattern = "%" + targetType.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("targetType")), targetTypePattern));
        }

        if (fromInstant != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
        }

        if (toInstant != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toInstant));
        }

        Page<AdminAuditLog> result = adminAuditLogRepository.findAll(spec, pageable);

        List<AdminAuditLogResponse> mapped = result.getContent().stream()
            .map(log -> new AdminAuditLogResponse(
                log.getId(),
                log.getActorUsername(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getTargetUsername(),
                log.getDetails(),
                log.getCreatedAt()
            ))
            .toList();

        return new PagedResponse<>(
            mapped,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.isFirst(),
            result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<String> actions() {
        return adminAuditLogRepository.findDistinctActions();
    }

    @Transactional(readOnly = true)
    public List<String> targetTypes() {
        return adminAuditLogRepository.findDistinctTargetTypes();
    }

    private String resolveActorUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return authentication.getName();
    }

    private Instant parseInstant(String raw, boolean endRange) {
        if (!hasText(raw)) {
            return null;
        }

        String value = raw.trim();

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // Try local datetime next.
        }

        try {
            LocalDateTime local = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return local.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ignored) {
            // Try HTML datetime-local format without seconds.
        }

        try {
            LocalDateTime local = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            Instant instant = local.atZone(ZoneId.systemDefault()).toInstant();
            if (endRange) {
                return instant.plusSeconds(59);
            }
            return instant;
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Invalid datetime format for value: " + raw);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new ValidationException("page must be greater than or equal to 0");
        }
        return page;
    }

    private int validatePageSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }
}
