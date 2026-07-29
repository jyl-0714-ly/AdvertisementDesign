package com.advertisementdesign.back.artifact.entity;

import com.advertisementdesign.back.artifact.enums.ArtifactEnums.PublicationStatus;
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

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName(value = "artifact_version", autoResultMap = true)
public class ArtifactVersionEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long artifactId;
    private Long projectId;
    private Integer versionNumber;
    private Long parentVersionId;
    @TableField(typeHandler = JacksonTypeHandler.class) private Map<String, Object> content;
    private String contentHash;
    private PublicationStatus publicationStatus;
    private Boolean generated;
    private ActorRef.ActorType createdByActorType;
    private Long createdByActorId;
    private Long publishedByUserId;
    @Version private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
