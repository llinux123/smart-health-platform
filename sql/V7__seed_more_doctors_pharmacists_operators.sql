-- ============================================================
-- V7: 补充预置医生（每科至少3名）、新增药师和运维账号
-- ============================================================

USE smart_health;

-- ============================================================
-- 1. 为现有 4 名医生创建登录账号（t_staff）
--    现有医生：王明华(1,皮肤科)、李秀英(2,内科)、
--             张建国(3,骨科)、陈晓燕(4,心内科)
--    统一密码：doctor123
-- ============================================================
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`, `doctor_id`)
VALUES
('doc_wangminghua', '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '王明华', '13800004001', 'DOCTOR', 1),
('doc_lixiuying',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '李秀英', '13800001002', 'DOCTOR', 2),
('doc_zhangjianguo','$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '张建国', '13800003003', 'DOCTOR', 3),
('doc_chenxiaoyan', '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '陈晓燕', '13800005004', 'DOCTOR', 4);

-- ============================================================
-- 2. 新增医生信息（t_doctor），从 id=5 开始
-- ============================================================

-- 内科（现有李秀英，新增3名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(5, '张伟',   '副主任医师', '内科', '呼吸系统疾病、慢性阻塞性肺病', '从事呼吸内科临床工作15年，对慢阻肺、哮喘等疾病有丰富经验。'),
(6, '李明',   '主治医师',   '内科', '消化系统疾病、胃肠镜诊治',   '擅长胃肠道疾病的内镜诊断与治疗。'),
(7, '赵强',   '主任医师',   '内科', '肾病综合征、血液净化',       '肾内科专家，对各类肾小球疾病诊治经验丰富。');

-- 外科
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(8,  '刘洋', '主任医师',   '外科', '肝胆外科、微创手术',         '肝胆胰外科专家，擅长腹腔镜微创手术。'),
(9,  '陈刚', '副主任医师', '外科', '胃肠外科、疝与腹壁外科',     '胃肠道肿瘤及疝气手术经验丰富。'),
(10, '王猛', '主治医师',   '外科', '甲状腺乳腺外科',             '甲状腺、乳腺疾病的外科诊疗。');

-- 骨科（现有张建国，新增2名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(11, '赵磊', '主任医师',   '骨科', '脊柱外科、关节置换',         '脊柱退变性疾病及人工关节置换手术。'),
(12, '孙鹏', '副主任医师', '骨科', '创伤骨科、骨折微创治疗',     '四肢骨折及骨盆创伤的微创内固定手术。'),
(13, '周涛', '主治医师',   '骨科', '运动医学、关节镜',           '膝关节及肩关节运动损伤的关节镜手术。');

-- 皮肤科（现有王明华，新增2名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(14, '吴敏', '主任医师',   '皮肤科', '皮肤肿瘤、激光美容',   '皮肤良恶性肿瘤切除及激光美容治疗。'),
(15, '郑秀', '副主任医师', '皮肤科', '湿疹、荨麻疹、银屑病', '过敏性皮肤病及免疫相关性皮肤病的诊治。'),
(16, '冯丽', '主治医师',   '皮肤科', '痤疮、真菌感染',       '面部痤疮综合治疗及皮肤真菌感染诊治。');

-- 心内科（现有陈晓燕，新增2名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(17, '周平', '主任医师',   '心内科', '冠心病介入治疗、心律失常', '冠脉造影及支架植入术经验丰富。'),
(18, '吴华', '副主任医师', '心内科', '心力衰竭、高血压',         '慢性心衰及难治性高血压的综合管理。'),
(19, '郑杰', '主治医师',   '心内科', '心肌病、心脏超声',         '各类心肌病的诊断及心脏超声评估。');

-- 儿科（新增3名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(20, '林芳', '主任医师',   '儿科', '新生儿疾病、儿童保健',   '新生儿重症监护及儿童生长发育指导。'),
(21, '黄磊', '副主任医师', '儿科', '小儿呼吸、小儿哮喘',     '儿童哮喘及反复呼吸道感染的规范化治疗。'),
(22, '马丽', '主治医师',   '儿科', '小儿消化、营养性疾病',   '儿童消化不良及营养缺乏性疾病的诊治。');

-- 妇产科（新增3名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(23, '孙丽', '主任医师',   '妇产科', '妇科肿瘤、盆底康复',       '妇科良恶性肿瘤手术及盆底功能障碍康复。'),
(24, '朱红', '副主任医师', '妇产科', '产科高危妊娠管理',         '高危妊娠监护及危重孕产妇救治。'),
(25, '沈洁', '主治医师',   '妇产科', '妇科内分泌、不孕不育',     '月经失调、多囊卵巢综合征及不孕症诊疗。');

-- 眼科（新增3名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(26, '高峰', '主任医师',   '眼科', '白内障、青光眼手术',     '白内障超声乳化及青光眼手术。'),
(27, '罗明', '副主任医师', '眼科', '眼底病、视网膜脱落',     '糖尿病视网膜病变及视网膜脱落手术。'),
(28, '韩冰', '主治医师',   '眼科', '屈光不正、斜弱视',       '青少年近视防控及斜弱视矫正治疗。');

-- 口腔科（新增3名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(29, '曹阳', '主任医师',   '口腔科', '口腔颌面外科、种植牙',   '颌面部创伤修复及种植牙手术。'),
(30, '许文', '副主任医师', '口腔科', '牙体牙髓、牙周病',       '根管治疗及牙周系统治疗。'),
(31, '邓杰', '主治医师',   '口腔科', '口腔正畸、儿童牙科',     '固定矫治及儿童龋病防治。');

-- 中医科（新增3名）
INSERT INTO `t_doctor` (`id`, `name`, `title`, `dept_name`, `specialty`, `intro`)
VALUES
(32, '何强', '主任医师',   '中医科', '中医内科、针灸',           '中医辨证治疗脾胃病及针灸康复。'),
(33, '吕静', '副主任医师', '中医科', '中医妇科、中医养生',       '月经不调、更年期综合征及体质调理。'),
(34, '潘敏', '主治医师',   '中医科', '中医骨伤、推拿按摩',       '颈肩腰腿痛的中医综合治疗及推拿。');

-- ============================================================
-- 3. 建立医生-科室关联（t_doctor_department）
--    已有：王明华(1)→皮肤科(4)、李秀英(2)→内科(1)、
--         张建国(3)→骨科(3)、陈晓燕(4)→心内科(5)
-- ============================================================

-- 内科：张伟(5)、李明(6)、赵强(7)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(5, 1, 1),
(6, 1, 1),
(7, 1, 1);

-- 外科：刘洋(8)、陈刚(9)、王猛(10)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(8,  2, 1),
(9,  2, 1),
(10, 2, 1);

-- 骨科：赵磊(11)、孙鹏(12)、周涛(13)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(11, 3, 1),
(12, 3, 1),
(13, 3, 1);

-- 皮肤科：吴敏(14)、郑秀(15)、冯丽(16)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(14, 4, 1),
(15, 4, 1),
(16, 4, 1);

-- 心内科：周平(17)、吴华(18)、郑杰(19)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(17, 5, 1),
(18, 5, 1),
(19, 5, 1);

-- 儿科：林芳(20)、黄磊(21)、马丽(22)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(20, 6, 1),
(21, 6, 1),
(22, 6, 1);

-- 妇产科：孙丽(23)、朱红(24)、沈洁(25)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(23, 7, 1),
(24, 7, 1),
(25, 7, 1);

-- 眼科：高峰(26)、罗明(27)、韩冰(28)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(26, 8, 1),
(27, 8, 1),
(28, 8, 1);

-- 口腔科：曹阳(29)、许文(30)、邓杰(31)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(29, 9, 1),
(30, 9, 1),
(31, 9, 1);

-- 中医科：何强(32)、吕静(33)、潘敏(34)
INSERT INTO `t_doctor_department` (`doctor_id`, `department_id`, `is_primary`) VALUES
(32, 10, 1),
(33, 10, 1),
(34, 10, 1);

-- ============================================================
-- 4. 插入新增医生登录账号（t_staff，统一密码：doctor123）
-- ============================================================
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`, `doctor_id`)
VALUES
-- 内科新增
('doc_zhangwei',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '张伟', '13800001005', 'DOCTOR', 5),
('doc_liming',    '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '李明', '13800001006', 'DOCTOR', 6),
('doc_zhaoqiang', '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '赵强', '13800001007', 'DOCTOR', 7),
-- 外科
('doc_liuyang',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '刘洋', '13800002008', 'DOCTOR', 8),
('doc_chengang',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '陈刚', '13800002009', 'DOCTOR', 9),
('doc_wangmeng',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '王猛', '13800002010', 'DOCTOR', 10),
-- 骨科新增
('doc_zhaolei',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '赵磊', '13800003011', 'DOCTOR', 11),
('doc_sunpeng',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '孙鹏', '13800003012', 'DOCTOR', 12),
('doc_zhoutao',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '周涛', '13800003013', 'DOCTOR', 13),
-- 皮肤科新增
('doc_wumin',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '吴敏', '13800004014', 'DOCTOR', 14),
('doc_zhengxiu',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '郑秀', '13800004015', 'DOCTOR', 15),
('doc_fengli',    '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '冯丽', '13800004016', 'DOCTOR', 16),
-- 心内科新增
('doc_zhouping',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '周平', '13800005017', 'DOCTOR', 17),
('doc_wuhua',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '吴华', '13800005018', 'DOCTOR', 18),
('doc_zhengjie',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '郑杰', '13800005019', 'DOCTOR', 19),
-- 儿科
('doc_linfang',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '林芳', '13800006020', 'DOCTOR', 20),
('doc_huanglei',  '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '黄磊', '13800006021', 'DOCTOR', 21),
('doc_mali',      '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '马丽', '13800006022', 'DOCTOR', 22),
-- 妇产科
('doc_sunli',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '孙丽', '13800007023', 'DOCTOR', 23),
('doc_zhuhong',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '朱红', '13800007024', 'DOCTOR', 24),
('doc_shenjie',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '沈洁', '13800007025', 'DOCTOR', 25),
-- 眼科
('doc_gaofeng',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '高峰', '13800008026', 'DOCTOR', 26),
('doc_luoming',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '罗明', '13800008027', 'DOCTOR', 27),
('doc_hanbing',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '韩冰', '13800008028', 'DOCTOR', 28),
-- 口腔科
('doc_caoyang',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '曹阳', '13800009029', 'DOCTOR', 29),
('doc_xuwen',     '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '许文', '13800009030', 'DOCTOR', 30),
('doc_dengjie',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '邓杰', '13800009031', 'DOCTOR', 31),
-- 中医科
('doc_heqiang',   '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '何强', '13800010032', 'DOCTOR', 32),
('doc_lvjing',    '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '吕静', '13800010033', 'DOCTOR', 33),
('doc_panmin',    '$2b$12$aedx/GdHH0SRJHTvj/BDbOvAFEFjzFJt41Dq2WJy1j2LCgN12ZFI2', '潘敏', '13800010034', 'DOCTOR', 34);

-- ============================================================
-- 5. 新增药师账号（密码：pharma123）
-- ============================================================
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`)
VALUES
('pharmacist1', '$2b$12$ih.6DiF.7TAgqLDvo0zaK.TEU21qfyVArXTnmhC23lfaUTszAPB0u', '刘芳', '13800000011', 'PHARMACIST'),
('pharmacist2', '$2b$12$ih.6DiF.7TAgqLDvo0zaK.TEU21qfyVArXTnmhC23lfaUTszAPB0u', '陈雪', '13800000012', 'PHARMACIST'),
('pharmacist3', '$2b$12$ih.6DiF.7TAgqLDvo0zaK.TEU21qfyVArXTnmhC23lfaUTszAPB0u', '王丽', '13800000013', 'PHARMACIST');

-- ============================================================
-- 6. 新增运维账号
--    已有 admin（密码：admin123）
-- ============================================================
INSERT INTO `t_staff` (`username`, `password`, `real_name`, `phone`, `role`)
VALUES
('superadmin', '$2b$12$vX7ELwzsU3ccj1zj8XCG.eG06cZT1gEFc1gDxBKX57EqGUrGnCd1G', '超级管理员', '13800000020', 'ADMIN'),
('operator',   '$2b$12$TQY0nlbfwFoxfScURR2xN.nBMGS53l/CfffJvKreAold95Gcz.0.e', '运维操作员', '13800000021', 'ADMIN'),
('monitor',    '$2b$12$TQY0nlbfwFoxfScURR2xN.nBMGS53l/CfffJvKreAold95Gcz.0.e', '系统监控员', '13800000022', 'ADMIN');
