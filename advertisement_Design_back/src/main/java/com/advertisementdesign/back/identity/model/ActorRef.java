package com.advertisementdesign.back.identity.model;

import java.util.Objects;

public record ActorRef(ActorType type, Long actorId) {
    public ActorRef {
        Objects.requireNonNull(type, "type");
        if (type == ActorType.SYSTEM_EVENT && actorId != null) {
            throw new IllegalArgumentException("SYSTEM_EVENT actorId must be null");
        }
        if (type != ActorType.SYSTEM_EVENT && actorId == null) {
            throw new IllegalArgumentException("actorId is required for non-system actors");
        }
    }

    public enum ActorType {
        CUSTOMER_USER,
        DESIGNER_USER,
        ADMIN_USER,
        COORDINATOR_AGENT,
        STAGE_AGENT,
        SYSTEM_EVENT
    }
}
