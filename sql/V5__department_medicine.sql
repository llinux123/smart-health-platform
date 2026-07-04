-- ============================================================
-- V5: 科室表 + 医生科室多对多 + 药品字典 + 库存日志
-- ============================================================

USE smart_health;

-- ========== 1. 科室信息表 ==========
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

-- ========== 2. 医生-科室多对多关联表 ==========
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

-- ========== 3. 初始科室数据 ==========
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

-- ========== 4. 排班表新增 department_id 列 ==========
ALTER TABLE `t_doctor_schedule`
  ADD COLUMN `department_id` bigint(20) DEFAULT NULL COMMENT '科室ID' AFTER `doctor_id`;

-- 数据回填：根据 dept_name 匹配 department_id
UPDATE `t_doctor_schedule` s
  JOIN `t_department` dep ON s.dept_name = dep.name
  SET s.department_id = dep.id;

-- 排班唯一约束（同医生+同科室+同日期+同班次不可重复）
ALTER TABLE `t_doctor_schedule`
  ADD UNIQUE KEY `uk_doctor_dept_date_shift` (`doctor_id`, `department_id`, `work_date`, `shift`);

-- ========== 5. 迁移现有医生的科室关联 ==========
-- 将 t_doctor 中已有的 dept_name 创建主科室关联记录
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`)
SELECT d.id, dep.id, 1
FROM `t_doctor` d
JOIN `t_department` dep ON d.dept_name = dep.name;

-- ========== 6. 药品字典表 ==========
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

-- ========== 7. 库存变动日志表 ==========
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

-- ========== 8. 初始药品数据（15种常见药品） ==========
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
