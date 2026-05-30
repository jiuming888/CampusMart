package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.entity.Product;
import com.campusmart.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/pending")
    public Result<?> getPendingProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(productService.getPendingProducts(page, pageSize));
    }
    
    @PutMapping("/approve/{id}")
    public Result<?> approveProduct(@PathVariable Long id) {
        try {
            Product product = productService.approveProduct(id);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/reject/{id}")
    public Result<?> rejectProduct(@PathVariable Long id, @RequestBody(required = false) String reason) {
        try {
            Product product = productService.rejectProduct(id, reason);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/all")
    public Result<?> getAllProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(productService.getAllProducts(page, pageSize, status));
    }
    
    @PutMapping("/status/{id}")
    public Result<?> updateProductStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            Product product = productService.updateProductStatus(id, status);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return Result.success("商品删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/stock/{id}")
    public Result<?> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        try {
            Product product = productService.updateStock(id, stock);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
