package com.campusmart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String images;
    private Long categoryId;
    private Long userId;
    private Integer status;
    private String reason;
    private Integer viewCount;
    private Integer stock;
    private String conditionLevel;
    private String location;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 扩展字段（非数据库字段）
    @TableField(exist = false)
    private String categoryName;
    
    @TableField(exist = false)
    private String sellerNickname;
    
    @TableField(exist = false)
    private String sellerAvatar;
}
