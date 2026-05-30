package com.campusmart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("tb_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private String tradeNo;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 扩展字段
    @TableField(exist = false)
    private List<OrderItem> items;

    // 存储创建订单时的购物车项ID，用于支付成功后删除
    @TableField(exist = false)
    private List<Long> cartItemIds;
}
