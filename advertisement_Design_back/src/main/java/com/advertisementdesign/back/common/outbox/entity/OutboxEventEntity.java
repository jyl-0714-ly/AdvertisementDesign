package com.advertisementdesign.back.common.outbox.entity;

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
@TableName(value = "outbox_event", autoResultMap = true)
public class OutboxEventEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String eventKey;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> payload;
    private Status status;
    private LocalDateTime availableAt;
    private LocalDateTime publishedAt;
    private Integer retryCount;
    private String lastErrorCode;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status { PENDING, PUBLISHING, PUBLISHED, FAILED, DEAD }
}
