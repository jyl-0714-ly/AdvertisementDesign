package com.advertisementdesign.back.identity.service;

import com.advertisementdesign.back.identity.model.ActorRef;

import java.util.Optional;

public interface CurrentActorProvider {
    CurrentActor requireCurrentActor();

    default Optional<CurrentActor> currentActor() {
        try {
            return Optional.of(requireCurrentActor());
        } catch (com.advertisementdesign.back.common.exception.ApiException exception) {
            if (exception.getCode() == 401) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    record CurrentActor(ActorRef actor, String displayName) {
    }
}
