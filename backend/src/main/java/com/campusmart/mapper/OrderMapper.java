package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmart.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    @Select("SELECT * FROM tb_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    IPage<Order> selectOrderPageByUserId(Page<Order> page, @Param("userId") Long userId);

    @Select("SELECT * FROM tb_order WHERE order_no = #{orderNo} LIMIT 1")
    Order selectByOrderNo(@Param("orderNo") String orderNo);
}
