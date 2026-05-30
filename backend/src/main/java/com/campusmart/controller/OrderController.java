package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.dto.OrderCreateDTO;
import com.campusmart.entity.Order;
import com.campusmart.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/create")
    public Result<?> createOrder(HttpServletRequest request,
                                @Valid @RequestBody OrderCreateDTO orderCreateDTO) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            Order order = orderService.createOrder(userId, orderCreateDTO);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/list")
    public Result<?> getOrderList(HttpServletRequest request,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(required = false) String status) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(orderService.getOrderList(userId, page, pageSize, status));
    }
    
    @GetMapping("/detail/{id}")
    public Result<?> getOrderDetail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            return Result.success(orderService.getOrderDetail(id, userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/cancel/{id}")
    public Result<?> cancelOrder(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            return Result.success(orderService.cancelOrder(id, userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/pay/{id}")
    public Result<?> payOrder(HttpServletRequest request, @PathVariable Long id) {
        try {
            orderService.payOrder(id, "MANUAL_PAY_" + System.currentTimeMillis());
            return Result.success("支付成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/confirm/{id}")
    public Result<?> confirmReceive(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            return Result.success(orderService.confirmReceive(id, userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Order> getOrderById(HttpServletRequest request, @PathVariable Long id) {
        try {
            Order order = orderService.getById(id);
            if (order == null) {
                return Result.error("订单不存在");
            }
            return Result.success(order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/by-no")
    public Result<?> getByOrderNo(HttpServletRequest request, @RequestParam String orderNo) {
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 模拟支付接口（用于测试）
     * 当支付宝沙箱不可用时，可以使用此接口模拟支付成功
     */
    @PostMapping("/mock-pay")
    public Result<?> mockPay(HttpServletRequest request, @RequestParam Long orderId) {
        try {
            orderService.payOrder(orderId, "MOCK_TRADE_" + System.currentTimeMillis());
            return Result.success("模拟支付成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> deleteOrder(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            orderService.deleteOrder(id, userId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete-all")
    public Result<?> deleteAllOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        try {
            orderService.deleteAllOrders(userId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
