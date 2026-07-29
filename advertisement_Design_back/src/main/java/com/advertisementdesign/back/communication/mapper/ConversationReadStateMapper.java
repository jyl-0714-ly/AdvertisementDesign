package com.advertisementdesign.back.communication.mapper;

import com.advertisementdesign.back.communication.entity.ConversationReadStateEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ConversationReadStateMapper extends BaseMapper<ConversationReadStateEntity> {
    @Insert("""
            INSERT INTO conversation_read_state
                (conversation_id, user_id, last_read_message_id, last_read_at, unread_count, updated_at)
            VALUES
                (#{conversationId}, #{userId}, #{lastReadMessageId}, #{lastReadAt},
                 (SELECT COUNT(*)
                  FROM message
                  WHERE conversation_id = #{conversationId}
                    AND id > #{lastReadMessageId}
                    AND NOT (actor_type IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER')
                             AND actor_id = #{userId})),
                 #{updatedAt})
            ON DUPLICATE KEY UPDATE
                unread_count = IF(
                    VALUES(last_read_message_id) > COALESCE(last_read_message_id, 0),
                    VALUES(unread_count),
                    unread_count),
                last_read_at = IF(
                    VALUES(last_read_message_id) > COALESCE(last_read_message_id, 0),
                    VALUES(last_read_at),
                    last_read_at),
                updated_at = IF(
                    VALUES(last_read_message_id) > COALESCE(last_read_message_id, 0),
                    VALUES(updated_at),
                    updated_at),
                last_read_message_id = GREATEST(
                    COALESCE(last_read_message_id, 0),
                    VALUES(last_read_message_id))
            """)
    int resetReadState(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("lastReadAt") LocalDateTime lastReadAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Insert("""
            INSERT INTO conversation_read_state
                (conversation_id, user_id, unread_count, updated_at)
            VALUES
                (#{conversationId}, #{userId}, 1, #{updatedAt})
            ON DUPLICATE KEY UPDATE
                unread_count = unread_count + 1,
                updated_at = VALUES(updated_at)
            """)
    int incrementUnreadCount(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE conversation_read_state
            SET unread_count = (
                    SELECT COUNT(*)
                    FROM message
                    WHERE conversation_id = #{conversationId}
                      AND id > COALESCE(conversation_read_state.last_read_message_id, 0)
                      AND is_deleted = 0
                      AND (sender_id IS NULL OR sender_id <> #{userId})),
                updated_at = #{updatedAt}
            WHERE conversation_id = #{conversationId}
              AND user_id = #{userId}
            """)
    int refreshUnreadCount(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("updatedAt") LocalDateTime updatedAt);
}
