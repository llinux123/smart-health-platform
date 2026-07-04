-- ============================================================
-- V6: 问诊会话表新增 file_urls 字段
-- ============================================================

USE smart_health;

ALTER TABLE `t_consultation_session`
  ADD COLUMN `file_urls` VARCHAR(500) DEFAULT NULL COMMENT '上传文件URL列表(逗号分隔)' AFTER `symptom_draft`;
