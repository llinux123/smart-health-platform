-- ============================================================
-- 智慧医疗与大健康管理平台 - 数据库完整初始化脚本
-- ============================================================
-- 由原 init.sql + V2 ~ V11 合并整理而成(2026-07-08)
-- 执行说明:
--   1. 本脚本用于从零重建数据库(空库 → 完整结构 + 种子数据)
--   2. 所有表使用 CREATE TABLE IF NOT EXISTS,可重复执行
--   3. 增量升级请参考 git 历史中的 V*.sql 文件,本脚本不再保留版本号
-- 4. MySQL 要求: 8.0+(JSON 字段、utf8mb4_unicode_ci 排序)
-- ============================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS smart_health DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE smart_health;

-- ============================================================
-- 一、表结构
-- ============================================================

-- 1. 患者用户表
-- 字段来源: init.sql + V3(软删除) + V8(邮箱) + V10(账号信息)
CREATE TABLE IF NOT EXISTS `t_patient` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '患者ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `id_card` varchar(18) NOT NULL COMMENT '身份证号',
  `phone` varchar(11) NOT NULL COMMENT '手机号',
  `gender` tinyint(1) DEFAULT '0' COMMENT '性别 (0:未知 1:男 2:女)',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱(密码找回)',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `id_card_status` tinyint(1) DEFAULT '0' COMMENT '实名认证状态 (0:未认证 1:审核中 2:已认证 3:已拒绝)',
  `id_card_front_url` varchar(255) DEFAULT NULL COMMENT '身份证正面URL',
  `id_card_back_url` varchar(255) DEFAULT NULL COMMENT '身份证反面URL',
  `face_recognition_url` varchar(255) DEFAULT NULL COMMENT '人脸识别结果URL',
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
  `title` varchar(50) NOT NULL COMMENT '职称(主任医师/副主任医师/主治医师)',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `dept_name` varchar(50) NOT NULL COMMENT '所属科室',
  `specialty` varchar(500) DEFAULT NULL COMMENT '擅长领域',
  `intro` text DEFAULT NULL COMMENT '医生简介',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息表';

-- 3. 员工表(医生/药师/运维统一登录账号) - V3
CREATE TABLE IF NOT EXISTS `t_staff` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `role` varchar(20) NOT NULL COMMENT '角色: DOCTOR / PHARMACIST / ADMIN',
  `doctor_id` bigint(20) DEFAULT NULL COMMENT '关联医生ID(仅 DOCTOR 角色)',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已软删除',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_doctor_id` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工用户表(医生/药师/运维)';

-- 4. 科室表 - V5
CREATE TABLE IF NOT EXISTS `t_department` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '科室ID',
  `name` varchar(50) NOT NULL COMMENT '科室名称',
  `description` varchar(500) DEFAULT NULL COMMENT '科室简述',
  `icon` varchar(255) DEFAULT NULL COMMENT '科室图标URL',
  `intro` text DEFAULT NULL COMMENT '科室详细介绍',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序权重(升序)',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室信息表';

-- 5. 医生-科室多对多关联表 - V5
CREATE TABLE IF NOT EXISTS `t_doctor_department` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `doctor_id` bigint(20) NOT NULL COMMENT '医生ID',
  `department_id` bigint(20) NOT NULL COMMENT '科室ID',
  `is_primary` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否主科室(1:主科室)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_dept` (`doctor_id`, `department_id`),
  KEY `idx_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生-科室关联表';

