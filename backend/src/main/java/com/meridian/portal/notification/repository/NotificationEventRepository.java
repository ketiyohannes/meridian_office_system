package com.meridian.portal.notification.repository;

import com.meridian.portal.notification.domain.NotificationEvent;
import com.meridian.portal.notification.domain.NotificationEventStatus;
import com.meridian.portal.notification.domain.NotificationTopic;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    Optional<NotificationEvent> findByEventKeyAndUsernameAndTopicCode(String eventKey, String username, NotificationTopic topicCode);

    List<NotificationEvent> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
        NotificationEventStatus status,
        Instant nextAttemptAt
    );
}
