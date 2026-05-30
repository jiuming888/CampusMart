package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusmart.entity.OrderItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT * FROM tb_order_item WHERE order_id = #{orderId}")
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    @Delete("<script>" +
            "DELETE FROM tb_cart WHERE user_id = #{userId} AND product_id IN " +
            "(SELECT product_id FROM tb_order_item WHERE order_id = #{orderId})" +
            "</script>")
    void deleteCartItemsByOrderId(@Param("orderId") Long orderId, @Param("userId") Long userId);
}
