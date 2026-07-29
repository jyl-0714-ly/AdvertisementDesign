package com.advertisementdesign.back.communication.repository;

import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.advertisementdesign.back.communication.mapper.ConversationMapper;
import com.advertisementdesign.back.communication.mapper.ConversationReadStateMapper;
import com.advertisementdesign.back.communication.mapper.MessageAttachmentMapper;
import com.advertisementdesign.back.communication.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommunicationRepositoryTest {
    @Mock private ConversationMapper conversationMapper;
    @Mock private MessageMapper messageMapper;
    @Mock private MessageAttachmentMapper attachmentMapper;
    @Mock private ConversationReadStateMapper readStateMapper;

    @Test
    void persistedMessageCannotBeModifiedThroughAppendBoundary() {
        CommunicationRepository repository = new CommunicationRepository(
                conversationMapper, messageMapper, attachmentMapper, readStateMapper);
        MessageEntity persisted = MessageEntity.builder()
                .id(90L)
                .conversationId(88L)
                .content("已发送内容")
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> repository.appendMessage(persisted, List.of()));

        verify(messageMapper, never()).insert(persisted);
    }
}
