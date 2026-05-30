package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.common.Constants;
import com.campusmart.dto.ProductDTO;
import com.campusmart.entity.Product;
import com.campusmart.mapper.ProductMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

@Service
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    @Autowired
    private ProductMapper productMapper;

    @Value("${app.upload.path:../public/images/}")
    private String uploadPath;
    
    public IPage<Product> getProductList(Integer page, Integer pageSize, Long categoryId, 
                                        String keyword, Double minPrice, Double maxPrice, 
                                        String conditionLevel) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        return productMapper.selectProductPageByConditions(pageParam, categoryId, keyword, 
                minPrice, maxPrice, conditionLevel);
    }
    
    public Product getProductDetail(Long id) {
        Product product = productMapper.selectProductDetail(id);
        if (product != null) {
            // 增加浏览次数
            Product updateProduct = new Product();
            updateProduct.setId(id);
            updateProduct.setViewCount(product.getViewCount() + 1);
            this.updateById(updateProduct);
        }
        return product;
    }
    
    @Transactional
    public Product publishProduct(ProductDTO productDTO, Long userId) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setOriginalPrice(productDTO.getOriginalPrice());
        product.setImages(productDTO.getImages());
        product.setCategoryId(productDTO.getCategoryId());
        product.setUserId(userId);
        product.setStatus(Constants.PRODUCT_PENDING);
        product.setViewCount(0);
        product.setStock(productDTO.getStock() != null && productDTO.getStock() > 0 ? productDTO.getStock() : 1);
        product.setConditionLevel(productDTO.getConditionLevel());
        product.setLocation(productDTO.getLocation());
        
        this.save(product);
        return product;
    }
    
    @Transactional
    public Product updateProduct(Long productId, ProductDTO productDTO, Long userId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此商品");
        }
        
        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setName(productDTO.getName());
        updateProduct.setDescription(productDTO.getDescription());
        updateProduct.setPrice(productDTO.getPrice());
        updateProduct.setOriginalPrice(productDTO.getOriginalPrice());
        updateProduct.setImages(productDTO.getImages());
        updateProduct.setCategoryId(productDTO.getCategoryId());
        updateProduct.setConditionLevel(productDTO.getConditionLevel());
        updateProduct.setLocation(productDTO.getLocation());
        updateProduct.setStatus(Constants.PRODUCT_PENDING);
        
        this.updateById(updateProduct);
        return this.getById(productId);
    }
    
    public boolean deleteProduct(Long productId, Long userId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此商品");
        }
        deleteImageFiles(product.getImages());
        return this.removeById(productId);
    }
    
    public IPage<Product> getMyProducts(Long userId, Integer page, Integer pageSize) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        return productMapper.selectMyProducts(pageParam, userId);
    }
    
    // ==================== 管理员功能 ====================
    
    public IPage<Product> getPendingProducts(Integer page, Integer pageSize) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, Constants.PRODUCT_PENDING);
        wrapper.orderByDesc(Product::getCreateTime);
        return this.page(pageParam, wrapper);
    }
    
    @Transactional
    public Product approveProduct(Long productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setStatus(Constants.PRODUCT_APPROVED);
        this.updateById(updateProduct);
        return this.getById(productId);
    }
    
    @Transactional
    public Product rejectProduct(Long productId, String reason) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setStatus(Constants.PRODUCT_REJECTED);
        updateProduct.setReason(reason);
        this.updateById(updateProduct);
        return this.getById(productId);
    }
    
    public IPage<Product> getAllProducts(Integer page, Integer pageSize, Integer status) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return this.page(pageParam, wrapper);
    }
    
    @Transactional
    public Product updateProductStatus(Long productId, Integer status) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setStatus(status);
        this.updateById(updateProduct);
        return this.getById(productId);
    }
    
    // 管理员删除商品（不需要用户验证）
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        deleteImageFiles(product.getImages());
        this.removeById(productId);
    }

    private void deleteImageFiles(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode images = mapper.readTree(imagesJson);
            for (JsonNode img : images) {
                String url = img.has("url") ? img.get("url").asText() : img.asText();
                String filePath = urlToFilePath(url);
                if (filePath != null) {
                    File file = new File(uploadPath, filePath);
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            // 图片删除失败不影响商品删除
        }
    }

    private String urlToFilePath(String url) {
        // /images/products/uuid.jpg → products/uuid.jpg
        if (url != null && url.startsWith("/images/")) {
            return url.substring("/images/".length());
        }
        return null;
    }

    @Transactional
    public Product updateStock(Long productId, Integer stock) {
        if (stock == null || stock < 0) {
            throw new RuntimeException("库存不能为负数");
        }
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setStock(stock);
        this.updateById(updateProduct);
        return this.getById(productId);
    }
    
    public List<Product> getHotProducts(int limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, Constants.PRODUCT_APPROVED);
        wrapper.orderByDesc(Product::getViewCount);
        wrapper.last("LIMIT " + limit);
        return this.list(wrapper);
    }
    
    public List<Product> getLatestProducts(int limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, Constants.PRODUCT_APPROVED);
        wrapper.orderByDesc(Product::getCreateTime);
        wrapper.last("LIMIT " + limit);
        return this.list(wrapper);
    }
    
    // 根据用户ID获取商品（用于卖家商品页面）
    public List<Product> getProductsByUser(Long userId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getUserId, userId);
        wrapper.eq(Product::getStatus, Constants.PRODUCT_APPROVED);
        wrapper.orderByDesc(Product::getCreateTime);
        return this.list(wrapper);
    }
}
