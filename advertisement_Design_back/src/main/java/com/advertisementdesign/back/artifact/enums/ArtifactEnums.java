package com.advertisementdesign.back.artifact.enums;

public final class ArtifactEnums {
    private ArtifactEnums() {}

    public enum ArtifactType { REQUIREMENT, RESEARCH_REPORT, SKETCH, FORMAL_DESIGN, DELIVERY }
    public enum ArtifactStatus { DRAFT, UNDER_REVIEW, PUBLISHED, SUPERSEDED, ARCHIVED }
    public enum PublicationStatus { DRAFT, PUBLISHED, WITHDRAWN }
    public enum ApprovalDecision { APPROVED, REJECTED }
    public enum ConfirmationType { REQUIREMENT, REPORT, SKETCH, FORMAL_DESIGN, DELIVERY_RECEIPT }
    public enum ConfirmationResult { CONFIRMED, REJECTED }
    public enum FileRole { PRIMARY, SOURCE, PREVIEW, SUPPLEMENT, DELIVERY }
    public enum AnnotationType { POINT, RECTANGLE, FREEHAND, TEXT }
}
