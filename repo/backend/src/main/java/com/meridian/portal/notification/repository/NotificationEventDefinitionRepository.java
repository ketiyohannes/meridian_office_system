package com.meridian.portal.notification.repository;

import com.meridian.portal.notification.domain.NotificationEventDefinition;
import com.meridian.portal.notification.domain.NotificationTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventDefinitionRepository extends JpaRepository<NotificationEventDefinition, NotificationTopic> {
}
