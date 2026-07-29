package com.advertisementdesign.back.artifact.entity;

import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ApprovalDecision;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("artifact_approval")
public class ArtifactApprovalEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long artifactVersionId;
    private ApprovalDecision decision;
    private Long reviewerUserId;
    private Long assignmentId;
    private String comment;
    private String requestId;
    private LocalDateTime decidedAt;
}
