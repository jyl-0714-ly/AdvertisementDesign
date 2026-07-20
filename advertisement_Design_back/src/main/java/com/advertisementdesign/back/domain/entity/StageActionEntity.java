package com.advertisementdesign.back.domain.entity;

import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.advertisementdesign.back.domain.enums.StageActionStatus;
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
@TableName("stage_action")
public class StageActionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long projectStageId;
    private String stageCode;
    private Long initiatorId;
    private MessageSenderRole initiatorRole;
    private Long confirmUserId;
    private StageActionStatus status;
    private String requestNote;
    private String responseNote;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
