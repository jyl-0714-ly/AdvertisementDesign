package com.advertisementdesign.back.common.idempotency.entity;

import com.advertisementdesign.back.identity.model.ActorRef;
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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "idempotency_record", autoResultMap = true)
public class IdempotencyRecordEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operationType;
    private ActorRef.ActorType actorType;
    private Long actorId;
    private String idempotencyKey;
    private String requestHash;
    private Status status;
    private String resourceType;
    private Long resourceId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> responseSnapshot;
    private String failureCode;
    private LocalDateTime expiresAt;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        PROCESSING,
        SUCCEEDED,
        FAILED
    }
}
