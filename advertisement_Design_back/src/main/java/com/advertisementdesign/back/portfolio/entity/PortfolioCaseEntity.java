package com.advertisementdesign.back.portfolio.entity;

import com.advertisementdesign.back.portfolio.enums.PortfolioCategory;
import com.advertisementdesign.back.portfolio.enums.PortfolioStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "portfolio_case", autoResultMap = true)
public class PortfolioCaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private PortfolioCategory category;
    private String industry;
    private String style;
    private String serviceType;
    private String coverUrl;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls;
    private String description;
    private Integer sortOrder;
    private Boolean featured;
    private PortfolioStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
