package com.meridian.portal.notification.repository;

import com.meridian.portal.notification.domain.NotificationTemplate;
import com.meridian.portal.notification.domain.NotificationTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, NotificationTopic> {
}
