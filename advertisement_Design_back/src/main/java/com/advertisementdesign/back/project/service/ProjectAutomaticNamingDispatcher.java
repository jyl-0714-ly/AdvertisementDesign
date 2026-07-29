package com.advertisementdesign.back.project.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Publishes an automatic-name request from the transaction that confirms a stable requirement version.
 * The listener deliberately runs only after that transaction commits.
 */
@Service
public class ProjectAutomaticNamingDispatcher {
    private final ApplicationEventPublisher eventPublisher;

    public ProjectAutomaticNamingDispatcher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void requestAfterCommit(Long projectId, Long expectedVersion, String generatedName) {
        eventPublisher.publishEvent(new Requested(projectId, expectedVersion, generatedName));
    }

    public record Requested(Long projectId, Long expectedVersion, String generatedName) {
    }
}
