package com.advertisementdesign.back.project.entity;

import com.advertisementdesign.back.project.enums.ProjectStatus;
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
@TableName("project")
public class ProjectEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long customerId;
    private Long designerId;
    private Long initialDesignerId;
    private Long consultantIntakeId;
    private Long confirmedRequirementVersionId;
    private String description;
    private String currentStage;
    private ProjectStatus status;
    private Integer progress;
    private LocalDateTime preparedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
