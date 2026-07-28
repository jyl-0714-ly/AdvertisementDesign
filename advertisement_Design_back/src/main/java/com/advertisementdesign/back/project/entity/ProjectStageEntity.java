package com.advertisementdesign.back.project.entity;

import com.advertisementdesign.back.project.enums.ProjectStageStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("project_stage")
public class ProjectStageEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String stageCode;
    private String stageName;
    private Integer sortOrder;
    private Boolean isRequired;
    private ProjectStageStatus status;
    private LocalDateTime reachedAt;
    private LocalDateTime updatedAt;
}
