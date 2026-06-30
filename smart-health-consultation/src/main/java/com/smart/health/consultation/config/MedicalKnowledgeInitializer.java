package com.smart.health.consultation.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 医学知识库初始化器
 * 启动时创建 ES 索引（ik 分词 + dense_vector mapping）并写入示例医学知识文档
 * 直接使用 ElasticsearchClient (ES Java Client) 避免 Spring Data ES 多模块严格模式问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalKnowledgeInitializer implements ApplicationRunner {

    private static final String INDEX_NAME = "idx_medical_knowledge";
    private static final int EMBEDDING_DIMS = 1536;

    private final ElasticsearchClient esClient;

    @Autowired(required = false)
    private EmbeddingClient embeddingClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!exists) {
                createIndexWithMapping();
                log.info("创建 ES 索引（含 mapping）: {}", INDEX_NAME);
            }

            long count = esClient.count(CountRequest.of(c -> c.index(INDEX_NAME))).count();
            if (count > 0) {
                log.info("医学知识库已有数据（{}条），跳过初始化", count);
                return;
            }

            List<MedicalKnowledgeDocument> docs = buildSampleDocuments();
            for (MedicalKnowledgeDocument doc : docs) {
                // 生成 embedding 向量
                if (embeddingClient != null) {
                    try {
                        List<Double> embedding = embeddingClient.embed(doc.getTitle() + " " + doc.getContent());
                        List<Float> floatEmbedding = embedding.stream()
                                .map(Double::floatValue)
                                .toList();
                        doc.setEmbedding(floatEmbedding);
                    } catch (Exception e) {
                        log.warn("Embedding 生成失败（文档 {}），跳过向量写入: {}", doc.getDocId(), e.getMessage());
                    }
                }
                esClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).document(doc)));
            }

            esClient.indices().refresh(r -> r.index(INDEX_NAME));
            log.info("医学知识库初始化完成，共导入 {} 条知识文档（embedding={})",
                    docs.size(), embeddingClient != null ? "已启用" : "未启用");

        } catch (Exception e) {
            log.warn("医学知识库初始化失败（ES可能未就绪），RAG检索将不可用: {}", e.getMessage());
        }
    }

    /**
     * 创建索引并指定 mapping（ik 分词器 + dense_vector）
     * 若 ik 不可用则降级为 standard 分词器
     */
    private void createIndexWithMapping() {
        String analyzer = detectIkAnalyzer();
        try {
            esClient.indices().create(CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .mappings(m -> m
                            .properties("docId", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t
                                    .analyzer(analyzer)
                                    .searchAnalyzer("ik_smart".equals(analyzer) ? "ik_smart" : "standard")))
                            .properties("content", p -> p.text(t -> t
                                    .analyzer(analyzer)
                                    .searchAnalyzer("ik_smart".equals(analyzer) ? "ik_smart" : "standard")))
                            .properties("category", p -> p.keyword(k -> k))
                            .properties("embedding", p -> p.denseVector(dv -> dv
                                    .dims(EMBEDDING_DIMS)
                                    .index(true)
                                    .similarity("cosine"))))
            ));
        } catch (Exception e) {
            log.error("创建索引 mapping 失败: {}", e.getMessage());
            throw new RuntimeException("创建索引 mapping 失败", e);
        }
    }

    /**
     * 检测 ik 分词器是否可用，不可用则降级为 standard
     */
    private String detectIkAnalyzer() {
        try {
            esClient.indices().analyze(a -> a
                    .analyzer("ik_max_word")
                    .text("测试分词"));
            log.info("检测到 ik 分词器可用，使用 ik_max_word");
            return "ik_max_word";
        } catch (Exception e) {
            log.warn("ik 分词器不可用，降级使用 standard 分词器（生产环境请安装 analysis-ik 插件）");
            return "standard";
        }
    }

    private List<MedicalKnowledgeDocument> buildSampleDocuments() {
        List<MedicalKnowledgeDocument> docs = new ArrayList<>();

        // MED-001 ~ MED-008（原有文档）
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-001").title("接触性皮炎诊疗指南").category("皮肤科")
                .content("接触性皮炎是皮肤或黏膜单次或多次接触外源性物质后，在接触部位甚至接触部位以外的皮肤发生的炎症反应。"
                        + "临床表现为红斑、肿胀、丘疹、水疱甚至大疱。治疗原则：1.寻找并避免接触致敏物；"
                        + "2.局部治疗：急性期用3%硼酸溶液湿敷，亚急性期外用糖皮质激素软膏；"
                        + "3.全身治疗：口服抗组胺药如氯雷他定、西替利嗪，严重者可短期口服泼尼松。"
                        + "注意事项：避免搔抓，防止继发感染。常用外用药包括糠酸莫米松乳膏、丁酸氢化可的松乳膏等。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-002").title("过敏性荨麻疹诊疗指南").category("皮肤科")
                .content("荨麻疹俗称风疹块，是由于皮肤、黏膜小血管扩张及渗透性增加而出现的一种局限性水肿反应。"
                        + "病因包括食物过敏（海鲜、坚果等）、药物过敏、感染、物理因素等。"
                        + "治疗：1.首选第二代H1抗组胺药如西替利嗪、氯雷他定、依巴斯汀；"
                        + "2.慢性荨麻疹可联合使用H2受体拮抗剂；"
                        + "3.严重急性荨麻疹可肌注肾上腺素或静脉注射糖皮质激素。"
                        + "患者教育：记录饮食日记，避免已知过敏原，随身携带抗过敏药物。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-003").title("上呼吸道感染诊疗指南").category("呼吸内科")
                .content("上呼吸道感染是鼻腔、咽或喉部急性炎症的总称，包括普通感冒、病毒性咽炎、喉炎等。"
                        + "病原体以鼻病毒、冠状病毒、腺病毒等病毒为主，少数由细菌引起。"
                        + "临床表现：鼻塞、流涕、咽痛、咳嗽、低热等。"
                        + "治疗原则：1.对症治疗为主，注意休息、多饮水；"
                        + "2.发热可用对乙酰氨基酚或布洛芬退热；"
                        + "3.鼻塞可用减充血剂如伪麻黄碱；"
                        + "4.抗生素仅在明确细菌感染时使用。普通感冒病程一般5-7天自愈。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-004").title("2型糖尿病慢病管理指南").category("内分泌科")
                .content("2型糖尿病是一种以高血糖为特征的代谢性疾病，需要长期综合管理。"
                        + "管理五驾马车：1.糖尿病教育；2.饮食控制（低GI饮食，每日总热量计算）；"
                        + "3.运动治疗（每周至少150分钟中等强度有氧运动）；"
                        + "4.药物治疗（二甲双胍为一线用药，可联合SGLT2抑制剂、DPP-4抑制剂等）；"
                        + "5.血糖监测（空腹血糖目标4.4-7.0mmol/L，餐后2h<10.0mmol/L，HbA1c<7%）。"
                        + "并发症筛查：每年检查眼底、肾功能、足部、神经传导。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-005").title("高血压患者用药指导").category("心血管内科")
                .content("高血压诊断标准：收缩压≥140mmHg和/或舒张压≥90mmHg（非同日3次测量）。"
                        + "降压药物五大类：1.CCB（氨氯地平、硝苯地平控释片）；"
                        + "2.ACEI（依那普利、贝那普利）；3.ARB（缬沙坦、氯沙坦）；"
                        + "4.利尿剂（氢氯噻嗪、吲达帕胺）；5.β受体阻滞剂（美托洛尔、比索洛尔）。"
                        + "用药原则：从小剂量开始，优先选择长效制剂，联合用药优于大剂量单药。"
                        + "目标血压：一般患者<140/90mmHg，合并糖尿病或肾病患者<130/80mmHg。"
                        + "注意事项：ACEI与ARB不宜联用，ACEI可引起干咳，CCB可引起踝部水肿。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-006").title("湿疹诊疗指南").category("皮肤科")
                .content("湿疹是由多种内外因素引起的瘙痒性皮肤炎症，具有多形性皮损和渗出倾向。"
                        + "分类：急性湿疹（红斑、丘疹、水疱、渗出）、亚急性湿疹、慢性湿疹（苔藓样变）。"
                        + "治疗：1.基础治疗：保湿润肤，避免刺激因素；"
                        + "2.外用糖皮质激素：急性期用乳剂，慢性期用软膏（如糠酸莫米松、卤米松）；"
                        + "3.外用钙调磷酸酶抑制剂（他克莫司软膏）适用于面部和间擦部位；"
                        + "4.口服抗组胺药止痒（氯雷他定、西替利嗪）；"
                        + "5.严重泛发性湿疹可短期系统使用糖皮质激素。"
                        + "患者教育：避免热水烫洗、搔抓，穿着棉质衣物，使用温和洗涤用品。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-007").title("胃食管反流病诊疗指南").category("消化内科")
                .content("胃食管反流病(GERD)是指胃内容物反流入食管引起不适症状和/或并发症。"
                        + "典型症状：烧心、反酸。非典型症状：胸痛、慢性咳嗽、声嘶。"
                        + "诊断：主要依据典型症状，必要时行胃镜或24小时食管pH监测。"
                        + "治疗：1.生活方式调整：抬高床头、避免饱餐和睡前进食、减重、戒烟酒；"
                        + "2.药物治疗：质子泵抑制剂(PPI)为首选（奥美拉唑20mg bid，疗程8周）；"
                        + "3.H2受体拮抗剂（法莫替丁）适用于轻症；"
                        + "4.促胃动力药（多潘立酮）可辅助使用。"
                        + "注意事项：长期PPI使用需关注骨质疏松、低镁血症等风险。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-008").title("儿童发热处理指南").category("儿科")
                .content("儿童发热定义：体温≥37.5℃（腋温）。发热是机体防御反应的表现，本身不会造成伤害。"
                        + "处理原则：1.体温<38.5℃一般无需退热药，物理降温即可；"
                        + "2.体温≥38.5℃可口服对乙酰氨基酚（10-15mg/kg/次）或布洛芬（5-10mg/kg/次）；"
                        + "3.两种退热药不建议交替使用；"
                        + "4.补充液体，防止脱水；"
                        + "5.观察精神状态、皮疹、呼吸等伴随症状。"
                        + "就医指征：3月龄以下婴儿发热；持续高热>3天；精神萎靡；出现惊厥；"
                        + "伴有剧烈头痛、颈部僵硬、皮疹等。"
                        + "禁忌：不使用阿司匹林退热（Reye综合征风险），不使用酒精擦浴。")
                .build());

        // MED-009 ~ MED-020（新增文档，覆盖更多科室）
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-009").title("腰椎间盘突出症诊疗指南").category("骨科")
                .content("腰椎间盘突出症是腰椎间盘退变后纤维环破裂，髓核组织突出压迫神经根或马尾神经引起的综合征。"
                        + "典型表现：腰痛伴下肢放射痛（坐骨神经痛），直腿抬高试验阳性。"
                        + "诊断：MRI为首选影像学检查，可明确突出部位和程度。"
                        + "治疗：1.保守治疗（80%患者可缓解）：卧床休息、NSAIDs（布洛芬、塞来昔布）、"
                        + "物理治疗（牵引、热敷）；2.微创介入：经皮椎间盘摘除术、射频消融；"
                        + "3.手术治疗：椎间盘摘除术（适应证：保守6周无效、进行性神经功能损害、马尾综合征）。"
                        + "预防：避免久坐弯腰搬重物，加强腰背肌锻炼（小燕飞、五点支撑）。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-010").title("偏头痛诊疗指南").category("神经内科")
                .content("偏头痛是一种反复发作的原发性头痛，特征为中重度搏动性头痛，多为单侧，持续4-72小时。"
                        + "伴随症状：恶心、呕吐、畏光、畏声。部分患者有先兆（视觉闪光、感觉异常）。"
                        + "诱发因素：压力、睡眠不足、特定食物（巧克力、奶酪）、月经期、强光。"
                        + "急性期治疗：1.轻中度：NSAIDs（布洛芬400mg、对乙酰氨基酚1000mg）；"
                        + "2.中重度：曲普坦类（舒马曲普坦50-100mg口服）；"
                        + "3.预防治疗：每月发作≥4次考虑，首选β受体阻滞剂（普萘洛尔）或氟桂利嗪。"
                        + "患者教育：记录头痛日记，识别并避免诱因。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-011").title("干眼症诊疗指南").category("眼科")
                .content("干眼症是由于泪液分泌不足或蒸发过快导致眼表损害的疾病。"
                        + "症状：眼干涩、异物感、烧灼感、视疲劳、畏光、视力波动。"
                        + "诊断：泪膜破裂时间（BUT）<10秒为异常，Schirmer试验<5mm/5min为泪液分泌不足。"
                        + "治疗：1.人工泪液（玻璃酸钠滴眼液为首选，每日4-6次）；"
                        + "2.睑板腺功能障碍：热敷+睑板腺按摩；"
                        + "3.严重干眼：环孢素A滴眼液或泪点栓塞；"
                        + "4.生活调整：减少屏幕使用时间，增加环境湿度，补充Omega-3脂肪酸。"
                        + "注意：长期使用含防腐剂滴眼液可加重眼表损害，推荐使用无防腐剂单剂量包装。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-012").title("过敏性鼻炎诊疗指南").category("耳鼻喉科")
                .content("过敏性鼻炎是IgE介导的鼻黏膜慢性炎症，分为季节性和常年性。"
                        + "典型症状：阵发性喷嚏、清水样鼻涕、鼻塞、鼻痒，可伴眼痒、流泪。"
                        + "诊断：皮肤点刺试验或血清特异性IgE检测明确过敏原。"
                        + "治疗阶梯：1.避免接触过敏原（基础措施）；"
                        + "2.鼻用糖皮质激素（一线用药：糠酸莫米松、丙酸氟替卡松，疗程≥4周）；"
                        + "3.口服第二代抗组胺药（氯雷他定、西替利嗪）；"
                        + "4.白三烯受体拮抗剂（孟鲁司特）适用于合并哮喘者；"
                        + "5.特异性免疫治疗（脱敏治疗）：适用于尘螨过敏、药物控制不佳者。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-013").title("泌尿系感染诊疗指南").category("泌尿科")
                .content("泌尿系感染(UTI)是病原微生物在泌尿系统内繁殖引起的炎症，女性发病率显著高于男性。"
                        + "分类：下尿路感染（膀胱炎、尿道炎）和上尿路感染（急性肾盂肾炎）。"
                        + "膀胱炎表现：尿频、尿急、尿痛、耻骨上不适，一般无发热。"
                        + "肾盂肾炎表现：寒战高热、腰痛、肾区叩击痛，伴膀胱刺激征。"
                        + "诊断：中段尿培养+药敏试验，菌落计数≥10^5 CFU/mL有诊断意义。"
                        + "治疗：1.单纯性膀胱炎：呋喃妥因100mg bid×5天或磷霉素氨丁三醇3g单次；"
                        + "2.急性肾盂肾炎：左氧氟沙星500mg qd×7天或头孢曲松1g iv qd；"
                        + "3.反复发作者需排查泌尿系结构异常。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-014").title("功能失调性子宫出血诊疗指南").category("妇产科")
                .content("功能失调性子宫出血(功血)是由于下丘脑-垂体-卵巢轴功能失调引起的异常子宫出血。"
                        + "分类：无排卵性功血（青春期和围绝经期多见）和排卵性功血（育龄期多见）。"
                        + "无排卵性功血表现：月经周期紊乱、经期长短不一、经量不定。"
                        + "诊断：排除器质性病变后诊断，B超排除子宫肌瘤/息肉，必要时诊刮送病理。"
                        + "治疗：1.急性大出血：大剂量雌激素止血或诊刮；"
                        + "2.调整周期：口服避孕药（妈富隆）或后半周期孕激素（地屈孕酮10-20mg/d×10-14天）；"
                        + "3.围绝经期功血需警惕子宫内膜病变，必要时行宫腔镜检查。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-015").title("急性扁桃体炎诊疗指南").category("耳鼻喉科")
                .content("急性扁桃体炎是腭扁桃体的急性非特异性炎症，常继发于上呼吸道感染。"
                        + "病原体：A组β溶血性链球菌最常见，其次为肺炎链球菌、金黄色葡萄球菌。"
                        + "临床表现：咽痛剧烈（吞咽时加重）、发热、扁桃体充血肿大、表面可见脓性分泌物。"
                        + "诊断：根据典型症状+体征，咽拭子培养可明确病原体，ASO滴度升高提示链球菌感染。"
                        + "治疗：1.抗生素：青霉素V钾为首选（疗程10天），过敏者用阿奇霉素；"
                        + "2.对症治疗：退热（布洛芬）、咽痛含片、盐水漱口；"
                        + "3.手术指征：每年发作≥5-7次或并发扁桃体周围脓肿者考虑扁桃体切除。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-016").title("支气管哮喘慢病管理指南").category("呼吸内科")
                .content("支气管哮喘是气道慢性炎症性疾病，特征为可逆性气流受限和气道高反应性。"
                        + "典型症状：反复发作的喘息、气促、胸闷、咳嗽，夜间和清晨加重。"
                        + "诊断：肺功能检查（支气管舒张试验阳性或激发试验阳性），PEF变异率>20%。"
                        + "分级治疗：1.间歇状态（第1级）：按需使用SABA（沙丁胺醇）；"
                        + "2.轻度持续（第2级）：低剂量ICS（布地奈德200μg/d）；"
                        + "3.中度持续（第3级）：低剂量ICS+LABA（布地奈德/福莫特罗）；"
                        + "4.重度持续（第4级）：中剂量ICS+LABA+LAMA或生物制剂（奥马珠单抗）。"
                        + "患者教育：掌握吸入装置正确用法，制定哮喘行动计划，定期复查肺功能。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-017").title("痛风诊疗指南").category("风湿免疫科")
                .content("痛风是嘌呤代谢紊乱导致血尿酸升高，尿酸盐结晶沉积在关节及周围组织引起的炎症性疾病。"
                        + "诊断标准：血尿酸>420μmol/L（男性）或>360μmol/L（女性），关节液找到尿酸盐结晶可确诊。"
                        + "急性期治疗：1.首选NSAIDs（依托考昔120mg/d，疗程≤8天）；"
                        + "2.秋水仙碱（首剂1.0mg，1小时后追加0.5mg）；"
                        + "3.糖皮质激素（口服泼尼松30-35mg/d×5天）适用于NSAIDs禁忌者。"
                        + "缓解期降尿酸治疗：1.别嘌醇（起始100mg/d，逐渐加量至300mg/d）；"
                        + "2.非布司他（40-80mg/d）适用于别嘌醇不耐受者；"
                        + "3.目标血尿酸<360μmol/L，有痛风石者<300μmol/L。"
                        + "生活方式：限制高嘌呤食物（内脏、海鲜、啤酒），多饮水（>2000ml/d），控制体重。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-018").title("甲状腺功能减退症诊疗指南").category("内分泌科")
                .content("甲状腺功能减退症(甲减)是由于甲状腺激素合成和分泌减少导致的全身代谢减低综合征。"
                        + "病因：桥本甲状腺炎最常见，其次为甲状腺手术/放射性碘治疗后、碘缺乏。"
                        + "临床表现：乏力、畏寒、体重增加、便秘、皮肤干燥、记忆力减退、心率减慢。"
                        + "诊断：TSH升高+FT4降低为原发性甲减；亚临床甲减：TSH升高但FT4正常。"
                        + "治疗：左甲状腺素钠(L-T4)替代治疗，起始25-50μg/d，每4-6周调整剂量。"
                        + "目标：TSH恢复正常（0.5-2.5mIU/L），成人维持量约1.6μg/kg/d。"
                        + "特殊人群：妊娠期甲减需积极治疗（TSH目标<2.5），老年患者从小剂量起始。"
                        + "注意事项：L-T4需空腹服用，与铁剂、钙剂间隔4小时以上。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-019").title("脑梗死急性期诊疗指南").category("神经内科")
                .content("脑梗死（缺血性脑卒中）占脑卒中的70-80%，是致残和致死的主要原因之一。"
                        + "急性期诊断：头颅CT排除出血后，MRI-DWI可早期明确梗死灶。"
                        + "病因分型(TOAST)：大动脉粥样硬化型、心源性栓塞型、小动脉闭塞型、其他明确病因型、不明原因型。"
                        + "超急性期治疗：1.静脉溶栓（发病4.5h内：阿替普酶0.9mg/kg，最大90mg）；"
                        + "2.血管内取栓（前循环大血管闭塞，发病6-24h内经影像评估可获益）。"
                        + "急性期一般治疗：1.抗血小板（阿司匹林100-300mg/d，发病48h内开始）；"
                        + "2.他汀类（阿托伐他汀40-80mg/d）；3.控制血压（急性期不宜过度降压）；"
                        + "4.抗凝仅适用于心源性栓塞（发病后4-14天开始，视梗死面积而定）。"
                        + "二级预防：控制高血压、糖尿病、房颤抗凝、颈动脉狭窄评估。")
                .build());
        docs.add(MedicalKnowledgeDocument.builder()
                .docId("MED-020").title("失眠症诊疗指南").category("精神心理科")
                .content("失眠症是指入睡困难（>30分钟）、睡眠维持障碍（夜间觉醒≥2次）或早醒，导致睡眠质量不满意。"
                        + "诊断标准：上述症状每周≥3次，持续≥3个月，并影响日间功能。需排除其他精神/躯体疾病所致。"
                        + "非药物治疗（首选）：1.认知行为治疗(CBT-I)：睡眠限制、刺激控制、认知重构；"
                        + "2.睡眠卫生教育：固定作息时间、避免午睡>30分钟、睡前避免咖啡因和剧烈运动。"
                        + "药物治疗：1.非苯二氮卓类（首选）：唑吡坦5-10mg、右佐匹克隆1-3mg；"
                        + "2.苯二氮卓类：艾司唑仑1-2mg（短期使用，避免依赖）；"
                        + "3.褪黑素受体激动剂：雷美替胺8mg（适用于入睡困难）；"
                        + "4.伴有焦虑/抑郁的失眠：曲唑酮25-100mg或米氮平7.5-15mg。"
                        + "用药原则：按需间断给药，疗程一般不超过4周，避免突然停药。")
                .build());

        return docs;
    }
}
