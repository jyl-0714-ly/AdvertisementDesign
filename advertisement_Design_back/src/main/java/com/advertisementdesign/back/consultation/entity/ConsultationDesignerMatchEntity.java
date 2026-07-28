package com.advertisementdesign.back.consultation.entity;

import com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus;
import com.advertisementdesign.back.consultation.enums.ConsultationMatchType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "consultation_designer_match", autoResultMap = true)
public class ConsultationDesignerMatchEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long consultantIntakeId;
    private Long designerId;
    private Integer attemptNo;
    private ConsultationMatchType matchType;
    private ConsultationMatchStatus status;
    private BigDecimal activityScore;
    private BigDecimal workloadScore;
    private BigDecimal specialtyScore;
    private BigDecimal fairnessScore;
    private BigDecimal totalScore;
    private BigDecimal requirementConfidence;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> scoreDetail;
    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime cancelledAt;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
