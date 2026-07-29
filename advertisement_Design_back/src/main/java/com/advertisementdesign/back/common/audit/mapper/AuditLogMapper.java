package com.advertisementdesign.back.common.audit.mapper;

import com.advertisementdesign.back.common.audit.entity.AuditLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
}
