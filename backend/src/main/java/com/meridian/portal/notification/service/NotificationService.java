package com.meridian.portal.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.exception.NotFoundException;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.notification.domain.NotificationEvent;
import com.meridian.portal.notification.domain.NotificationEventDefinition;
import com.meridian.portal.notification.domain.NotificationEventStatus;
import com.meridian.portal.notification.domain.NotificationMessage;
import com.meridian.portal.notification.domain.NotificationStatus;
import com.meridian.portal.notification.domain.NotificationSubscription;
import com.meridian.portal.notification.domain.NotificationTemplate;
import com.meridian.portal.notification.domain.NotificationTopic;
import com.meridian.portal.notification.domain.UserNotificationPreference;
import com.meridian.portal.notification.dto.ApprovalOutcomeRequest;
import com.meridian.portal.notification.dto.CheckinEventRequest;
import com.meridian.portal.notification.dto.EventDefinitionUpdateDto;
import com.meridian.portal.notification.dto.NotificationResponse;
import com.meridian.portal.notification.dto.SubscriptionDto;
import com.meridian.portal.notification.dto.TemplateUpdateDto;
import com.meridian.portal.notification.dto.UserNotificationPreferenceDto;
import com.meridian.portal.notification.repository.NotificationEventDefinitionRepository;
import com.meridian.portal.notification.repository.NotificationEventRepository;
import com.meridian.portal.notification.repository.NotificationMessageRepository;
import com.meridian.portal.notification.repository.NotificationSubscriptionRepository;
import com.meridian.portal.notification.repository.NotificationTemplateRepository;
import com.meridian.portal.notification.repository.UserNotificationPreferenceRepository;
import com.meridian.portal.repository.UserAccountRepository;
import com.meridian.portal.service.AdminAuditLogService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private static final int MAX_REMINDERS_CAP = 3;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);


    private final NotificationTemplateRepository templateRepository;
    private final NotificationEventDefinitionRepository definitionRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationMessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final AdminAuditLogService adminAuditLogService;
    private final ObjectMapper objectMapper;

    public NotificationService(
        NotificationTemplateRepository templateRepository,
        NotificationEventDefinitionRepository definitionRepository,
        UserNotificationPreferenceRepository preferenceRepository,
        NotificationSubscriptionRepository subscriptionRepository,
        NotificationEventRepository eventRepository,
        NotificationMessageRepository messageRepository,
        UserAccountRepository userAccountRepository,
        AdminAuditLogService adminAuditLogService,
        ObjectMapper objectMapper
    ) {
        this.templateRepository = templateRepository;
        this.definitionRepository = definitionRepository;
        this.preferenceRepository = preferenceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.adminAuditLogService = adminAuditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> myNotifications(Authentication auth, int page, int size) {
        validatePage(page);
        validatePageSize(size);
        String username = auth.getName();
        ensureUserExists(username);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<NotificationMessage> result = messageRepository.findByUsernameOrderBySentAtDesc(username, pageable);

        List<NotificationResponse> content = result.getContent().stream()
            .map(this::toResponse)
            .toList();

        return new PagedResponse<>(
            content,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.isFirst(),
            result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public long unreadCount(Authentication auth) {
        ensureUserExists(auth.getName());
        return messageRepository.countByUsernameAndStatus(auth.getName(), NotificationStatus.SENT);
    }

    @Transactional
    public void markRead(Authentication auth, Long notificationId) {
        NotificationMessage message = messageRepository.findByIdAndUsername(notificationId, auth.getName())
            .orElseThrow(() -> new NotFoundException("Notification not found"));
        message.setStatus(NotificationStatus.READ);
        message.setReadAt(Instant.now());
        messageRepository.save(message);
    }

    @Transactional
    public int markAllRead(Authentication auth) {
        ensureUserExists(auth.getName());
        return messageRepository.markAllRead(auth.getName(), NotificationStatus.READ, Instant.now());
    }

    @Transactional(readOnly = true)
    public UserNotificationPreferenceDto myPreferences(Authentication auth) {
        ensureUserExists(auth.getName());
        UserNotificationPreference pref = preferenceRepository.findById(auth.getName())
            .orElseGet(() -> defaultPreference(auth.getName()));
        return new UserNotificationPreferenceDto(pref.getDndStart().toString(), pref.getDndEnd().toString(), pref.getMaxRemindersPerEvent());
    }

    @Transactional
    public UserNotificationPreferenceDto updatePreferences(Authentication auth, UserNotificationPreferenceDto input) {
        if (input == null) {
            throw new ValidationException("Preference payload is required");
        }
        UserNotificationPreference pref = ensureUserSettings(auth.getName());
        LocalTime start = parseLocalTime(input.dndStart(), "dndStart");
        LocalTime end = parseLocalTime(input.dndEnd(), "dndEnd");

        if (input.maxRemindersPerEvent() < 1 || input.maxRemindersPerEvent() > MAX_REMINDERS_CAP) {
            throw new ValidationException("maxRemindersPerEvent must be between 1 and " + MAX_REMINDERS_CAP);
        }

        pref.setDndStart(start);
        pref.setDndEnd(end);
        pref.setMaxRemindersPerEvent(input.maxRemindersPerEvent());
        preferenceRepository.save(pref);

        return new UserNotificationPreferenceDto(pref.getDndStart().toString(), pref.getDndEnd().toString(), pref.getMaxRemindersPerEvent());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDto> mySubscriptions(Authentication auth) {
        ensureUserExists(auth.getName());

        List<NotificationSubscription> stored = subscriptionRepository.findByUsernameOrderByTopicCodeAsc(auth.getName());
        if (stored.isEmpty()) {
            return defaultSubscriptions(auth.getName()).stream()
                .map(s -> new SubscriptionDto(s.getTopicCode().name(), true))
                .toList();
        }

        return stored.stream()
            .map(s -> new SubscriptionDto(s.getTopicCode().name(), s.isSubscribed()))
            .toList();
    }

    @Transactional
    public List<SubscriptionDto> updateSubscriptions(Authentication auth, List<SubscriptionDto> updates) {
        String username = auth.getName();
        ensureUserSettings(username);
        if (updates == null || updates.isEmpty()) {
            throw new ValidationException("At least one subscription update is required");
        }

        for (SubscriptionDto update : updates) {
            if (update == null) {
                throw new ValidationException("Subscription update entry is required");
            }
            NotificationTopic topic = parseTopic(update.topic());
            NotificationSubscription sub = subscriptionRepository.findByUsernameAndTopicCode(username, topic)
                .orElseGet(() -> {
                    NotificationSubscription created = new NotificationSubscription();
                    created.setUsername(username);
                    created.setTopicCode(topic);
                    created.setSubscribed(true);
                    return created;
                });
            sub.setSubscribed(update.subscribed());
            subscriptionRepository.save(sub);
        }

        return mySubscriptions(auth);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> templates() {
        return templateRepository.findAll();
    }

    @Transactional
    public NotificationTemplate updateTemplate(NotificationTopic topic, TemplateUpdateDto dto) {
        if (dto == null) {
            throw new ValidationException("Template payload is required");
        }
        NotificationTemplate template = templateRepository.findById(topic)
            .orElseThrow(() -> new NotFoundException("Notification template not found"));
        template.setTitleTemplate(dto.titleTemplate());
        template.setBodyTemplate(dto.bodyTemplate());
        template.setActive(dto.active());
        NotificationTemplate saved = templateRepository.save(template);
        adminAuditLogService.log(
            "NOTIFICATION_TEMPLATE_UPDATE",
            "NOTIFICATION_TEMPLATE",
            null,
            null,
            "topic=" + topic.name() + ", active=" + dto.active()
        );
        log.info("event=notification_template_updated topic={} active={}", topic.name(), dto.active());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationEventDefinition> definitions() {
        return definitionRepository.findAll();
    }

    @Transactional
    public NotificationEventDefinition updateDefinition(NotificationTopic topic, EventDefinitionUpdateDto dto) {
        if (dto == null) {
            throw new ValidationException("Definition payload is required");
        }
        if (dto.maxReminders() > MAX_REMINDERS_CAP) {
            throw new ValidationException("maxReminders cannot exceed " + MAX_REMINDERS_CAP);
        }
        NotificationEventDefinition definition = definitionRepository.findById(topic)
            .orElseThrow(() -> new NotFoundException("Notification event definition not found"));
        definition.setReminderIntervalMinutes(dto.reminderIntervalMinutes());
        definition.setMaxReminders(dto.maxReminders());
        definition.setActionable(dto.actionable());
        definition.setActive(dto.active());
        NotificationEventDefinition saved = definitionRepository.save(definition);
        adminAuditLogService.log(
            "NOTIFICATION_EVENT_DEFINITION_UPDATE",
            "NOTIFICATION_EVENT_DEFINITION",
            null,
            null,
            "topic=" + topic.name() + ", active=" + dto.active()
        );
        log.info(
            "event=notification_definition_updated topic={} intervalMinutes={} maxReminders={} active={}",
            topic.name(),
            dto.reminderIntervalMinutes(),
            dto.maxReminders(),
            dto.active()
        );
        return saved;
    }

    @Transactional
    public void createCheckinEvents(CheckinEventRequest req) {
        if (req == null) {
            throw new ValidationException("Check-in event payload is required");
        }
        String username = req.username().trim();
        ensureUserExists(username);

        Instant opensAt = parseInstant(req.opensAt(), "opensAt", false);
        Instant cutoffAt = parseInstant(req.cutoffAt(), "cutoffAt", false);
        Instant missedAt = req.missedAt() == null || req.missedAt().isBlank()
            ? cutoffAt.plusSeconds(60)
            : parseInstant(req.missedAt(), "missedAt", false);

        if (!opensAt.isBefore(cutoffAt)) {
            throw new ValidationException("opensAt must be earlier than cutoffAt");
        }

        Instant cutoffWarningAt = cutoffAt.minusSeconds(15 * 60L);
        if (cutoffWarningAt.isBefore(opensAt)) {
            cutoffWarningAt = opensAt;
        }

        scheduleEvent(req.eventKey(), username, NotificationTopic.CHECKIN_WINDOW_OPEN, opensAt, Map.of("time", toDisplayTime(opensAt)));
        scheduleEvent(req.eventKey(), username, NotificationTopic.CHECKIN_CUTOFF_WARNING, cutoffWarningAt, Map.of("time", toDisplayTime(cutoffAt)));
        scheduleEvent(req.eventKey(), username, NotificationTopic.CHECKIN_MISSED, missedAt, Map.of("time", toDisplayTime(missedAt)));
    }

    @Transactional
    public void createApprovalOutcomeEvent(ApprovalOutcomeRequest req) {
        if (req == null) {
            throw new ValidationException("Approval outcome payload is required");
        }
        String username = req.username().trim();
        ensureUserExists(username);

        Map<String, String> payload = new HashMap<>();
        payload.put("outcome", req.approved() ? "APPROVED" : "REJECTED");
        payload.put("details", req.details() == null ? "" : req.details());

        scheduleEvent(req.eventKey(), username, NotificationTopic.EXCEPTION_APPROVAL_OUTCOME, Instant.now(), payload);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processDueEvents() {
        Instant now = Instant.now();
        List<NotificationEvent> dueEvents = eventRepository
            .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(NotificationEventStatus.PENDING, now);
        int completed = 0;
        int deferred = 0;
        int delivered = 0;

        for (NotificationEvent event : dueEvents) {
            UserNotificationPreference pref = ensureUserSettings(event.getUsername());
            NotificationEventDefinition definition = definitionRepository.findById(event.getTopicCode())
                .orElseThrow(() -> new NotFoundException("Missing event definition for topic " + event.getTopicCode()));
            NotificationTemplate template = templateRepository.findById(event.getTopicCode())
                .orElseThrow(() -> new NotFoundException("Template missing for topic " + event.getTopicCode()));

            if (!definition.isActive() || !template.isActive() || !isSubscribed(event.getUsername(), event.getTopicCode())) {
                event.setStatus(NotificationEventStatus.COMPLETED);
                eventRepository.save(event);
                completed++;
                continue;
            }

            int effectiveCap = NotificationDeliveryPolicy.effectiveReminderCap(
                MAX_REMINDERS_CAP,
                pref.getMaxRemindersPerEvent(),
                event.getMaxReminders(),
                definition.getMaxReminders()
            );

            if (isWithinDnd(pref, now)) {
                if (!definition.isActionable() && now.isAfter(event.getEventTime().plusSeconds(2 * 60 * 60))) {
                    event.setStatus(NotificationEventStatus.COMPLETED);
                    completed++;
                } else {
                    event.setNextAttemptAt(nextOutsideDnd(pref, now));
                    deferred++;
                }
                eventRepository.save(event);
                continue;
            }

            sendMessage(event, template);
            delivered++;

            event.setReminderCount(event.getReminderCount() + 1);
            if (event.getReminderCount() >= effectiveCap) {
                event.setStatus(NotificationEventStatus.COMPLETED);
                completed++;
            } else {
                event.setNextAttemptAt(now.plusSeconds(definition.getReminderIntervalMinutes() * 60L));
                deferred++;
            }
            eventRepository.save(event);
        }
        log.info(
            "event=notifications_batch_processed dueEvents={} delivered={} deferred={} completed={}",
            dueEvents.size(),
            delivered,
            deferred,
            completed
        );
    }

    @Scheduled(cron = "0 45 0 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        Instant cutoff = Instant.now().minusSeconds(90L * 24 * 60 * 60);
        messageRepository.deleteBySentAtBefore(cutoff);
        log.info("event=notifications_cleanup cutoff={}", cutoff);
    }

    private void sendMessage(NotificationEvent event, NotificationTemplate template) {
        Map<String, String> payload = parsePayload(event.getPayloadJson());

        String title = applyTemplate(template.getTitleTemplate(), payload);
        String body = applyTemplate(template.getBodyTemplate(), payload);

        NotificationMessage message = new NotificationMessage();
        message.setUsername(event.getUsername());
        message.setTopicCode(event.getTopicCode());
        message.setEventKey(event.getEventKey());
        message.setTitle(title);
        message.setBody(body);
        message.setSentAt(Instant.now());
        message.setStatus(NotificationStatus.SENT);
        messageRepository.save(message);
    }

    private void scheduleEvent(String eventKey, String username, NotificationTopic topic, Instant eventTime, Map<String, String> payload) {
        ensureUserSettings(username);
        if (eventKey == null || eventKey.trim().isEmpty()) {
            throw new ValidationException("eventKey is required");
        }

        NotificationEvent event = eventRepository.findByEventKeyAndUsernameAndTopicCode(eventKey, username, topic)
            .orElseGet(NotificationEvent::new);

        NotificationEventDefinition definition = definitionRepository.findById(topic)
            .orElseThrow(() -> new NotFoundException("Missing event definition for topic " + topic));

        event.setEventKey(eventKey);
        event.setUsername(username);
        event.setTopicCode(topic);
        event.setEventTime(eventTime);
        event.setPayloadJson(writePayload(payload));
        event.setStatus(NotificationEventStatus.PENDING);
        event.setReminderCount(0);
        event.setMaxReminders(Math.min(definition.getMaxReminders(), MAX_REMINDERS_CAP));
        event.setNextAttemptAt(eventTime);
        eventRepository.save(event);
    }

    private NotificationTopic parseTopic(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException("topic is required");
        }
        try {
            return NotificationTopic.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ValidationException("Invalid topic: " + raw);
        }
    }

    private UserNotificationPreference ensureUserSettings(String username) {
        ensureUserExists(username);

        UserNotificationPreference pref = preferenceRepository.findById(username)
            .orElseGet(() -> {
                UserNotificationPreference created = defaultPreference(username);
                return preferenceRepository.save(created);
            });

        for (NotificationTopic topic : NotificationTopic.values()) {
            subscriptionRepository.findByUsernameAndTopicCode(username, topic)
                .orElseGet(() -> {
                    NotificationSubscription sub = new NotificationSubscription();
                    sub.setUsername(username);
                    sub.setTopicCode(topic);
                    sub.setSubscribed(true);
                    return subscriptionRepository.save(sub);
                });
        }

        return pref;
    }

    private UserNotificationPreference defaultPreference(String username) {
        UserNotificationPreference created = new UserNotificationPreference();
        created.setUsername(username);
        created.setDndStart(LocalTime.of(21, 0));
        created.setDndEnd(LocalTime.of(7, 0));
        created.setMaxRemindersPerEvent(3);
        return created;
    }

    private List<NotificationSubscription> defaultSubscriptions(String username) {
        List<NotificationSubscription> defaults = new ArrayList<>();
        for (NotificationTopic topic : NotificationTopic.values()) {
            NotificationSubscription sub = new NotificationSubscription();
            sub.setUsername(username);
            sub.setTopicCode(topic);
            sub.setSubscribed(true);
            defaults.add(sub);
        }
        return defaults;
    }

    private void ensureUserExists(String username) {
        if (userAccountRepository.findByUsername(username).isEmpty()) {
            throw new ValidationException("Unknown user: " + username);
        }
    }

    private boolean isSubscribed(String username, NotificationTopic topic) {
        return subscriptionRepository.findByUsernameAndTopicCode(username, topic)
            .map(NotificationSubscription::isSubscribed)
            .orElse(true);
    }

    private boolean isWithinDnd(UserNotificationPreference pref, Instant nowInstant) {
        return NotificationDeliveryPolicy.isWithinDnd(pref.getDndStart(), pref.getDndEnd(), nowInstant);
    }

    private Instant nextOutsideDnd(UserNotificationPreference pref, Instant nowInstant) {
        return NotificationDeliveryPolicy.nextOutsideDnd(pref.getDndEnd(), nowInstant);
    }

    private String applyTemplate(String template, Map<String, String> payload) {
        String rendered = template;
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String writePayload(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new ValidationException("Failed to encode notification payload");
        }
    }

    private Map<String, String> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private NotificationResponse toResponse(NotificationMessage message) {
        return new NotificationResponse(
            message.getId(),
            message.getTopicCode().name(),
            message.getTitle(),
            message.getBody(),
            message.getSentAt(),
            message.getReadAt(),
            message.getStatus().name()
        );
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new ValidationException("page must be greater than or equal to 0");
        }
    }

    private void validatePageSize(int size) {
        if (size < 1 || size > 100) {
            throw new ValidationException("size must be between 1 and 100");
        }
    }

    private LocalTime parseLocalTime(String value, String field) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Invalid time for " + field + ". Expected HH:mm or HH:mm:ss");
        }
    }

    private Instant parseInstant(String value, String field, boolean endOfMinute) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime local = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            Instant instant = local.atZone(ZoneId.systemDefault()).toInstant();
            return endOfMinute ? instant.plusSeconds(59) : instant;
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Invalid datetime for " + field);
        }
    }

    private String toDisplayTime(Instant instant) {
        return DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(instant);
    }
}
