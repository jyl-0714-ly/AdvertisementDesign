package com.advertisementdesign.back.domain.entity;

import com.advertisementdesign.back.domain.enums.ProjectStatus;
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
    private String description;
    private String currentStage;
    private ProjectStatus status;
    private Integer progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
