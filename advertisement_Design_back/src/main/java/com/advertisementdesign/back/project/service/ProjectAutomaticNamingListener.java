package com.advertisementdesign.back.project.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class ProjectAutomaticNamingListener {
    private final ProjectNamingService namingService;

    public ProjectAutomaticNamingListener(ProjectNamingService namingService) {
        this.namingService = namingService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterRequirementCommit(ProjectAutomaticNamingDispatcher.Requested event) {
        try {
            namingService.applyAutomaticName(
                    event.projectId(), event.expectedVersion(), event.generatedName());
        } catch (RuntimeException exception) {
            log.warn("Automatic project naming failed after commit: projectId={}, expectedVersion={}",
                    event.projectId(), event.expectedVersion(), exception);
        }
    }
}