-- 6. 医生排班号源表(新增 department_id + 联合唯一键) - init.sql + V5
CREATE TABLE IF NOT EXISTS `t_doctor_schedule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  `doctor_id` bigint(20) NOT NULL COMMENT '医生ID',
  `department_id` bigint(20) DEFAULT NULL COMMENT '科室ID',
  `dept_name` varchar(50) NOT NULL COMMENT '科室名称',
  `work_date` date NOT NULL COMMENT '出诊日期',
  `shift` tinyint(1) NOT NULL DEFAULT '1' COMMENT '班次 (1:上午 2:下午)',
  `total_count` int(11) NOT NULL DEFAULT '0' COMMENT '总号源量',
  `visible_count` int(11) NOT NULL DEFAULT '0' COMMENT '剩余可抢号源量',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '挂号费',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_dept_date_shift` (`doctor_id`, `department_id`, `work_date`, `shift`),
  KEY `idx_doctor_date` (`doctor_id`, `work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生排班号源表';

-- 7. 挂号订单表
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

-- 8. 电子处方表
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
  KEY `idx_patient_doc` (`patient_id`, `doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子处方表';

-- 9. 处方明细表
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

-- 10. 药房药品库存表
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
  UNIQUE KEY `uk_pharmacy_medicine` (`pharmacy_id`, `medicine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药房药品库存表';

-- 11. 药品字典表 - V5
CREATE TABLE IF NOT EXISTS `t_medicine` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `name` varchar(100) NOT NULL COMMENT '药品通用名',
  `brand_name` varchar(100) DEFAULT NULL COMMENT '商品名',
  `category` varchar(50) DEFAULT NULL COMMENT '药品分类',
  `spec` varchar(100) DEFAULT NULL COMMENT '常用规格',
  `unit` varchar(20) NOT NULL COMMENT '最小单位(盒/支/瓶/片/粒)',
  `manufacturer` varchar(200) DEFAULT NULL COMMENT '生产厂家',
  `approval_number` varchar(100) DEFAULT NULL COMMENT '国药准字',
  `price` decimal(10,2) DEFAULT NULL COMMENT '参考单价',
  `is_otc` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否OTC(0:处方药 1:OTC)',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品字典表';

-- 12. 库存变动日志表 - V5
CREATE TABLE IF NOT EXISTS `t_inventory_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `pharmacy_id` bigint(20) NOT NULL COMMENT '药房ID',
  `medicine_id` bigint(20) NOT NULL COMMENT '药品ID',
  `change_type` varchar(20) NOT NULL COMMENT '变动类型: INBOUND/OUTBOUND/RECONCILE/DEDUCT/RESTORE',
  `quantity_change` int(11) NOT NULL COMMENT '变动数量(正=增,负=减)',
  `stock_before` int(11) NOT NULL COMMENT '变动前库存',
  `stock_after` int(11) NOT NULL COMMENT '变动后库存',
  `reason` varchar(500) DEFAULT NULL COMMENT '变动原因',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_pharmacy_medicine` (`pharmacy_id`, `medicine_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动日志表';

-- 13. AI 问诊会话表(init.sql + V2 字段 + V6 文件上传 + V11 转诊医生)
CREATE TABLE IF NOT EXISTS `t_consultation_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_sn` varchar(64) NOT NULL COMMENT '会话编号',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `draft_id` varchar(64) DEFAULT NULL COMMENT '症状草稿ID',
  `symptom_draft` text DEFAULT NULL COMMENT '症状自查草稿内容',
  `file_urls` varchar(500) DEFAULT NULL COMMENT '上传文件URL列表(逗号分隔)',
  `status` varchar(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '会话状态: IN_PROGRESS/COMPLETED',
  `assigned_doctor_id` bigint(20) DEFAULT NULL COMMENT '指配的医生ID(关联 t_doctor.id)',
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

-- 14. 问诊对话轮次表(init.sql + V11 发送者类型)
CREATE TABLE IF NOT EXISTS `t_consultation_turn` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '轮次ID',
  `session_sn` varchar(64) NOT NULL COMMENT '会话编号',
  `turn_number` int(11) NOT NULL COMMENT '轮次序号(从1开始)',
  `user_message` text NOT NULL COMMENT '用户消息',
  `assistant_message` text NOT NULL COMMENT 'AI回复',
  `citations` json DEFAULT NULL COMMENT '引用来源(JSON数组)',
  `sender_type` varchar(10) NOT NULL DEFAULT 'PATIENT' COMMENT '发送者类型: PATIENT / AI / DOCTOR',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_turn` (`session_sn`, `turn_number`),
  KEY `idx_session_sn` (`session_sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊对话轮次表';

-- 15. 问诊评分表 - V2
CREATE TABLE IF NOT EXISTS `t_consultation_rating` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评分ID',
  `session_sn` varchar(64) NOT NULL COMMENT '会话编号',
  `patient_id` bigint(20) NOT NULL COMMENT '患者ID',
  `rating` tinyint(4) NOT NULL COMMENT '评分(1-5星)',
  `feedback` varchar(500) DEFAULT NULL COMMENT '文字反馈(可选)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_sn` (`session_sn`),
  KEY `idx_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊评分表';

-- ============================================================
-- 二、种子数据
-- ============================================================

-- 1. 默认管理员 - V3(密码 admin123,BCrypt 加密)
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`) VALUES
('admin', '$2b$10$UtwOO7w8w2JQf.K2YDZPQOgQ1ZE/qdStSKxzTMpTY/1UCBoekxb1q', '系统管理员', '13800000000', 'ADMIN');

-- 2. 预置医生 - V4
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`) VALUES
(1, '王建国', '主任医师', '内科', '心血管内科、高血压、冠心病', '从事心血管内科临床工作20余年，擅长冠心病、高血压等疾病的诊治。');

-- 3. 预置医生登录账号 - V4(密码 doctor123,BCrypt 加密)
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`, `doctor_id`) VALUES
('doctor', '$2a$12$32Tgoyau0w8vFMa25cv6NuLOhJtfkyvwdEIBgd/7KjEUo6WuQ5/X2', '王建国', '13800000001', 'DOCTOR', 1);

-- 4. 预置药师登录账号 - V4(密码 pharma123,BCrypt 加密)
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`) VALUES
('pharmacist', '$2a$12$lLG6YKshTkgyHmF0cwd15uFH7pG3hLWj1hlUHPew6SSaFBAm6t.LC', '刘芳', '13800000002', 'PHARMACIST');

-- 5. 10 个科室字典 - V5
INSERT INTO `t_department` (`name`, `description`, `sort_order`) VALUES
('内科', '常见病、多发病的诊治', 1),
('外科', '手术治疗及创伤处理', 2),
('骨科', '骨骼、关节、脊柱疾病', 3),
('皮肤科', '皮肤疾病及美容', 4),
('心内科', '心血管系统疾病', 5),
('儿科', '儿童及青少年疾病', 6),
('妇产科', '女性生殖健康及产科', 7),
('眼科', '眼部疾病诊治', 8),
('口腔科', '口腔及颌面疾病', 9),
('中医科', '中医辨证论治', 10);

-- 6. 15 种常见药品字典 - V5
INSERT INTO `t_medicine` (`name`, `brand_name`, `category`, `spec`, `unit`, `price`) VALUES
('阿莫西林胶囊', '阿莫仙', '抗生素', '0.5g*24粒', '盒', 12.50),
('布洛芬缓释胶囊', '芬必得', '解热镇痛', '0.3g*20粒', '盒', 18.00),
('奥美拉唑肠溶胶囊', '洛赛克', '消化系统', '20mg*14粒', '盒', 35.00),
('头孢克洛胶囊', '希刻劳', '抗生素', '0.25g*12粒', '盒', 28.50),
('复方甘草片', NULL, '镇咳祛痰', '100片', '瓶', 8.00),
('蒙脱石散', '思密达', '消化系统', '3g*10袋', '盒', 22.00),
('氯雷他定片', '开瑞坦', '抗过敏', '10mg*6片', '盒', 25.00),
('硝苯地平控释片', '拜新同', '心血管', '30mg*7片', '盒', 42.00),
('二甲双胍片', '格华止', '降糖药', '0.5g*30片', '盒', 15.00),
('阿托伐他汀钙片', '立普妥', '降脂药', '20mg*7片', '盒', 55.00),
('红霉素软膏', NULL, '皮肤外用', '10g', '支', 5.00),
('莫匹罗星软膏', '百多邦', '皮肤外用', '5g', '支', 18.50),
('复方丹参滴丸', NULL, '心血管', '27mg*180丸', '瓶', 25.00),
('维生素C片', NULL, '维生素', '100mg*100片', '瓶', 6.00),
('氯化钠注射液', NULL, '输液', '250ml', '瓶', 3.50);

-- 7. 现有医生的科室关联 - V5(根据 t_doctor.dept_name 自动关联主科室)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`)
SELECT d.id, dep.id, 1
FROM `t_doctor` d
JOIN `t_department` dep ON d.dept_name = dep.name;

