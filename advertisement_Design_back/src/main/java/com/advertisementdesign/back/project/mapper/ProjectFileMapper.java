package com.advertisementdesign.back.project.mapper;

import com.advertisementdesign.back.project.entity.ProjectFileEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectFileMapper extends BaseMapper<ProjectFileEntity> {
    @Select("""
            SELECT COUNT(*)
            FROM project_file pf
            INNER JOIN project p ON p.id = pf.project_id
            WHERE pf.file_id = #{fileId}
              AND (p.customer_id = #{userId} OR p.designer_id = #{userId})
            """)
    long countAccessibleByUser(
            @Param("fileId") Long fileId,
            @Param("userId") Long userId);
}
