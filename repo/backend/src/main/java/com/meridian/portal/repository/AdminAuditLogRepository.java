package com.meridian.portal.repository;

import com.meridian.portal.domain.AdminAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long>, JpaSpecificationExecutor<AdminAuditLog> {

    @Query("select distinct a.action from AdminAuditLog a order by a.action")
    List<String> findDistinctActions();

    @Query("select distinct a.targetType from AdminAuditLog a order by a.targetType")
    List<String> findDistinctTargetTypes();
}
