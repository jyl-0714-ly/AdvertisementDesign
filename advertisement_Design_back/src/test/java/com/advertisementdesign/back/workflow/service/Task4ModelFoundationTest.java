package com.advertisementdesign.back.workflow.service;

import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.communication.model.ConversationModels;
import com.advertisementdesign.back.project.enums.ProjectNameSource;
import com.advertisementdesign.back.project.enums.ProjectStatus;
import com.advertisementdesign.back.project.model.ProjectModels;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Task4ModelFoundationTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void fixedCatalogCreatesExactlySevenNotStartedStagesInBusinessOrder() {
        FixedStageCatalog catalog = new FixedStageCatalog();
        LocalDateTime now = LocalDateTime.of(2026, 7, 29, 10, 0);

        var definitions = catalog.definitions();
        var instances = catalog.createInitialInstances(101L, now);

        assertEquals(List.of(
                StageCode.REQUIREMENT_GUIDE,
                StageCode.CONTRACT_PREPAYMENT,
                StageCode.RESEARCH_REPORT,
                StageCode.SKETCH_STYLE,
                StageCode.REVIEW_FINAL,
                StageCode.DELIVERY_FINAL_PAYMENT,
                StageCode.AFTER_SALE_REPURCHASE), definitions.stream().map(definition -> definition.code()).toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7),
                definitions.stream().map(definition -> definition.sortOrder()).toList());
        assertTrue(definitions.stream().allMatch(definition -> definition.initialStatus() == StageStatus.NOT_STARTED));
        assertEquals(7, instances.size());
        assertTrue(instances.stream().allMatch(instance -> instance.getStatus() == StageStatus.NOT_STARTED));
        assertTrue(instances.stream().allMatch(instance -> Integer.valueOf(0).equals(instance.getActivationCount())));
    }

    @Test
    void customerProjectionOmitsInternalMessageFieldsAndSummaryRemainsStructurallyReduced() {
        var customerMessage = new ConversationModels.CustomerMessageView(
                1L, 2L, MessageType.TEXT, "需求内容", "客户甲", null, null, List.of(), LocalDateTime.now());
        JsonNode messageJson = objectMapper.valueToTree(customerMessage);

        assertFalse(messageJson.has("actor"));
        assertFalse(messageJson.has("actorId"));
        assertFalse(messageJson.has("actorType"));
        assertFalse(messageJson.has("sendSource"));
        assertFalse(messageJson.has("authorizationBasis"));
        assertFalse(messageJson.has("clientMessageId"));
        assertFalse(messageJson.has("requestId"));

        LocalDateTime now = LocalDateTime.now();
        var summary = new ProjectModels.ProjectSummaryView(9L, "项目", ProjectStatus.ACTIVE, now, now);
        var full = new ProjectModels.ProjectFullDetailView(
                9L, 20L, "项目", ProjectNameSource.MANUAL, "私有需求", ProjectStatus.ACTIVE,
                30L, now, null, null, null, 4L, now, now);
        JsonNode summaryJson = objectMapper.valueToTree(summary);
        JsonNode fullJson = objectMapper.valueToTree(full);

        assertNotEquals(ProjectModels.ProjectSummaryView.class, ProjectModels.ProjectFullDetailView.class);
        assertFalse(summaryJson.has("organizationId"));
        assertFalse(summaryJson.has("description"));
        assertFalse(summaryJson.has("confirmedRequirementVersionId"));
        assertFalse(summaryJson.has("version"));
        assertTrue(fullJson.has("organizationId"));
        assertTrue(fullJson.has("description"));
        assertTrue(fullJson.has("version"));
    }
}
