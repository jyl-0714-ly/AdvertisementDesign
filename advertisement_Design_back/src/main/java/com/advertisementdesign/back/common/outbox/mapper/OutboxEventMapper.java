package com.advertisementdesign.back.common.outbox.mapper;

import com.advertisementdesign.back.common.outbox.entity.OutboxEventEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventEntity> {
}
