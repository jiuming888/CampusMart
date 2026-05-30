-- ============================================
-- CampusMart 校园闲置交易平台 - 数据库初始化脚本
-- 数据库名：campus_mart
-- 版本：v1.0
-- 创建日期：2026-05-14
-- ============================================

-- 创建数据库
DROP DATABASE IF EXISTS `campus_mart`;
CREATE DATABASE `campus_mart` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `campus_mart`;

-- ============================================
-- 1. 用户表 (tb_user)
-- ============================================
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户，ADMIN-管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 商品分类表 (tb_category)
-- ============================================
DROP TABLE IF EXISTS `tb_category`;
CREATE TABLE `tb_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '图标URL',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序，数字越小越靠前',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- ============================================
-- 3. 商品表 (tb_product)
-- ============================================
DROP TABLE IF EXISTS `tb_product`;
CREATE TABLE `tb_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `description` MEDIUMTEXT COMMENT '商品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `images` MEDIUMTEXT COMMENT '图片JSON数组，格式：[{"url":"...","isCover":true},...]',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-已上架，2-已下架，3-审核拒绝',
    `reason` VARCHAR(255) DEFAULT NULL COMMENT '审核拒绝原因',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `stock` INT NOT NULL DEFAULT 1 COMMENT '库存数量',
    `condition_level` VARCHAR(20) DEFAULT NULL COMMENT '新旧程度：全新/几乎全新/轻微使用/明显使用',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '交易地点',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `tb_category` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_product_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ============================================
