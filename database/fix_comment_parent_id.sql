-- ============================================
-- 修复评论回复功能：添加 parent_id 字段
-- ============================================

USE campus_mart;

-- 添加评论回复字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'campus_mart' 
    AND TABLE_NAME = 'tb_comment' 
    AND COLUMN_NAME = 'parent_id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE tb_comment ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT \'回复的评论ID，NULL表示顶级评论\'',
    'SELECT \'Column parent_id already exists\' AS result'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'campus_mart' 
    AND TABLE_NAME = 'tb_comment' 
    AND INDEX_NAME = 'idx_parent_id'
);

SET @sql2 = IF(@index_exists = 0,
    'ALTER TABLE tb_comment ADD INDEX `idx_parent_id` (`parent_id`)',
    'SELECT \'Index idx_parent_id already exists\' AS result'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SELECT '数据库修复完成！' AS result;
