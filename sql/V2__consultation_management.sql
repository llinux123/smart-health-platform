-- ============================================================
-- V2: 问诊记录管理功能 - 数据层重构
-- Issue: #28
-- ============================================================

USE smart_health;

-- 1. 改造 t_consultation_session 表
-- 新增: status, is_deleted, deleted_at, is_pinned, ai_summary, last_chat_time
-- 移除: chat_log (迁移到 t_consultation_turn)

ALTER TABLE `t_consultation_session`
  ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '会话状态: IN_PROGRESS/COMPLETED' AFTER `symptom_draft`,
  ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除(回收站)' AFTER `status`,
  ADD COLUMN `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间(回收站)' AFTER `is_deleted`,
  ADD COLUMN `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶' AFTER `deleted_at`,
  ADD COLUMN `ai_summary` TEXT DEFAULT NULL COMMENT 'AI总结' AFTER `is_pinned`,
  ADD COLUMN `last_chat_time` DATETIME DEFAULT NULL COMMENT '最后对话时间' AFTER `ai_summary`;

-- 为 session 表添加索引
ALTER TABLE `t_consultation_session`
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_is_deleted` (`is_deleted`),
  ADD KEY `idx_last_chat_time` (`patient_id`, `last_chat_time` DESC);

-- 2. 新建 t_consultation_turn 表 (一轮对话一行)
CREATE TABLE IF NOT EXISTS `t_consultation_turn` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '轮次ID',
  `session_sn` VARCHAR(64) NOT NULL COMMENT '会话编号',
  `turn_number` INT NOT NULL COMMENT '轮次序号(从1开始)',
  `user_message` TEXT NOT NULL COMMENT '用户消息',
  `assistant_message` TEXT NOT NULL COMMENT 'AI回复',
  `citations` JSON DEFAULT NULL COMMENT '引用来源(JSON数组)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_turn` (`session_sn`, `turn_number`),
  KEY `idx_session_sn` (`session_sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊对话轮次表';

-- 3. 新建 t_consultation_rating 表 (会话评分)
CREATE TABLE IF NOT EXISTS `t_consultation_rating` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '评分ID',
  `session_sn` VARCHAR(64) NOT NULL COMMENT '会话编号',
  `patient_id` BIGINT(20) NOT NULL COMMENT '患者ID',
  `rating` TINYINT NOT NULL COMMENT '评分(1-5星)',
  `feedback` VARCHAR(500) DEFAULT NULL COMMENT '文字反馈(可选)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_sn` (`session_sn`),
  KEY `idx_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊评分表';

-- 4. 开发阶段: 清空旧 chat_log 数据并移除字段
-- 注意: 生产环境需先执行数据迁移脚本将 chat_log 解析到 t_consultation_turn
ALTER TABLE `t_consultation_session` DROP COLUMN `chat_log`;
