package com.campusmart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_cart")
public class Cart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    // 扩展字段
    @TableField(exist = false)
    private Product product;
}
