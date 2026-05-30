package com.campusmart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.entity.Order;
import com.campusmart.entity.OrderItem;
import com.campusmart.entity.User;
import com.campusmart.mapper.OrderItemMapper;
import com.campusmart.mapper.OrderMapper;
import com.campusmart.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取有已支付订单的用户列表（包含订单统计）
     */
    @GetMapping("/users")
    public Result<?> getUsersWithPaidOrders() {
        // 查询所有有已支付或待付款订单的用户
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, Constants.ORDER_PENDING)
                .or()
                .eq(Order::getStatus, Constants.ORDER_PAID)
                .or()
                .eq(Order::getStatus, Constants.ORDER_SHIPPED)
                .or()
                .eq(Order::getStatus, Constants.ORDER_COMPLETED);
        orderWrapper.select(Order::getUserId);
        orderWrapper.groupBy(Order::getUserId);

        List<Order> orders = orderMapper.selectList(orderWrapper);

        // 去重获取用户ID
        List<Long> userIds = orders.stream().map(Order::getUserId).distinct().collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        // 查询用户信息
        List<User> users = userMapper.selectBatchIds(userIds);

        // 为每个用户统计订单数量和总金额
        LambdaQueryWrapper<Order> statWrapper = new LambdaQueryWrapper<>();
        statWrapper.eq(Order::getStatus, Constants.ORDER_PENDING)
                .or().eq(Order::getStatus, Constants.ORDER_PAID)
                .or().eq(Order::getStatus, Constants.ORDER_SHIPPED)
                .or().eq(Order::getStatus, Constants.ORDER_COMPLETED);

        List<Order> allPaidOrders = orderMapper.selectList(statWrapper);

        // 按用户分组统计
        Map<Long, Long> orderCountMap = allPaidOrders.stream()
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));
        Map<Long, java.math.BigDecimal> totalAmountMap = allPaidOrders.stream()
                .collect(Collectors.groupingBy(Order::getUserId,
                        Collectors.reducing(java.math.BigDecimal.ZERO, Order::getTotalAmount, java.math.BigDecimal::add)));

        // 组合用户信息和统计
        List<Map<String, Object>> result = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("avatar", user.getAvatar());
            map.put("orderCount", orderCountMap.getOrDefault(user.getId(), 0L));
            map.put("totalAmount", totalAmountMap.getOrDefault(user.getId(), java.math.BigDecimal.ZERO));
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取指定用户的已支付订单列表
     */
    @GetMapping("/user/{userId}")
    public Result<?> getUserOrders(@PathVariable Long userId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Order> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .eq(Order::getStatus, Constants.ORDER_PENDING)
                .or()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, Constants.ORDER_PAID)
                .or()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, Constants.ORDER_SHIPPED)
                .or()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, Constants.ORDER_COMPLETED);
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(pageParam, wrapper);

        // 填充订单项
        for (Order order : orderPage.getRecords()) {
            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            order.setItems(items);
        }

        // 查询用户信息
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", orderPage.getRecords());
        result.put("total", orderPage.getTotal());
        result.put("user", user);

        return Result.success(result);
    }
}
