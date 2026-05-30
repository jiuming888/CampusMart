-- ============================================
-- 修复字段大小问题
-- ============================================

USE `campus_mart`;

-- 将 images 字段改为 MEDIUMTEXT（最大 16MB）
ALTER TABLE `tb_product` MODIFY COLUMN `images` MEDIUMTEXT COMMENT '图片JSON数组';

-- 将 description 字段也改为 MEDIUMTEXT 以确保安全
ALTER TABLE `tb_product` MODIFY COLUMN `description` MEDIUMTEXT COMMENT '商品描述';

-- 同样修改订单项表中的 product_image 字段
ALTER TABLE `tb_order_item` MODIFY COLUMN `product_image` MEDIUMTEXT COMMENT '商品图片快照';

SELECT '字段修改完成！' AS '消息';
