-- ============================================
-- CampusMart - 添加用户最后登录时间字段
-- 执行日期：2026-05-28
-- ============================================

ALTER TABLE `tb_user` ADD COLUMN `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER `email`;
