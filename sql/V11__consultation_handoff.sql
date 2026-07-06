-- ============================================================
-- V11: 问诊转诊支持——会话状态扩展与发送者类型
-- ============================================================

USE smart_health;

-- 1. 问诊会话表: 新增指配医生字段
ALTER TABLE `t_consultation_session`
    ADD COLUMN `assigned_doctor_id` bigint(20) DEFAULT NULL COMMENT '指配的医生ID（关联t_doctor.id）' AFTER `status`;

-- 2. 问诊轮次表: 新增发送者类型
ALTER TABLE `t_consultation_turn`
    ADD COLUMN `sender_type` varchar(10) NOT NULL DEFAULT 'PATIENT' COMMENT '发送者类型: PATIENT / AI / DOCTOR' AFTER `citations`;
