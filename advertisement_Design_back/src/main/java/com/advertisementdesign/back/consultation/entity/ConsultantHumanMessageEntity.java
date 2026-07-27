package com.advertisementdesign.back.consultation.entity;

import com.advertisementdesign.back.communication.enums.MessageSenderRole;
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
@TableName("consultant_human_message")
public class ConsultantHumanMessageEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String humanChatId;
    private Long senderId;
    private MessageSenderRole senderRole;
    private String content;
    private LocalDateTime createdAt;
}
