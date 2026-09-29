package com.safari.wildlife.service;

import com.safari.wildlife.entity.AuditLog;
import com.safari.wildlife.entity.User;
import com.safari.wildlife.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog logAction(User user, String actionType, String entityName, String entityId, String details, String ipAddress) {
        AuditLog log = new AuditLog(user, actionType, entityName, entityId, details, ipAddress);
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
}
