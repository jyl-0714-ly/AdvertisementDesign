package com.advertisementdesign.back.project.entity;

import com.advertisementdesign.back.project.enums.ProjectTransferStatus;
import com.advertisementdesign.back.project.enums.ProjectTransferType;
import com.advertisementdesign.back.project.enums.TransferApprovalStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("project_designer_transfer")
public class ProjectDesignerTransferEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long conversationId;
    private Long requirementVersionId;
    private Long fromDesignerId;
    private Long toDesignerId;
    private ProjectTransferType transferType;
    private String reasonCode;
    private String reasonDescription;
    private String handoverSummary;
    private Integer attemptNo;
    private ProjectTransferStatus status;
    private TransferApprovalStatus approvalStatus;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String approvalNote;
    private LocalDateTime requestedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
