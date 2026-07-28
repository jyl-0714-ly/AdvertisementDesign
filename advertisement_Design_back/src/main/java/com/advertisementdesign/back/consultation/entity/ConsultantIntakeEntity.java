package com.advertisementdesign.back.consultation.entity;

import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
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

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "consultant_intake", autoResultMap = true)
public class ConsultantIntakeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String projectType;
    private String industry;
    private String requirementDescription;
    private String budgetRange;
    private String projectCycle;
    private ConsultantIntakeStatus status;
    private Long matchedDesignerId;
    private Long initialDesignerId;
    private LocalDateTime designerAssignedAt;
    private LocalDateTime designerAcknowledgedAt;
    private Integer matchAttemptCount;
    private LocalDateTime handoffConfirmedAt;
    private LocalDateTime nextMatchAt;
    private LocalDateTime closedAt;
    private String closeReason;
    private LocalDateTime contractConfirmedAt;
    private LocalDateTime initialPaymentConfirmedAt;
    private String humanChatId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> greetingMessages;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
