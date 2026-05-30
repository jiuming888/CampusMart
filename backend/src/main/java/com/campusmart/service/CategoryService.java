package com.campusmart.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.entity.Category;
import com.campusmart.mapper.CategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    public List<Category> getAllCategories() {
        return this.lambdaQuery()
                .orderByAsc(Category::getSort)
                .list();
    }

    public Category addCategory(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new RuntimeException("分类名称不能为空");
        }
        // 检查同名分类
        Long count = this.lambdaQuery()
                .eq(Category::getName, category.getName().trim())
                .count();
        if (count > 0) {
            throw new RuntimeException("分类名称已存在");
        }
        category.setName(category.getName().trim());
        this.save(category);
        return category;
    }

    public Category updateCategory(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new RuntimeException("分类名称不能为空");
        }
        // 检查同名分类（排除自身）
        Long count = this.lambdaQuery()
                .eq(Category::getName, category.getName().trim())
                .ne(Category::getId, category.getId())
                .count();
        if (count > 0) {
            throw new RuntimeException("分类名称已存在");
        }
        category.setName(category.getName().trim());
        this.updateById(category);
        return category;
    }

    public boolean deleteCategory(Long id) {
        try {
            return this.removeById(id);
        } catch (DataIntegrityViolationException e) {
            logger.warn("删除分类失败，分类下有商品: id={}", id);
            throw new RuntimeException("该分类下存在商品，无法删除");
        }
    }
}
