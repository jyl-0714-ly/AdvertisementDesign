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
}
