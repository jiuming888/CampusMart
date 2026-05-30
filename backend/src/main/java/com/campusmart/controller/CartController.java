package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.entity.Cart;
import com.campusmart.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    @Autowired
    private CartService cartService;
    
    @GetMapping("/list")
    public Result<List<Cart>> getCartList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(cartService.getCartList(userId));
    }
    
    @PostMapping("/add")
    public Result<?> addToCart(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        Long productId = Long.parseLong(params.get("productId").toString());
        Integer quantity = params.containsKey("quantity") ? 
                Integer.parseInt(params.get("quantity").toString()) : 1;
        
        try {
            Cart cart = cartService.addToCart(userId, productId, quantity);
            return Result.success(cart);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/update/{id}")
    public Result<?> updateCartQuantity(HttpServletRequest request,
                                       @PathVariable Long id,
                                       @RequestBody Map<String, Integer> params) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        Integer quantity = params.get("quantity");
        
        try {
            Cart cart = cartService.updateCartQuantity(id, userId, quantity);
            return Result.success(cart);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/delete/{id}")
    public Result<?> removeFromCart(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            boolean success = cartService.removeFromCart(id, userId);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/clear")
    public Result<?> clearCart(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        cartService.clearCart(userId);
        return Result.success("购物车已清空");
    }
}
