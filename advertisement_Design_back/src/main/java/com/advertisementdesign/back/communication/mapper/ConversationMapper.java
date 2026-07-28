package com.advertisementdesign.back.communication.mapper;

import com.advertisementdesign.back.communication.entity.ConversationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationMapper extends BaseMapper<ConversationEntity> {
    @Select("SELECT * FROM conversation WHERE id = #{id} FOR UPDATE")
    ConversationEntity selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM conversation WHERE consultant_intake_id = #{consultantIntakeId} FOR UPDATE")
    ConversationEntity selectByConsultantIntakeIdForUpdate(
            @Param("consultantIntakeId") Long consultantIntakeId);

    @Select("""
            SELECT c.*
            FROM conversation c
            INNER JOIN message m ON m.conversation_id = c.id AND m.is_deleted = 0
            INNER JOIN message_file mf ON mf.message_id = m.id
            WHERE mf.file_id = #{fileId}
            LIMIT 1
            """)
    ConversationEntity selectByAttachedFileId(@Param("fileId") Long fileId);
}
