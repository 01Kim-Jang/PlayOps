package com.playops.api.repository;

import com.playops.api.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {
    List<UserAuditLog> findTop200ByOrderByCreatedAtDesc();
}
