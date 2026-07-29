package com.advertisementdesign.back.communication.mapper;

import com.advertisementdesign.back.communication.entity.MessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {
    @Select("""
            SELECT *
            FROM message
            WHERE conversation_id = #{conversationId}
            ORDER BY sent_at DESC, id DESC
            LIMIT 1
            """)
    MessageEntity selectLatestByConversationId(@Param("conversationId") Long conversationId);
}
