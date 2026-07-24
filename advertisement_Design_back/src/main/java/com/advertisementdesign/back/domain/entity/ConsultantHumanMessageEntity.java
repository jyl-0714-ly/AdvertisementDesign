package com.advertisementdesign.back.domain.entity;

import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultantHumanMessageEntity {
    private Long id;
    private String humanChatId;
    private Long senderId;
    private MessageSenderRole senderRole;
    private String content;
    private LocalDateTime createdAt;
}
