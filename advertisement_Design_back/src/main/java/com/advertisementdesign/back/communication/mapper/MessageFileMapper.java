package com.advertisementdesign.back.communication.mapper;

import com.advertisementdesign.back.communication.entity.MessageFileEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageFileMapper extends BaseMapper<MessageFileEntity> {
    @Select("""
            SELECT COUNT(*)
            FROM message_file mf
            INNER JOIN message m ON m.id = mf.message_id
            WHERE mf.file_id = #{fileId}
              AND m.conversation_id = #{conversationId}
              AND m.is_deleted = 0
            """)
    long countByFileAndConversation(
            @Param("fileId") Long fileId,
            @Param("conversationId") Long conversationId);

    @Select("""
            SELECT COUNT(*)
            FROM message_file mf
            INNER JOIN message m ON m.id = mf.message_id
            INNER JOIN conversation c ON c.id = m.conversation_id
            WHERE mf.file_id = #{fileId}
              AND m.is_deleted = 0
              AND (c.customer_id = #{userId} OR c.designer_id = #{userId})
            """)
    long countAccessibleByUser(
            @Param("fileId") Long fileId,
            @Param("userId") Long userId);
}
