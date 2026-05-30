package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.common.Constants;
import com.campusmart.dto.LoginDTO;
import com.campusmart.dto.RegisterDTO;
import com.campusmart.entity.Order;
import com.campusmart.entity.OrderItem;
import com.campusmart.entity.Product;
import com.campusmart.entity.User;
import com.campusmart.mapper.AddressMapper;
import com.campusmart.mapper.CartMapper;
import com.campusmart.mapper.CommentMapper;
import com.campusmart.mapper.MessageMapper;
import com.campusmart.mapper.OrderItemMapper;
import com.campusmart.mapper.OrderMapper;
import com.campusmart.mapper.ProductMapper;
import com.campusmart.mapper.UserMapper;
import com.campusmart.mapper.WarningMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campusmart.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private WarningMapper warningMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Value("${app.upload.path:../public/images/}")
    private String uploadPath;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public Map<String, Object> login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(wrapper);
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // 记录最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        result.put("isAdmin", Constants.ROLE_ADMIN.equals(user.getRole()));

        return result;
    }
    
    public User register(RegisterDTO registerDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setRole(Constants.ROLE_USER);
        user.setStatus(1);
        
        this.save(user);
        return user;
    }
    
    public User getUserInfo(Long userId) {
        return this.getById(userId);
    }
    
    public User updateUser(Long userId, User user) {
        User existingUser = this.getById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (user.getNickname() != null) {
            existingUser.setNickname(user.getNickname());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getAvatar() != null) {
            existingUser.setAvatar(user.getAvatar());
        }
        
        this.updateById(existingUser);
        return existingUser;
    }
    
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }

    public void disableUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (Constants.ROLE_ADMIN.equals(user.getRole())) {
            throw new RuntimeException("不能禁用管理员账号");
        }
        user.setStatus(0);
        this.updateById(user);
    }

    public void enableUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(1);
        this.updateById(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (Constants.ROLE_ADMIN.equals(user.getRole())) {
            throw new RuntimeException("不能删除管理员账号");
        }
        // 1. 查询该用户的所有商品
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getUserId, userId);
        List<Product> userProducts = productMapper.selectList(productWrapper);
        List<Long> userProductIds = userProducts.stream()
            .map(Product::getId).collect(Collectors.toList());

        // 2. 查询该用户的订单
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getUserId, userId);
        List<Order> orders = orderMapper.selectList(orderWrapper);
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        // 3. 删除所有引用了该用户商品的订单项（包括其他用户下的订单！）
        if (!userProductIds.isEmpty()) {
            orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getProductId, userProductIds));
        }
        // 4. 删除该用户的订单项（该用户作为买家）
        if (!orderIds.isEmpty()) {
            orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds));
        }
        // 5. 删除该用户的订单
        orderMapper.delete(orderWrapper);
        // 6. 删除私信
        messageMapper.delete(new LambdaQueryWrapper<com.campusmart.entity.Message>()
            .eq(com.campusmart.entity.Message::getFromUserId, userId));
        messageMapper.delete(new LambdaQueryWrapper<com.campusmart.entity.Message>()
            .eq(com.campusmart.entity.Message::getToUserId, userId));
        // 7. 删除收货地址
        addressMapper.delete(new LambdaQueryWrapper<com.campusmart.entity.Address>()
            .eq(com.campusmart.entity.Address::getUserId, userId));
        // 8. 删除评论
        commentMapper.delete(new LambdaQueryWrapper<com.campusmart.entity.Comment>()
            .eq(com.campusmart.entity.Comment::getUserId, userId));
        // 9. 删除警告记录
        warningMapper.delete(new LambdaQueryWrapper<com.campusmart.entity.Warning>()
            .eq(com.campusmart.entity.Warning::getUserId, userId));
        // 10. 删除商品图片文件
        for (Product product : userProducts) {
            deleteImageFiles(product.getImages());
        }
        // 11. 删除商品
        productMapper.delete(productWrapper);
        // 12. 删除购物车
        cartMapper.delete(new LambdaQueryWrapper<com.campusmart.entity.Cart>()
            .eq(com.campusmart.entity.Cart::getUserId, userId));
        // 13. 最后删除用户
        this.removeById(userId);
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
                if (url != null && url.startsWith("/images/")) {
                    String relativePath = url.substring("/images/".length());
                    File file = new File(uploadPath, relativePath);
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            // 图片删除失败不影响用户删除
        }
    }

    @Transactional
    public void deleteUserOrders(Long userId) {
        // 查询该用户的所有订单
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getUserId, userId);
        List<Order> orders = orderMapper.selectList(orderWrapper);

        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
            // 删除订单项
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(OrderItem::getOrderId, orderIds);
            orderItemMapper.delete(itemWrapper);
            // 删除订单
            orderMapper.delete(orderWrapper);
        }
    }
}
