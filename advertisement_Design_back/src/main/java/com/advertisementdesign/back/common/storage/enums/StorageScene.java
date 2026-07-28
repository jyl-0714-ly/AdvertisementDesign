package com.advertisementdesign.back.common.storage.enums;

import lombok.Getter;

@Getter
public enum StorageScene {
    GENERAL_PRIVATE(StorageVisibility.PRIVATE, "general"),
    PORTFOLIO_COVER_PUBLIC(StorageVisibility.PUBLIC, "portfolio/covers"),
    PORTFOLIO_DETAIL_PUBLIC(StorageVisibility.PUBLIC, "portfolio/details"),
    USER_AVATAR_PUBLIC(StorageVisibility.PUBLIC, "avatars"),
    CONSULTATION_ATTACHMENT(StorageVisibility.PRIVATE, "consultations/attachments"),
    CONVERSATION_IMAGE(StorageVisibility.PRIVATE, "conversations/images"),
    CONVERSATION_ATTACHMENT(StorageVisibility.PRIVATE, "conversations/attachments"),
    PROJECT_CONTRACT(StorageVisibility.PRIVATE, "projects/contracts"),
    PROJECT_REQUIREMENT_MATERIAL(StorageVisibility.PRIVATE, "projects/requirements"),
    PROJECT_REPORT(StorageVisibility.PRIVATE, "projects/reports"),
    PROJECT_DRAFT(StorageVisibility.PRIVATE, "projects/drafts"),
    PROJECT_FINAL(StorageVisibility.PRIVATE, "projects/finals"),
    PROJECT_DELIVERABLE(StorageVisibility.PRIVATE, "projects/deliverables"),
    PROJECT_OTHER(StorageVisibility.PRIVATE, "projects/other");

    private final StorageVisibility visibility;
    private final String keySegment;

    StorageScene(StorageVisibility visibility, String keySegment) {
        this.visibility = visibility;
        this.keySegment = keySegment;
    }

    public boolean isPublic() {
        return visibility == StorageVisibility.PUBLIC;
    }
}
