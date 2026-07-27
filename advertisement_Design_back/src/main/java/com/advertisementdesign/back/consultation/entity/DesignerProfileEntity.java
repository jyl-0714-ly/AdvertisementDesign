package com.advertisementdesign.back.consultation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "designer_profile", autoResultMap = true)
public class DesignerProfileEntity {
    @TableId
    private Long designerId;
    private Boolean enabled;
    private Boolean online;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> specialties;
}
