package com.meridian.portal.notification.repository;

import com.meridian.portal.notification.domain.NotificationSubscription;
import com.meridian.portal.notification.domain.NotificationTopic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {
    List<NotificationSubscription> findByUsernameOrderByTopicCodeAsc(String username);

    Optional<NotificationSubscription> findByUsernameAndTopicCode(String username, NotificationTopic topicCode);
}
