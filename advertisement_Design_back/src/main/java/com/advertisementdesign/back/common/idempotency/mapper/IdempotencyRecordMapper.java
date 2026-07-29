package com.advertisementdesign.back.common.idempotency.mapper;

import com.advertisementdesign.back.common.idempotency.entity.IdempotencyRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecordEntity> {
}
