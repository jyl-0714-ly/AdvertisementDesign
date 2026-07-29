package com.advertisementdesign.back.artifact.entity;

import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ConfirmationResult;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ConfirmationType;
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

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName(value = "artifact_confirmation", autoResultMap = true)
public class ArtifactConfirmationEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long artifactId;
    private Long artifactVersionId;
    private Integer artifactVersionNumber;
    private ConfirmationType confirmationType;
    private ConfirmationResult result;
    private ActorRef.ActorType actorType;
    private Long actorId;
    private Long customerMemberId;
    @TableField(typeHandler = JacksonTypeHandler.class) private Map<String, Object> authorizationBasis;
    private Long objectVersion;
    private String comment;
    private String idempotencyKey;
    private LocalDateTime confirmedAt;
}
