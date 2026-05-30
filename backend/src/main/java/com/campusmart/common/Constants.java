package com.campusmart.common;

public class Constants {
    // 用户角色
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    
    // 商品状态
    public static final Integer PRODUCT_PENDING = 0;   // 待审核
    public static final Integer PRODUCT_APPROVED = 1;  // 已上架
    public static final Integer PRODUCT_OFFLINE = 2;   // 已下架
    public static final Integer PRODUCT_REJECTED = 3;  // 审核拒绝
    
    // 订单状态
    public static final String ORDER_PENDING = "PENDING";      // 待付款
    public static final String ORDER_PAID = "PAID";            // 已付款
    public static final String ORDER_SHIPPED = "SHIPPED";      // 已发货
    public static final String ORDER_COMPLETED = "COMPLETED";  // 已完成
    public static final String ORDER_CANCELLED = "CANCELLED";  // 已取消
    
    // JWT
    public static final String JWT_TOKEN = "token";
    public static final String JWT_USER_ID = "userId";
    public static final String JWT_ROLE = "role";
}
