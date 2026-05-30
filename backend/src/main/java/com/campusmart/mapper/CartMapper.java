package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusmart.entity.Cart;
import com.campusmart.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
    
    @Select("SELECT c.*, p.name AS product_name, p.price, p.images, p.stock, p.status " +
            "FROM tb_cart c " +
            "LEFT JOIN tb_product p ON c.product_id = p.id " +
            "WHERE c.user_id = #{userId}")
    List<Cart> selectCartWithProduct(@Param("userId") Long userId);
    
    @Select("SELECT id, name, description, price, original_price AS originalPrice, " +
            "images, category_id AS categoryId, user_id AS userId, status, reason, " +
            "view_count AS viewCount, stock, condition_level AS conditionLevel, location, " +
            "create_time AS createTime, update_time AS updateTime " +
            "FROM tb_product WHERE id = #{productId}")
    Product selectProductById(@Param("productId") Long productId);
}
