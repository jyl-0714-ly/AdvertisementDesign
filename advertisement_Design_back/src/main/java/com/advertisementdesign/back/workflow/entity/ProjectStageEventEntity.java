package com.advertisementdesign.back.workflow.entity;

import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.enums.StageEventSource;
import com.advertisementdesign.back.workflow.enums.StageEventType;
import com.advertisementdesign.back.workflow.enums.StageStatus;
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
@TableName("project_stage_event")
public class ProjectStageEventEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long stageInstanceId;
    private StageCode stageCode;
    private StageEventType eventType;
    private StageStatus fromStatus;
    private StageStatus toStatus;
    private Integer activationNumber;
    private String relatedObjectType;
    private Long relatedObjectId;
    private Integer relatedObjectVersion;
    private ActorRef.ActorType actorType;
    private Long actorId;
    private StageEventSource source;
    private String authorizationBasis;
    private String reason;
    private String requestId;
    private LocalDateTime occurredAt;
}
