package com.advertisementdesign.back.domain.entity;

import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("operation_log")
public class OperationLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private MessageSenderRole operatorRole;
    private String bizType;
    private Long bizId;
    private String action;
    private String description;
    private Map<String, Object> beforeData;
    private Map<String, Object> afterData;
    private LocalDateTime createdAt;
}
