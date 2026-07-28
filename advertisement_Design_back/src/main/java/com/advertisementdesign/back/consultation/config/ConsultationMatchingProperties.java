package com.advertisementdesign.back.consultation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.consultation")
public record ConsultationMatchingProperties(
        Duration matchingDelay,
        Duration acknowledgementTimeout,
        Duration schedulerDelay) {

    public ConsultationMatchingProperties {
        matchingDelay = matchingDelay == null ? Duration.ofSeconds(3) : matchingDelay;
        acknowledgementTimeout = acknowledgementTimeout == null
                ? Duration.ofSeconds(60) : acknowledgementTimeout;
        schedulerDelay = schedulerDelay == null ? Duration.ofMillis(500) : schedulerDelay;
    }
}
