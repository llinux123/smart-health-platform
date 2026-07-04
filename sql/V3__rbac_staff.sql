-- ============================================================
-- V3: 基于角色的访问控制（RBAC）
-- Issue: #30
-- ============================================================

USE smart_health;

-- 1. 新建 t_staff 员工表（医生/药师/运维共用）
CREATE TABLE IF NOT EXISTS `t_staff` (
  `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`    VARCHAR(100) NOT NULL COMMENT '加密密码',
  `real_name`   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  `phone`       VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
  `role`        VARCHAR(20)  NOT NULL COMMENT '角色: DOCTOR / PHARMACIST / ADMIN',
  `doctor_id`   BIGINT(20)   DEFAULT NULL COMMENT '关联医生ID（仅DOCTOR角色）',
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已软删除',
  `deleted_at`  DATETIME     DEFAULT NULL COMMENT '软删除时间',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_doctor_id` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工用户表（医生/药师/运维）';

-- 2. t_patient 表新增软删除字段
ALTER TABLE `t_patient`
  ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已软删除' AFTER `gender`,
  ADD COLUMN `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间' AFTER `is_deleted`;

-- 3. 插入默认管理员种子账号（密码: admin123，BCrypt 加密）
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', '13800000000', 'ADMIN');
