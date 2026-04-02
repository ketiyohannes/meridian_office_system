package com.meridian.portal.notification.repository;

import com.meridian.portal.notification.domain.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, String> {
}
