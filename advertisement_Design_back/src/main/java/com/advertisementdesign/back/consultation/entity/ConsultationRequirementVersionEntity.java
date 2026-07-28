package com.advertisementdesign.back.consultation.entity;

import com.advertisementdesign.back.consultation.enums.RequirementVersionStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "consultation_requirement_version", autoResultMap = true)
public class ConsultationRequirementVersionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long consultantIntakeId;
    private Long conversationId;
    private Integer versionNo;
    private String projectType;
    private String industry;
    private String requirementDescription;
    private String budgetRange;
    private String projectCycle;
    private String usageScenario;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> deliverables;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> specifications;
    private String referenceDescription;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> confirmedItems;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> pendingItems;
    private String designerSummary;
    private RequirementVersionStatus status;
    private Long createdByDesignerId;
    private LocalDateTime submittedAt;
    private LocalDateTime customerConfirmedAt;
    private LocalDateTime customerRejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
