-- ============================================================
-- V10: 患者表新增账号信息相关字段
-- ============================================================

USE smart_health;

ALTER TABLE `t_patient`
    ADD COLUMN `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL' AFTER `email`,
    ADD COLUMN `birthday` date DEFAULT NULL COMMENT '生日' AFTER `avatar`,
    ADD COLUMN `id_card_status` tinyint(1) DEFAULT '0' COMMENT '实名认证状态 (0:未认证 1:审核中 2:已认证 3:已拒绝)' AFTER `birthday`,
    ADD COLUMN `id_card_front_url` varchar(255) DEFAULT NULL COMMENT '身份证正面URL' AFTER `id_card_status`,
    ADD COLUMN `id_card_back_url` varchar(255) DEFAULT NULL COMMENT '身份证反面URL' AFTER `id_card_front_url`,
    ADD COLUMN `face_recognition_url` varchar(255) DEFAULT NULL COMMENT '人脸识别结果URL' AFTER `id_card_back_url`;
