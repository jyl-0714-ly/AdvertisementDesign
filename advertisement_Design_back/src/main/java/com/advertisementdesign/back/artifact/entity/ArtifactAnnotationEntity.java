package com.advertisementdesign.back.artifact.entity;

import com.advertisementdesign.back.artifact.enums.ArtifactEnums.AnnotationType;
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
@TableName(value = "artifact_annotation", autoResultMap = true)
public class ArtifactAnnotationEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long artifactVersionId;
    private Long fileAssetId;
    private AnnotationType annotationType;
    @TableField(typeHandler = JacksonTypeHandler.class) private Map<String, Object> geometry;
    private String content;
    private ActorRef.ActorType actorType;
    private Long actorId;
    private LocalDateTime createdAt;
}
