package com.advertisementdesign.back.artifact.entity;

import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ArtifactStatus;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ArtifactType;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("artifact")
public class ArtifactEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long stageInstanceId;
    private ArtifactType artifactType;
    private String title;
    private ArtifactStatus status;
    private Integer latestVersionNumber;
    @Version private Long version;
    private ActorRef.ActorType createdByActorType;
    private Long createdByActorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