-- 8. 补充预置医生(V7): 内科 +3、外科 +3、骨科 +3、皮肤科 +3、心内科 +3、儿科 +3、妇产科 +3、眼科 +3、口腔科 +3、中医科 +3
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`) VALUES
(5,  '张伟', '副主任医师', '内科', '呼吸系统疾病、慢性阻塞性肺病', '从事呼吸内科临床工作15年，对慢阻肺、哮喘等疾病有丰富经验。'),
(6,  '李明', '主治医师',   '内科', '消化系统疾病、胃肠镜诊治',   '擅长胃肠道疾病的内镜诊断与治疗。'),
(7,  '赵强', '主任医师',   '内科', '肾病综合征、血液净化',       '肾内科专家，对各类肾小球疾病诊治经验丰富。'),
(8,  '刘洋', '主任医师',   '外科', '肝胆外科、微创手术',         '肝胆胰外科专家，擅长腹腔镜微创手术。'),
(9,  '陈刚', '副主任医师', '外科', '胃肠外科、疝与腹壁外科',     '胃肠道肿瘤及疝气手术经验丰富。'),
(10, '王猛', '主治医师',   '外科', '甲状腺乳腺外科',             '甲状腺、乳腺疾病的外科诊疗。'),
(11, '赵磊', '主任医师',   '骨科', '脊柱外科、关节置换',         '脊柱退变性疾病及人工关节置换手术。'),
(12, '孙鹏', '副主任医师', '骨科', '创伤骨科、骨折微创治疗',     '四肢骨折及骨盆创伤的微创内固定手术。'),
(13, '周涛', '主治医师',   '骨科', '运动医学、关节镜',           '膝关节及肩关节运动损伤的关节镜手术。'),
(14, '吴敏', '主任医师',   '皮肤科', '皮肤肿瘤、激光美容',   '皮肤良恶性肿瘤切除及激光美容治疗。'),
(15, '郑秀', '副主任医师', '皮肤科', '湿疹、荨麻疹、银屑病', '过敏性皮肤病及免疫相关性皮肤病的诊治。'),
(16, '冯丽', '主治医师',   '皮肤科', '痤疮、真菌感染',       '面部痤疮综合治疗及皮肤真菌感染诊治。'),
(17, '周平', '主任医师',   '心内科', '冠心病介入治疗、心律失常', '冠脉造影及支架植入术经验丰富。'),
(18, '吴华', '副主任医师', '心内科', '心力衰竭、高血压',         '慢性心衰及难治性高血压的综合管理。'),
(19, '郑杰', '主治医师',   '心内科', '心肌病、心脏超声',         '各类心肌病的诊断及心脏超声评估。'),
(20, '林芳', '主任医师',   '儿科', '新生儿疾病、儿童保健',   '新生儿重症监护及儿童生长发育指导。'),
(21, '黄磊', '副主任医师', '儿科', '小儿呼吸、小儿哮喘',     '儿童哮喘及反复呼吸道感染的规范化治疗。'),
(22, '马丽', '主治医师',   '儿科', '小儿消化、营养性疾病',   '儿童消化不良及营养缺乏性疾病的诊治。'),
(23, '孙丽', '主任医师',   '妇产科', '妇科肿瘤、盆底康复',       '妇科良恶性肿瘤手术及盆底功能障碍康复。'),
(24, '朱红', '副主任医师', '妇产科', '产科高危妊娠管理',         '高危妊娠监护及危重孕产妇救治。'),
(25, '沈洁', '主治医师',   '妇产科', '妇科内分泌、不孕不育',     '月经失调、多囊卵巢综合征及不孕症诊疗。'),
(26, '高峰', '主任医师',   '眼科', '白内障、青光眼手术',     '白内障超声乳化及青光眼手术。'),
(27, '罗明', '副主任医师', '眼科', '眼底病、视网膜脱落',     '糖尿病视网膜病变及视网膜脱落手术。'),
(28, '韩冰', '主治医师',   '眼科', '屈光不正、斜弱视',       '青少年近视防控及斜弱视矫正治疗。'),
(29, '曹阳', '主任医师',   '口腔科', '口腔颌面外科、种植牙',   '颌面部创伤修复及种植牙手术。'),
(30, '许文', '副主任医师', '口腔科', '牙体牙髓、牙周病',       '根管治疗及牙周系统治疗。'),
(31, '邓杰', '主治医师',   '口腔科', '口腔正畸、儿童牙科',     '固定矫治及儿童龋病防治。'),
(32, '何强', '主任医师',   '中医科', '中医内科、针灸',           '中医辨证治疗脾胃病及针灸康复。'),
(33, '吕静', '副主任医师', '中医科', '中医妇科、中医养生',       '月经不调、更年期综合征及体质调理。'),
(34, '潘敏', '主治医师',   '中医科', '中医骨伤、推拿按摩',       '颈肩腰腿痛的中医综合治疗及推拿。');

-- 9. 为上述 30 名新医生建立医生-科室主关联(V7)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(5,  1, 1), (6,  1, 1), (7,  1, 1),
(8,  2, 1), (9,  2, 1), (10, 2, 1),
(11, 3, 1), (12, 3, 1), (13, 3, 1),
(14, 4, 1), (15, 4, 1), (16, 4, 1),
(17, 5, 1), (18, 5, 1), (19, 5, 1),
(20, 6, 1), (21, 6, 1), (22, 6, 1),
(23, 7, 1), (24, 7, 1), (25, 7, 1),
(26, 8, 1), (27, 8, 1), (28, 8, 1),
(29, 9, 1), (30, 9, 1), (31, 9, 1),
(32, 10, 1), (33, 10, 1), (34, 10, 1);

-- 10. 为现有 4 名医生创建登录账号(V7 注释里描述的医生,doctor_id 1~4)
-- 现有医生:王明华(1,皮肤科)、李秀英(2,内科)、张建国(3,骨科)、陈晓燕(4,心内科)
-- 注意: t_doctor 中 id=1 实际为 V4 插入的"王建国",其余 id(2/3/4) 在此脚本中不实际存在;
--      t_staff.doctor_id 仅作业务引用,无外键约束,可保留以兼容现有脚本语义。
-- 统一密码 doctor123
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`, `doctor_id`) VALUES
('doc_wangminghua',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '王明华', '13800004001', 'DOCTOR', 1),
('doc_lixiuying',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '李秀英', '13800001002', 'DOCTOR', 2),
('doc_zhangjianguo',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '张建国', '13800003003', 'DOCTOR', 3),
('doc_chenxiaoyan',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '陈晓燕', '13800005004', 'DOCTOR', 4);

