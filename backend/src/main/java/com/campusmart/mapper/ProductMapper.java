package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmart.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    
    @Select("SELECT p.*, c.name AS category_name, u.nickname AS seller_nickname, u.avatar AS seller_avatar " +
            "FROM tb_product p " +
            "LEFT JOIN tb_category c ON p.category_id = c.id " +
            "LEFT JOIN tb_user u ON p.user_id = u.id " +
            "WHERE p.id = #{id}")
    Product selectProductDetail(@Param("id") Long id);
    
    @Select("SELECT p.*, c.name AS category_name, u.nickname AS seller_nickname, u.avatar AS seller_avatar " +
            "FROM tb_product p " +
            "LEFT JOIN tb_category c ON p.category_id = c.id " +
            "LEFT JOIN tb_user u ON p.user_id = u.id " +
            "WHERE p.status = 1 " +
            "ORDER BY p.create_time DESC")
    IPage<Product> selectApprovedProductPage(Page<Product> page);
    
    @Select("<script>" +
            "SELECT p.*, c.name AS category_name, u.nickname AS seller_nickname, u.avatar AS seller_avatar " +
            "FROM tb_product p " +
            "LEFT JOIN tb_category c ON p.category_id = c.id " +
            "LEFT JOIN tb_user u ON p.user_id = u.id " +
            "WHERE p.status = 1 " +
            "<if test='categoryId != null'> AND p.category_id = #{categoryId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR p.description LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='minPrice != null'> AND p.price &gt;= #{minPrice}</if>" +
            "<if test='maxPrice != null'> AND p.price &lt;= #{maxPrice}</if>" +
            "<if test='conditionLevel != null and conditionLevel != \"\"'> AND p.condition_level = #{conditionLevel}</if>" +
            "ORDER BY p.create_time DESC" +
            "</script>")
    IPage<Product> selectProductPageByConditions(Page<Product> page,
                                                   @Param("categoryId") Long categoryId,
                                                   @Param("keyword") String keyword,
                                                   @Param("minPrice") Double minPrice,
                                                   @Param("maxPrice") Double maxPrice,
                                                   @Param("conditionLevel") String conditionLevel);
    
    @Select("SELECT p.*, c.name AS category_name, u.nickname AS seller_nickname, u.avatar AS seller_avatar " +
            "FROM tb_product p " +
            "LEFT JOIN tb_category c ON p.category_id = c.id " +
            "LEFT JOIN tb_user u ON p.user_id = u.id " +
            "WHERE p.user_id = #{userId} " +
            "ORDER BY p.create_time DESC")
    IPage<Product> selectMyProducts(Page<Product> page, @Param("userId") Long userId);

    @org.apache.ibatis.annotations.Update("UPDATE tb_product SET stock = stock - #{quantity}, sales = sales + #{sales} WHERE id = #{productId}")
    void updateStockAndSales(@Param("productId") Long productId, @Param("quantity") Integer quantity, @Param("sales") Integer sales);

    @org.apache.ibatis.annotations.Update("UPDATE tb_product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
    void deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
