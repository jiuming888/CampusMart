package com.campusmart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.entity.Category;
import com.campusmart.entity.Order;
import com.campusmart.entity.OrderItem;
import com.campusmart.entity.Product;
import com.campusmart.entity.User;
import com.campusmart.mapper.CategoryMapper;
import com.campusmart.mapper.OrderItemMapper;
import com.campusmart.mapper.OrderMapper;
import com.campusmart.mapper.ProductMapper;
import com.campusmart.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 仪表盘统计数据
     */
    @GetMapping("/stats")
    public Result<?> getStats() {
        long totalUsers = userMapper.selectCount(null);

        List<User> allUsers = userMapper.selectList(null);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date weekAgo = cal.getTime();
        LocalDateTime weekAgoTime = dateToLocalDateTime(weekAgo);
        long newUsersThisWeek = allUsers.stream()
            .filter(u -> u.getCreateTime() != null && u.getCreateTime().isAfter(weekAgoTime))
            .count();

        long totalProducts = productMapper.selectCount(null);

        long pendingProducts = productMapper.selectCount(
            new LambdaQueryWrapper<Product>().eq(Product::getStatus, Constants.PRODUCT_PENDING)
        );

        long totalOrders = orderMapper.selectCount(null);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Date todayStart = today.getTime();
        LocalDateTime todayStartTime = dateToLocalDateTime(todayStart);

        long todayOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, todayStartTime)
        );

        List<Order> todayOrdersList = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStartTime)
                .in(Order::getStatus, Constants.ORDER_PAID, Constants.ORDER_SHIPPED, Constants.ORDER_COMPLETED)
        );
        BigDecimal todaySales = todayOrdersList.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSales = orderMapper.selectList(
            new LambdaQueryWrapper<Order>().in(Order::getStatus,
                Constants.ORDER_PAID, Constants.ORDER_SHIPPED, Constants.ORDER_COMPLETED)
        ).stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>().eq(Order::getStatus, Constants.ORDER_PENDING)
        );

        long disabledUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getStatus, 0)
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("newUsersThisWeek", newUsersThisWeek);
        stats.put("totalProducts", totalProducts);
        stats.put("pendingProducts", pendingProducts);
        stats.put("totalOrders", totalOrders);
        stats.put("todayOrders", todayOrders);
        stats.put("todaySales", todaySales);
        stats.put("totalSales", totalSales);
        stats.put("pendingOrders", pendingOrders);
        stats.put("disabledUsers", disabledUsers);

        return Result.success(stats);
    }

    /**
     * 近7天每日订单数量
     */
    @GetMapping("/orders/week")
    public Result<?> getOrdersWeek() {
        List<Map<String, Object>> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date dayStart = cal.getTime();
            LocalDateTime dayStartTime = dateToLocalDateTime(dayStart);

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date dayEnd = cal.getTime();
            LocalDateTime dayEndTime = dateToLocalDateTime(dayEnd);

            long count = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, dayStartTime)
                    .le(Order::getCreateTime, dayEndTime)
            );

            Map<String, Object> day = new HashMap<>();
            day.put("date", new java.text.SimpleDateFormat("MM-dd").format(dayStart));
            day.put("count", count);
            result.add(day);
        }

        return Result.success(result);
    }

    /**
     * 近7天每日销售额
     */
    @GetMapping("/sales/week")
    public Result<?> getSalesWeek() {
        List<Map<String, Object>> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date dayStart = cal.getTime();
            LocalDateTime dayStartTime = dateToLocalDateTime(dayStart);

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date dayEnd = cal.getTime();
            LocalDateTime dayEndTime = dateToLocalDateTime(dayEnd);

            BigDecimal amount = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, dayStartTime)
                    .le(Order::getCreateTime, dayEndTime)
                    .in(Order::getStatus, Constants.ORDER_PAID, Constants.ORDER_SHIPPED, Constants.ORDER_COMPLETED)
            ).stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> day = new HashMap<>();
            day.put("date", new java.text.SimpleDateFormat("MM-dd").format(dayStart));
            day.put("amount", amount);
            result.add(day);
        }

        return Result.success(result);
    }

    /**
     * 各分类商品数量
     */
    @GetMapping("/products/by-category")
    public Result<?> getProductsByCategory() {
        List<Category> categories = categoryMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Category cat : categories) {
            long count = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, cat.getId())
            );
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", cat.getName());
                item.put("value", count);
                result.add(item);
            }
        }

        return Result.success(result);
    }

    /**
     * 订单状态分布
     */
    @GetMapping("/orders/by-status")
    public Result<?> getOrdersByStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] statusNames = {"待付款", "已付款", "已发货", "已完成", "已取消"};
        String[] statuses = {
            Constants.ORDER_PENDING,
            Constants.ORDER_PAID,
            Constants.ORDER_SHIPPED,
            Constants.ORDER_COMPLETED,
            Constants.ORDER_CANCELLED
        };

        for (int i = 0; i < statuses.length; i++) {
            long count = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, statuses[i])
            );
            Map<String, Object> item = new HashMap<>();
            item.put("name", statusNames[i]);
            item.put("value", count);
            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 近7天新增用户
     */
    @GetMapping("/users/week")
    public Result<?> getUsersWeek() {
        List<Map<String, Object>> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        List<User> allUsers = userMapper.selectList(null);

        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date dayStart = cal.getTime();
            LocalDateTime dayStartTime = dateToLocalDateTime(dayStart);

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date dayEnd = cal.getTime();
            LocalDateTime dayEndTime = dateToLocalDateTime(dayEnd);

            long count = allUsers.stream()
                .filter(u -> u.getCreateTime() != null
                    && u.getCreateTime().isAfter(dayStartTime)
                    && u.getCreateTime().isBefore(dayEndTime))
                .count();

            Map<String, Object> day = new HashMap<>();
            day.put("date", new java.text.SimpleDateFormat("MM-dd").format(dayStart));
            day.put("count", count);
            result.add(day);
        }

        return Result.success(result);
    }

    /**
     * 商品状态分布
     */
    @GetMapping("/products/by-product-status")
    public Result<?> getProductsByProductStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] names = {"待审核", "已上架", "已下架", "审核拒绝"};
        Integer[] statuses = {
            Constants.PRODUCT_PENDING,
            Constants.PRODUCT_APPROVED,
            Constants.PRODUCT_OFFLINE,
            Constants.PRODUCT_REJECTED
        };

        for (int i = 0; i < statuses.length; i++) {
            long count = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, statuses[i])
            );
            Map<String, Object> item = new HashMap<>();
            item.put("name", names[i]);
            item.put("value", count);
            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 热门商品 TOP 10
     */
    @GetMapping("/products/hot")
    public Result<?> getHotProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, Constants.PRODUCT_APPROVED)
               .orderByDesc(Product::getViewCount)
               .last("LIMIT 10");
        List<Product> products = productMapper.selectList(wrapper);

        List<Map<String, Object>> result = products.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("viewCount", p.getViewCount());
            item.put("price", p.getPrice());
            item.put("status", p.getStatus());
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 近期待处理订单
     */
    @GetMapping("/orders/recent")
    public Result<?> getRecentOrders() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, Constants.ORDER_PENDING, Constants.ORDER_PAID, Constants.ORDER_SHIPPED)
               .orderByDesc(Order::getCreateTime)
               .last("LIMIT 10");
        List<Order> orders = orderMapper.selectList(wrapper);

        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", o.getId());
            item.put("orderNo", o.getOrderNo());
            item.put("totalAmount", o.getTotalAmount());
            item.put("status", o.getStatus());
            item.put("createTime", o.getCreateTime());

            User buyer = userMapper.selectById(o.getUserId());
            if (buyer != null) {
                item.put("buyerName", buyer.getNickname() != null ? buyer.getNickname() : buyer.getUsername());
            }

            long itemCount = orderItemMapper.selectCount(
                new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, o.getId())
            );
            item.put("itemCount", itemCount);

            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}
