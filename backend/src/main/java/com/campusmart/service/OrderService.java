package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.common.Constants;
import com.campusmart.dto.OrderCreateDTO;
import com.campusmart.entity.*;
import com.campusmart.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {
    
    @Autowired
    private OrderItemMapper orderItemMapper;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private AddressService addressService;
    
    @Transactional
    public Order createOrder(Long userId, OrderCreateDTO orderCreateDTO) {
        List<Cart> cartItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        // 获取购物车项
        if (orderCreateDTO.getCartItemIds() != null && !orderCreateDTO.getCartItemIds().isEmpty()) {
            for (Long cartId : orderCreateDTO.getCartItemIds()) {
                Cart cart = cartService.getById(cartId);
                if (cart == null || !cart.getUserId().equals(userId)) {
                    throw new RuntimeException("购物车项不存在");
                }
                // 使用 Mapper 查询完整的商品信息
                Product product = productMapper.selectProductDetail(cart.getProductId());
                if (product == null || product.getStatus() != Constants.PRODUCT_APPROVED) {
                    throw new RuntimeException("商品已下架或不存在");
                }
                cart.setProduct(product);
                cartItems.add(cart);
                totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(cart.getQuantity())));
            }
        } else if (orderCreateDTO.getProductId() != null) {
            // 直接购买
            Product product = productMapper.selectProductDetail(orderCreateDTO.getProductId());
            if (product == null || product.getStatus() != Constants.PRODUCT_APPROVED) {
                throw new RuntimeException("商品已下架或不存在");
            }
            Cart cart = new Cart();
            cart.setProductId(product.getId());
            cart.setQuantity(orderCreateDTO.getQuantity() != null ? orderCreateDTO.getQuantity() : 1);
            cart.setProduct(product);
            cartItems.add(cart);
            totalAmount = product.getPrice().multiply(new BigDecimal(cart.getQuantity()));
        }
        
        // 获取收货地址
        String receiverName, receiverPhone, receiverAddress;
        if (orderCreateDTO.getAddressId() != null) {
            Address address = addressService.getById(orderCreateDTO.getAddressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new RuntimeException("收货地址不存在");
            }
            receiverName = address.getReceiverName();
            receiverPhone = address.getPhone();
            receiverAddress = address.getFullAddress();
        } else {
            receiverName = orderCreateDTO.getReceiverName();
            receiverPhone = orderCreateDTO.getReceiverPhone();
            receiverAddress = orderCreateDTO.getReceiverAddress();
        }
        
        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(Constants.ORDER_PENDING);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setRemark(orderCreateDTO.getRemark());
        
        this.save(order);
        
        // 创建订单项
        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cart : cartItems) {
            Product product = cart.getProduct();
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getImages());
            item.setPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            orderItemMapper.insert(item);
            orderItems.add(item);
        }
        order.setItems(orderItems);

        // 保存购物车项ID，支付成功后再删除
        if (orderCreateDTO.getCartItemIds() != null && !orderCreateDTO.getCartItemIds().isEmpty()) {
            order.setCartItemIds(orderCreateDTO.getCartItemIds());
        }

        return order;
    }
    
    public Page<Order> getOrderList(Long userId, Integer page, Integer pageSize, String status) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> orderPage = this.page(pageParam, wrapper);
        
        // 填充订单项
        for (Order order : orderPage.getRecords()) {
            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            order.setItems(items);
        }
        
        return orderPage;
    }
    
    public Order getById(Long id) {
        return baseMapper.selectById(id);
    }

    public Order getOrderDetail(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        order.setItems(items);
        return order;
    }
    
    @Transactional
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!Constants.ORDER_PENDING.equals(order.getStatus())) {
            throw new RuntimeException("只有待付款的订单可以取消");
        }
        
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(Constants.ORDER_CANCELLED);
        this.updateById(updateOrder);
        return this.getById(orderId);
    }
    
    @Transactional
    public Order payOrder(Long orderId, String tradeNo) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Constants.ORDER_PENDING.equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        // 删除购物车项（支付成功后才删除）
        if (order.getCartItemIds() != null && !order.getCartItemIds().isEmpty()) {
            cartService.removeByIds(order.getCartItemIds());
        }

        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(Constants.ORDER_PAID);
        updateOrder.setTradeNo(tradeNo);
        this.updateById(updateOrder);

        // 扣减库存（先加载订单子项）
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            productMapper.deductStock(item.getProductId(), item.getQuantity());
        }

        return this.getById(orderId);
    }

    public Order getByOrderNo(String orderNo) {
        return baseMapper.selectByOrderNo(orderNo);
    }
    
    @Transactional
    public Order confirmReceive(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!Constants.ORDER_SHIPPED.equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(Constants.ORDER_COMPLETED);
        this.updateById(updateOrder);
        return this.getById(orderId);
    }

    @Transactional
    public void deleteOrder(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        // 允许删除待付款、已取消或已完成的订单
        if (!Constants.ORDER_PENDING.equals(order.getStatus()) 
            && !Constants.ORDER_CANCELLED.equals(order.getStatus()) 
            && !Constants.ORDER_COMPLETED.equals(order.getStatus())) {
            throw new RuntimeException("该状态订单不可删除");
        }

        // 删除订单项
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        // 删除订单
        this.removeById(orderId);
    }

    @Transactional
    public void deleteAllOrders(Long userId) {
        // 查询该用户待付款、已取消或已完成的订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .and(w -> w.eq(Order::getStatus, Constants.ORDER_PENDING)
                         .or()
                         .eq(Order::getStatus, Constants.ORDER_CANCELLED)
                         .or()
                         .eq(Order::getStatus, Constants.ORDER_COMPLETED));

        List<Order> orders = this.list(wrapper);
        if (orders.isEmpty()) {
            return;
        }

        // 批量删除订单项
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

        // 批量删除订单
        this.removeByIds(orderIds);
    }

    // 管理员功能
    public Page<Order> getAllOrders(Integer page, Integer pageSize, String status) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> orderPage = this.page(pageParam, wrapper);
        
        for (Order order : orderPage.getRecords()) {
            List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            order.setItems(items);
        }
        
        return orderPage;
    }
    
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "CM" + date + uuid;
    }
}
