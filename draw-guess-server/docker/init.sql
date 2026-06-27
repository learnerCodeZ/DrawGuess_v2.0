-- DrawGuess v2.0 数据库初始化脚本
SET NAMES utf8mb4;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS draw_guess DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE draw_guess;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `phone` VARCHAR(11) NOT NULL COMMENT '手机号（登录标识）',
    `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
    `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER/ADMIN/SUPER_ADMIN',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    `total_score` INT NOT NULL DEFAULT 0 COMMENT '总得分',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS `friend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/ACCEPTED/REJECTED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 房间表
CREATE TABLE IF NOT EXISTS `room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` VARCHAR(6) NOT NULL COMMENT '6位房间号',
    `creator_id` BIGINT NOT NULL COMMENT '房主ID',
    `state` VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '状态: WAITING/PLAYING/PAUSED/ENDED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_id` (`room_id`),
    KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间表';

-- 房间成员表
CREATE TABLE IF NOT EXISTS `room_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `score` INT NOT NULL DEFAULT 0 COMMENT '本局得分',
    `painter_order` INT NOT NULL DEFAULT 0 COMMENT '画家顺序',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间成员表';

-- 游戏记录表
CREATE TABLE IF NOT EXISTS `game_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `played_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '游戏时间',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏记录表';

-- 游戏记录详情表
CREATE TABLE IF NOT EXISTS `game_record_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `record_id` BIGINT NOT NULL COMMENT '游戏记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `score` INT NOT NULL DEFAULT 0 COMMENT '本局得分',
    `word` VARCHAR(50) DEFAULT NULL COMMENT '本局分配词语',
    PRIMARY KEY (`id`),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏记录详情表';

-- 词库表
CREATE TABLE IF NOT EXISTS `word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `word` VARCHAR(50) NOT NULL COMMENT '词语',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认词(不可删)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='词库表';

-- 插入5个默认词语
INSERT IGNORE INTO `word` (`word`, `is_default`) VALUES
('苹果', 1),
('大象', 1),
('太阳', 1),
('飞机', 1),
('钢琴', 1);

-- 插入扩展词库（非默认，可删除）
INSERT IGNORE INTO `word` (`word`, `is_default`) VALUES
('猫咪', 0),
('月亮', 0),
('汽车', 0),
('蛋糕', 0),
('足球', 0),
('书包', 0),
('雨伞', 0),
('手表', 0),
('西瓜', 0),
('眼镜', 0),
('吉他', 0),
('风筝', 0),
('冰箱', 0),
('火箭', 0),
('蝴蝶', 0),
('蜗牛', 0),
('长颈鹿', 0),
('向日葵', 0),
('彩虹', 0),
('雪人', 0);
