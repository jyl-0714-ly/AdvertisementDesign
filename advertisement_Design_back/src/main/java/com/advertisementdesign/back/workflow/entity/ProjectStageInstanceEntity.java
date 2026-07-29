package com.advertisementdesign.back.workflow.entity;

import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("project_stage_instance")
public class ProjectStageInstanceEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private StageCode stageCode;
    private String stageName;
    private Integer sortOrder;
    @Builder.Default
    private StageStatus status = StageStatus.NOT_STARTED;
    @Builder.Default
    private Integer activationCount = 0;
    private LocalDateTime activatedAt;
    private LocalDateTime completedAt;
    @Version
    @Builder.Default
    private Long version = 0L;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
