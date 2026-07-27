package com.advertisementdesign.back.common.audit.entity;

import com.advertisementdesign.back.communication.enums.MessageSenderRole;
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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "operation_log", autoResultMap = true)
public class OperationLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private MessageSenderRole operatorRole;
    private String bizType;
    private Long bizId;
    private String action;
    private String description;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> beforeData;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> afterData;
    private LocalDateTime createdAt;
}