-- 11. 给 30 名新医生创建登录账号(V7,统一密码 doctor123)
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`, `doctor_id`) VALUES
('doc_zhangwei',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '张伟', '13800001005', 'DOCTOR', 5),
('doc_liming',    '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '李明', '13800001006', 'DOCTOR', 6),
('doc_zhaoqiang', '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '赵强', '13800001007', 'DOCTOR', 7),
('doc_liuyang',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '刘洋', '13800002008', 'DOCTOR', 8),
('doc_chengang',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '陈刚', '13800002009', 'DOCTOR', 9),
('doc_wangmeng',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '王猛', '13800002010', 'DOCTOR', 10),
('doc_zhaolei',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '赵磊', '13800003011', 'DOCTOR', 11),
('doc_sunpeng',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '孙鹏', '13800003012', 'DOCTOR', 12),
('doc_zhoutao',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '周涛', '13800003013', 'DOCTOR', 13),
('doc_wumin',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '吴敏', '13800004014', 'DOCTOR', 14),
('doc_zhengxiu',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '郑秀', '13800004015', 'DOCTOR', 15),
('doc_fengli',    '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '冯丽', '13800004016', 'DOCTOR', 16),
('doc_zhouping',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '周平', '13800005017', 'DOCTOR', 17),
('doc_wuhua',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '吴华', '13800005018', 'DOCTOR', 18),
('doc_zhengjie',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '郑杰', '13800005019', 'DOCTOR', 19),
('doc_linfang',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '林芳', '13800006020', 'DOCTOR', 20),
('doc_huanglei',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '黄磊', '13800006021', 'DOCTOR', 21),
('doc_mali',      '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '马丽', '13800006022', 'DOCTOR', 22),
('doc_sunli',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '孙丽', '13800007023', 'DOCTOR', 23),
('doc_zhuhong',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '朱红', '13800007024', 'DOCTOR', 24),
('doc_shenjie',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '沈洁', '13800007025', 'DOCTOR', 25),
('doc_gaofeng',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '高峰', '13800008026', 'DOCTOR', 26),
('doc_luoming',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '罗明', '13800008027', 'DOCTOR', 27),
('doc_hanbing',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '韩冰', '13800008028', 'DOCTOR', 28),
('doc_caoyang',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '曹阳', '13800009029', 'DOCTOR', 29),
('doc_xuwen',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '许文', '13800009030', 'DOCTOR', 30),
('doc_dengjie',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '邓杰', '13800009031', 'DOCTOR', 31),
('doc_heqiang',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '何强', '13800010032', 'DOCTOR', 32),
('doc_lvjing',    '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '吕静', '13800010033', 'DOCTOR', 33),
('doc_panmin',    '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '潘敏', '13800010034', 'DOCTOR', 34);

-- 12. 新增药师账号(V7,统一密码 pharma123)
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`) VALUES
('pharmacist1', '$2b$12$ih.6DiF.7TAgqLDvo0zaK.TEU21qfyVArXTnmhC23lfaUTszAPB0u', '刘芳', '13800000011', 'PHARMACIST'),
('pharmacist2', '$2b$12$ih.6DiF.7TAgqLDvo0zaK.TEU21qfyVArXTnmhC23lfaUTszAPB0u', '陈雪', '13800000012', 'PHARMACIST'),
('pharmacist3', '$2b$12$ih.6DiF.7TAgqLDvo0zaK.TEU21qfyVArXTnmhC23lfaUTszAPB0u', '王丽', '13800000013', 'PHARMACIST');

