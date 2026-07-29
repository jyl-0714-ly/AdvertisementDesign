package com.advertisementdesign.back.common.audit.entity;

import com.advertisementdesign.back.identity.model.ActorRef;
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
@TableName(value = "audit_log", autoResultMap = true)
public class AuditLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private ActorRef.ActorType actorType;
    private Long actorId;
    private String customerDisplayIdentity;
    private Source source;
    private String objectType;
    private Long objectId;
    private String objectVersion;
    private String action;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> authorizationBasis;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> beforeState;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> afterState;
    private Result result;
    private String failureCode;
    private String requestId;
    private String correlationId;
    private LocalDateTime occurredAt;

    public enum Source { CUSTOMER_UI, DESIGNER_UI, ADMIN_UI, AUTOMATION, EXTERNAL_EVENT, SYSTEM }
    public enum Result { SUCCESS, REJECTED, FAILED }
}
