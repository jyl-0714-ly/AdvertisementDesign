package com.advertisementdesign.back.common.audit.mapper;

import com.advertisementdesign.back.common.audit.entity.OperationLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}
