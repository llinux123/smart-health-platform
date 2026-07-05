-- ============================================================
-- 智慧医疗与大健康管理平台 - 数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_health DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE smart_health;

-- 1. 患者用户表
CREATE TABLE IF NOT EXISTS `t_patient` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '患者ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `id_card` varchar(18) NOT NULL COMMENT '身份证号',
  `phone` varchar(11) NOT NULL COMMENT '手机号',
  `gender` tinyint(1) DEFAULT '0' COMMENT '性别 (0:未知 1:男 2:女)',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱（密码找回）',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已软删除',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_id_card` (`id_card`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者用户表';

-- 2. 医生信息表
CREATE TABLE IF NOT EXISTS `t_doctor` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '医生ID',
  `name` varchar(50) NOT NULL COMMENT '医生姓名',
  `title` varchar(50) NOT NULL COMMENT '职称（如：主任医师、副主任医师、主治医师）',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `dept_name` varchar(50) NOT NULL COMMENT '所属科室',
  `specialty` varchar(500) DEFAULT NULL COMMENT '擅长领域',
  `intro` text DEFAULT NULL COMMENT '医生简介',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息表';

-- 3. 医生排班号源表
CREATE TABLE IF NOT EXISTS `t_doctor_schedule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  `doctor_id` bigint(20) NOT NULL COMMENT '医生ID',
  `dept_name` varchar(50) NOT NULL COMMENT '科室名称',
  `work_date` date NOT NULL COMMENT '出诊日期',
  `shift` tinyint(1) NOT NULL DEFAULT '1' COMMENT '班次 (1:上午 2:下午)',
  `total_count` int(11) NOT NULL DEFAULT '0' COMMENT '总号源量',
  `visible_count` int(11) NOT NULL DEFAULT '0' COMMENT '剩余可抢号源量',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '挂号费',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_doctor_date` (`doctor_id`,`work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生排班号源表';

-- 3. 挂号订单表
CREATE TABLE IF NOT EXISTS `t_registration_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '挂号单ID',
  `order_sn` varchar(64) NOT NULL COMMENT '订单流水号(全局唯一)',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `schedule_id` bigint(20) NOT NULL COMMENT '排班ID',
  `sequence_number` int(11) DEFAULT NULL COMMENT '就诊呼叫序号',
  `amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '支付金额',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态 (0:排队中 1:待支付 2:已支付 3:已就诊 4:已退号)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_sn` (`order_sn`),
  KEY `idx_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号订单表';

-- 4. 电子处方表
CREATE TABLE IF NOT EXISTS `t_prescription` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '处方ID',
  `prescription_sn` varchar(64) NOT NULL COMMENT '处方全国唯一编码',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `doctor_id` bigint(20) NOT NULL COMMENT '开具医生ID',
  `diagnosis` varchar(500) NOT NULL COMMENT '临床诊断结论',
  `pdf_url` varchar(255) DEFAULT NULL COMMENT '电子处方PDF存根路径',
  `audit_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '药师审核状态 (0:待审核 1:审核通过 2:驳回)',
  `pharmacist_id` bigint(20) DEFAULT NULL COMMENT '审核药师ID',
  `audit_comments` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '流转状态 (0:未配药 1:配药中 2:已发药)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开具时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pres_sn` (`prescription_sn`),
  KEY `idx_patient_doc` (`patient_id`,`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子处方表';

-- 5. 药房药品库存表
CREATE TABLE IF NOT EXISTS `t_pharmacy_inventory` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '库存记录ID',
  `pharmacy_id` bigint(20) NOT NULL COMMENT '院区药房ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `medicine_name` varchar(100) NOT NULL COMMENT '药品通用名',
  `stock` int(11) NOT NULL DEFAULT '0' COMMENT '实际库存量',
  `lock_stock` int(11) NOT NULL DEFAULT '0' COMMENT '冻结库存量',
  `unit` varchar(20) NOT NULL COMMENT '单位',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pharmacy_medicine` (`pharmacy_id`,`medicine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药房药品库存表';

-- 6. AI问诊会话表
CREATE TABLE IF NOT EXISTS `t_consultation_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_sn` varchar(64) NOT NULL COMMENT '会话编号',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `draft_id` varchar(64) DEFAULT NULL COMMENT '症状草稿ID',
  `symptom_draft` text DEFAULT NULL COMMENT '症状自查草稿内容',
  `status` varchar(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '会话状态: IN_PROGRESS/COMPLETED',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已删除(回收站)',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间(回收站)',
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `ai_summary` text DEFAULT NULL COMMENT 'AI总结',
  `last_chat_time` datetime DEFAULT NULL COMMENT '最后对话时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_sn` (`session_sn`),
  KEY `idx_patient_id` (`patient_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_last_chat_time` (`patient_id`, `last_chat_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问诊会话表';

-- 6b. 问诊对话轮次表
CREATE TABLE IF NOT EXISTS `t_consultation_turn` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '轮次ID',
  `session_sn` varchar(64) NOT NULL COMMENT '会话编号',
  `turn_number` int NOT NULL COMMENT '轮次序号(从1开始)',
  `user_message` text NOT NULL COMMENT '用户消息',
  `assistant_message` text NOT NULL COMMENT 'AI回复',
  `citations` json DEFAULT NULL COMMENT '引用来源(JSON数组)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_turn` (`session_sn`, `turn_number`),
  KEY `idx_session_sn` (`session_sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊对话轮次表';

-- 6c. 问诊评分表
CREATE TABLE IF NOT EXISTS `t_consultation_rating` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评分ID',
  `session_sn` varchar(64) NOT NULL COMMENT '会话编号',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `rating` tinyint NOT NULL COMMENT '评分(1-5星)',
  `feedback` varchar(500) DEFAULT NULL COMMENT '文字反馈(可选)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_sn` (`session_sn`),
  KEY `idx_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊评分表';

-- 8. 处方审核字段迁移（已有数据库升级用）
-- ALTER TABLE `t_prescription`
--   ADD COLUMN `pharmacist_id` bigint(20) DEFAULT NULL COMMENT '审核药师ID' AFTER `audit_status`,
--   ADD COLUMN `audit_comments` varchar(500) DEFAULT NULL COMMENT '审核意见' AFTER `pharmacist_id`,
--   ADD COLUMN `audit_time` datetime DEFAULT NULL COMMENT '审核时间' AFTER `audit_comments`;

-- 7. 处方明细表
CREATE TABLE IF NOT EXISTS `t_prescription_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `prescription_id` bigint(20) NOT NULL COMMENT '处方ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `medicine_name` varchar(100) NOT NULL COMMENT '药品名称',
  `pharmacy_id` bigint(20) NOT NULL COMMENT '院区药房ID',
  `quantity` int(11) NOT NULL DEFAULT '1' COMMENT '数量',
  `unit` varchar(20) NOT NULL COMMENT '单位',
  `spec` varchar(100) DEFAULT NULL COMMENT '规格',
  `usage` varchar(200) DEFAULT NULL COMMENT '用法用量',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  PRIMARY KEY (`id`),
  KEY `idx_prescription_id` (`prescription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方明细表';
