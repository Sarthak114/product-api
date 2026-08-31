package com.zestindia.productapi.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Async("auditExecutor")
    public void record(String actor, String action, String resource, String details) {
        log.info("AUDIT actor={} action={} resource={} details={}", actor, action, resource, details);
    }
}
