package com.meridian.portal.notification.repository;

import com.meridian.portal.notification.domain.NotificationMessage;
import com.meridian.portal.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
    Page<NotificationMessage> findByUsernameOrderBySentAtDesc(String username, Pageable pageable);

    Optional<NotificationMessage> findByIdAndUsername(Long id, String username);

    long countByUsernameAndStatus(String username, NotificationStatus status);

    @Modifying
    @Query("""
        update NotificationMessage n
           set n.status = :status, n.readAt = :readAt
         where n.username = :username
           and n.status <> :status
        """)
    int markAllRead(@Param("username") String username, @Param("status") NotificationStatus status, @Param("readAt") Instant readAt);

    @Modifying
    @Query("delete from NotificationMessage n where n.sentAt < :cutoff")
    int deleteBySentAtBefore(@Param("cutoff") Instant cutoff);
}
