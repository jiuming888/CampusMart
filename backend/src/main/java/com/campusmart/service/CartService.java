package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.entity.Cart;
import com.campusmart.entity.Product;
import com.campusmart.mapper.CartMapper;
import com.campusmart.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService extends ServiceImpl<CartMapper, Cart> {
    
    @Autowired
    private ProductMapper productMapper;
    
    public List<Cart> getCartList(Long userId) {
        List<Cart> carts = this.lambdaQuery()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime)
                .list();
        
        // 关联查询商品信息
        for (Cart cart : carts) {
            try {
                Product dbProduct = productMapper.selectProductDetail(cart.getProductId());
                if (dbProduct != null) {
                    cart.setProduct(dbProduct);
                } else {
                    // 商品不存在，创建一个标记对象
                    Product placeholder = new Product();
                    placeholder.setId(cart.getProductId());
                    placeholder.setName("商品已下架");
                    placeholder.setStatus(4); // 标记为不存在
                    cart.setProduct(placeholder);
                }
            } catch (Exception e) {
                // 查询失败，添加占位符
                Product placeholder = new Product();
                placeholder.setId(cart.getProductId());
                placeholder.setName("商品信息加载失败");
                placeholder.setStatus(4);
                cart.setProduct(placeholder);
            }
        }
        
        return carts;
    }
    
    @Transactional
    public Cart addToCart(Long userId, Long productId, Integer quantity) {
        // 检查商品是否存在且已上架
        Product product = productMapper.selectProductDetail(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getStatus() != 1) {
            throw new RuntimeException("商品未上架，无法加入购物车");
        }
        
        // 检查是否已存在
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .eq(Cart::getProductId, productId);
        Cart existingCart = this.getOne(wrapper);
        
        if (existingCart != null) {
            existingCart.setQuantity(existingCart.getQuantity() + quantity);
            this.updateById(existingCart);
            existingCart.setProduct(product);
            return existingCart;
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            this.save(cart);
            cart.setProduct(product);
            return cart;
        }
    }
    
    @Transactional
    public Cart updateCartQuantity(Long cartId, Long userId, Integer quantity) {
        Cart cart = this.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new RuntimeException("购物车项不存在");
        }
        cart.setQuantity(quantity);
        this.updateById(cart);
        return cart;
    }
    
    @Transactional
    public boolean removeFromCart(Long cartId, Long userId) {
        Cart cart = this.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new RuntimeException("购物车项不存在");
        }
        return this.removeById(cartId);
    }
    
    @Transactional
    public void clearCart(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        this.remove(wrapper);
    }
}
