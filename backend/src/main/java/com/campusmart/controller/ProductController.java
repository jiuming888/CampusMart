package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.dto.ProductDTO;
import com.campusmart.entity.Product;
import com.campusmart.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/list")
    public Result<?> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String conditionLevel) {
        return Result.success(productService.getProductList(page, pageSize, categoryId, 
                keyword, minPrice, maxPrice, conditionLevel));
    }
    
    @GetMapping("/detail/{id}")
    public Result<?> getProductDetail(@PathVariable Long id) {
        Product product = productService.getProductDetail(id);
        return Result.success(product);
    }
    
    @PostMapping("/publish")
    public Result<?> publishProduct(HttpServletRequest request, 
                                   @RequestBody ProductDTO productDTO) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            Product product = productService.publishProduct(productDTO, userId);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/update/{id}")
    public Result<?> updateProduct(HttpServletRequest request,
                                   @PathVariable Long id,
                                   @RequestBody ProductDTO productDTO) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            Product product = productService.updateProduct(id, productDTO, userId);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteProduct(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            boolean success = productService.deleteProduct(id, userId);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/my")
    public Result<?> getMyProducts(HttpServletRequest request,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(productService.getMyProducts(userId, page, pageSize));
    }
    
    @GetMapping("/hot")
    public Result<?> getHotProducts(@RequestParam(defaultValue = "6") Integer limit) {
        return Result.success(productService.getHotProducts(limit));
    }
    
    @GetMapping("/latest")
    public Result<?> getLatestProducts(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(productService.getLatestProducts(limit));
    }
    
    @GetMapping("/user/{userId}")
    public Result<?> getProductsByUser(@PathVariable Long userId) {
        return Result.success(productService.getProductsByUser(userId));
    }
}
