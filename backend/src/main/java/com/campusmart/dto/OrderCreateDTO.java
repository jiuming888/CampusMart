package com.campusmart.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderCreateDTO {
    private List<Long> cartItemIds;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    // 直接购买时的参数
    private Long productId;
    private Integer quantity;
}
