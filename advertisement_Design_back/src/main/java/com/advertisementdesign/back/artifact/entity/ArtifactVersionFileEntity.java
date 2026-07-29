package com.advertisementdesign.back.artifact.entity;

import com.advertisementdesign.back.artifact.enums.ArtifactEnums.FileRole;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("artifact_version_file")
public class ArtifactVersionFileEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long artifactVersionId;
    private Long fileAssetId;
    private FileRole fileRole;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
