-- ============================================================
-- V4: 预置医生和药师种子账号
-- ============================================================

USE smart_health;

-- 1. 插入示例医生信息
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES (1, '王建国', '主任医师', '内科', '心血管内科、高血压、冠心病', '从事心血管内科临床工作20余年，擅长冠心病、高血压等疾病的诊治。');

-- 2. 插入医生登录账号（密码: doctor123，BCrypt 加密）
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`, `doctor_id`)
VALUES ('doctor', '$2a$12$32Tgoyau0w8vFMa25cv6NuLOhJtfkyvwdEIBgd/7KjEUo6WuQ5/X2', '王建国', '13800000001', 'DOCTOR', 1);

-- 3. 插入药师登录账号（密码: pharma123，BCrypt 加密）
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`)
VALUES ('pharmacist', '$2a$12$lLG6YKshTkgyHmF0cwd15uFH7pG3hLWj1hlUHPew6SSaFBAm6t.LC', '刘芳', '13800000002', 'PHARMACIST');