-- 4. 购物车表 (tb_cart)
-- ============================================
DROP TABLE IF EXISTS `tb_cart`;
CREATE TABLE `tb_cart` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `tb_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- ============================================
-- 5. 收货地址表 (tb_address)
-- ============================================
DROP TABLE IF EXISTS `tb_address`;
CREATE TABLE `tb_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
    `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
    `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
    `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- ============================================
-- 6. 订单表 (tb_order)
-- ============================================
DROP TABLE IF EXISTS `tb_order`;
CREATE TABLE `tb_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单号（唯一）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（买家）',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING-待付款，PAID-已付款，SHIPPED-已发货，COMPLETED-已完成，CANCELLED-已取消',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `receiver_address` VARCHAR(255) NOT NULL COMMENT '收货地址',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================
-- 7. 订单商品表 (tb_order_item)
-- ============================================
DROP TABLE IF EXISTS `tb_order_item`;
CREATE TABLE `tb_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称（快照）',
    `product_image` MEDIUMTEXT DEFAULT NULL COMMENT '商品图片（快照）',
    `price` DECIMAL(10,2) NOT NULL COMMENT '单价（快照）',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`),
    CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `tb_order` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_item_product` FOREIGN KEY (`product_id`) REFERENCES `tb_product` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单商品表';

-- ============================================
-- 8. 私信表 (tb_message)
-- ============================================
DROP TABLE IF EXISTS `tb_message`;
CREATE TABLE `tb_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `from_user_id` BIGINT NOT NULL COMMENT '发送者ID',
    `to_user_id` BIGINT NOT NULL COMMENT '接收者ID',
    `product_id` BIGINT DEFAULT NULL COMMENT '关联商品ID（可选）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_from_user` (`from_user_id`),
    KEY `idx_to_user` (`to_user_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_message_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_product` FOREIGN KEY (`product_id`) REFERENCES `tb_product` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信表';

-- ============================================
-- 9. 商品评论表 (tb_comment)
-- ============================================
DROP TABLE IF EXISTS `tb_comment`;
CREATE TABLE `tb_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `user_id` BIGINT NOT NULL COMMENT '评论者ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `rating` TINYINT DEFAULT NULL COMMENT '评分：1-5星',
    `parent_id` BIGINT DEFAULT NULL COMMENT '回复的评论ID，NULL表示顶级评论',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    CONSTRAINT `fk_comment_product` FOREIGN KEY (`product_id`) REFERENCES `tb_product` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评论表';

-- ============================================
-- 10. 管理员警告表 (tb_warning)
-- ============================================
DROP TABLE IF EXISTS `tb_warning`;
CREATE TABLE `tb_warning` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '警告ID',
    `admin_id` BIGINT NOT NULL COMMENT '管理员ID',
    `user_id` BIGINT NOT NULL COMMENT '被警告用户ID',
    `reason` VARCHAR(255) NOT NULL COMMENT '警告原因',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '警告时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_warning_admin` FOREIGN KEY (`admin_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_warning_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员警告表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入管理员账号（密码：admin123，加密后的BCrypt）
INSERT INTO `tb_user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'ADMIN', 1),
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户', 'USER', 1);

-- 插入商品分类
INSERT INTO `tb_category` (`name`, `icon`, `sort`) VALUES
('书籍教材', 'book', 1),
('生活用品', 'household', 2),
('电子产品', 'electronics', 3),
('服装鞋包', 'fashion', 4),
('美妆护肤', 'beauty', 5),
('运动户外', 'sports', 6),
('文具办公', 'stationery', 7),
('其他', 'other', 8);

-- 插入测试商品（关联测试用户ID=2）
INSERT INTO `tb_product` (`name`, `description`, `price`, `original_price`, `images`, `category_id`, `user_id`, `status`, `view_count`, `stock`, `condition_level`, `location`) VALUES
('高等数学 第七版', '同济大学第七版高等数学，书籍保存完好，有少量笔记，不影响阅读。适合大一新生。', 25.00, 59.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 1, 2, 1, 156, 1, '轻微使用', '图书馆前广场'),
('考研全套资料', '包含数学、英语、政治全套考研资料，附赠真题和笔记。资料齐全，备考必备。', 180.00, 450.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 1, 2, 1, 89, 1, '明显使用', '学生宿舍5号楼'),
('小米台灯', '小米米家LED台灯，99新，只用过几次。色温可调，保护眼睛。', 89.00, 169.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 2, 2, 1, 234, 1, '几乎全新', '学生公寓'),
('便携式电风扇', 'USB充电便携风扇，三档调速，静音设计。夏天必备神器！', 35.00, 69.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 2, 2, 1, 167, 2, '全新', '校内菜鸟驿站'),
('AirPods Pro 2', '苹果AirPods Pro第二代，主动降噪耳机。用了半年，功能正常。', 899.00, 1899.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 3, 2, 1, 456, 1, '轻微使用', '图书馆'),
('机械键盘', '樱桃MX-Board 3.0机械键盘，青轴。打字手感超级好，宿舍使用略吵。', 299.00, 499.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 3, 2, 1, 189, 1, '轻微使用', '实验室'),
('显示器27寸', 'AOC 27寸1080P显示器，ips屏幕。毕业离校出，支持自提。', 650.00, 1299.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 3, 2, 1, 312, 1, '几乎全新', '研究生宿舍'),
('NIKE运动鞋', 'NIKE Air Max 270，尺码42。穿过几次，太小了不合适。', 299.00, 799.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 4, 2, 1, 145, 1, '轻微使用', '操场旁'),
('双肩包', 'jansport经典款双肩包，黑色大容量。耐磨耐用，适合学生党。', 89.00, 299.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 4, 2, 1, 98, 1, '轻微使用', '教学楼'),
('护肤品套装', '科颜氏高保湿套装，水+乳液+面霜。买多了用不完，保质期内。', 320.00, 680.00, '[{\"url\":\"/images/products/default-product.svg\",\"isCover\":true}]', 5, 2, 1, 203, 1, '全新', '女生宿舍');

-- 插入测试收货地址
INSERT INTO `tb_address` (`user_id`, `receiver_name`, `phone`, `province`, `city`, `district`, `detail_address`, `is_default`) VALUES
(2, '张三', '13800138000', '北京市', '北京市', '海淀区', '中关村大街1号学生公寓5号楼301', 1),
(2, '李四', '13900139000', '北京市', '北京市', '朝阳区', '望京SOHO T3 1801', 0);

-- 插入测试评论
INSERT INTO `tb_comment` (`product_id`, `user_id`, `content`, `rating`) VALUES
(1, 2, '书籍很新，价格实惠，推荐购买！', 5),
(5, 2, '耳机音质很好，降噪效果明显，好评！', 5),
(6, 2, '键盘手感很棒，打字很舒服，就是有点吵', 4);

-- ============================================
-- 创建索引优化查询性能
-- ============================================
CREATE INDEX idx_product_name ON tb_product(name(50));
CREATE INDEX idx_product_price ON tb_product(price);

-- ============================================
-- 创建视图：商品详情视图（包含分类名和发布者）
-- ============================================
CREATE OR REPLACE VIEW v_product_detail AS
SELECT 
    p.*,
    c.name AS category_name,
    u.nickname AS seller_nickname,
    u.avatar AS seller_avatar
FROM tb_product p
LEFT JOIN tb_category c ON p.category_id = c.id
LEFT JOIN tb_user u ON p.user_id = u.id
WHERE p.status = 1;

-- ============================================
-- 完成
-- ============================================
SELECT 'CampusMart 数据库初始化完成！' AS '消息';
