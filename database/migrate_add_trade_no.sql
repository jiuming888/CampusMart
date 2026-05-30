-- 添加 trade_no 字段到 tb_order 表
-- 用于存储支付宝交易号

ALTER TABLE tb_order
ADD COLUMN IF NOT EXISTS `trade_no` VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号' AFTER `status`;

-- 如果上面的语法不支持，使用下面的语句（MySQL 8.0.29+ 支持 IF NOT EXISTS）
-- ALTER TABLE tb_order ADD COLUMN `trade_no` VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号' AFTER `status`;
