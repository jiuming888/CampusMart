-- 添加评论回复支持
ALTER TABLE tb_comment 
ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT '回复的评论ID，NULL表示顶级评论';

-- 添加索引
ALTER TABLE tb_comment 
ADD INDEX `idx_parent_id` (`parent_id`);