-- 13. 新增运维账号(V7)
-- superadmin / operator / monitor,密码分别为 superadmin123 / operator123 / monitor123(BCrypt 加密)
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`) VALUES
('superadmin', '$2b$10$LhBMBZlFPfC3mOV1l43JS.VjmvsJhqxQBdSGi9cIEqRJpYkabRCFW', '超级管理员', '13800000020', 'ADMIN'),
('operator',   '$2b$10$.nyNPyumGPTmwBzmgBsO2./XiNjQSkXvX6JqIBdT/d.QvmXkRxe5S', '运维操作员', '13800000021', 'ADMIN'),
('monitor',    '$2b$10$QF/bcBBlWILpUiSTSp1zluNQErYKU7ih6CUxUehkKPELZqzsekPam', '系统监控员', '13800000022', 'ADMIN');

-- 14. 初始化药房库存数据(pharmacy_id=1, 为全部 15 种药品生成初始库存)
-- 注意: medicine_id 与第 6 节 t_medicine 插入顺序一致(1~15)
INSERT INTO `t_pharmacy_inventory` (`pharmacy_id`, `medicine_id`, `medicine_name`, `stock`, `lock_stock`, `unit`) VALUES
(1, 1,  '阿莫西林胶囊',       200, 0, '盒'),
(1, 2,  '布洛芬缓释胶囊',     150, 0, '盒'),
(1, 3,  '奥美拉唑肠溶胶囊',   120, 0, '盒'),
(1, 4,  '头孢克洛胶囊',       100, 0, '盒'),
(1, 5,  '复方甘草片',         80,  0, '瓶'),
(1, 6,  '蒙脱石散',           100, 0, '盒'),
(1, 7,  '氯雷他定片',         90,  0, '盒'),
(1, 8,  '硝苯地平控释片',     60,  0, '盒'),
(1, 9,  '二甲双胍片',         150, 0, '盒'),
(1, 10, '阿托伐他汀钙片',     50,  0, '盒'),
(1, 11, '红霉素软膏',         100, 0, '支'),
(1, 12, '莫匹罗星软膏',       80,  0, '支'),
(1, 13, '复方丹参滴丸',       120, 0, '瓶'),
(1, 14, '维生素C片',          200, 0, '瓶'),
(1, 15, '氯化钠注射液',       300, 0, '瓶');

-- ============================================================
-- 完成
-- ============================================================