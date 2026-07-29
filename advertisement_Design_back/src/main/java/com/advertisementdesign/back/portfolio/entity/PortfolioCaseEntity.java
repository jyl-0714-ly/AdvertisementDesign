package com.advertisementdesign.back.portfolio.entity;

import com.advertisementdesign.back.portfolio.enums.PortfolioCategory;
import com.advertisementdesign.back.portfolio.enums.PortfolioStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("portfolio_case")
public class PortfolioCaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private PortfolioCategory category;
    private String industry;
    private String style;
    private String serviceType;
    private Long coverFileId;
    private String description;
    private Integer sortOrder;
    private Boolean featured;
    private PortfolioStatus status;
    private LocalDateTime publishedAt;
    @Version private Long version;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
