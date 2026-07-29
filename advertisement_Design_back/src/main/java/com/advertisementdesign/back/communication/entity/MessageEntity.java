package com.advertisementdesign.back.communication.entity;

import com.advertisementdesign.back.communication.enums.MessageSendSource;
import com.advertisementdesign.back.communication.enums.MessageType;
import com.advertisementdesign.back.identity.model.ActorRef;
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
@TableName("message")
public class MessageEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private MessageType messageType;
    private String content;
    private String customerDisplayIdentity;
    private ActorRef.ActorType actorType;
    private Long actorId;
    private MessageSendSource sendSource;
    private String authorizationBasis;
    private Long replyToMessageId;
    private Long correctionMessageId;
    private String clientMessageId;
    private LocalDateTime sentAt;
}
