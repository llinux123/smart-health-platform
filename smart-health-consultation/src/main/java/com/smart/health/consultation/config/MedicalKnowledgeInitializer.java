package com.smart.health.consultation.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.CountRequest;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 医学知识库初始化器
 * 启动时向 ES 写入示例医学知识文档，用于 RAG 检索演示
 * 直接使用 ElasticsearchClient (ES Java Client) 避免 Spring Data ES 多模块严格模式问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalKnowledgeInitializer implements ApplicationRunner {

    private static final String INDEX_NAME = "idx_medical_knowledge";
    private final ElasticsearchClient esClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. 检查索引是否存在
            boolean exists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!exists) {
                // 2. 创建索引（不指定 ik 分词器，使用默认 standard 分词器）
                esClient.indices().create(CreateIndexRequest.of(c -> c.index(INDEX_NAME)));
                log.info("创建 ES 索引: {}", INDEX_NAME);
            }

            // 3. 检查是否已有数据
            long count = esClient.count(CountRequest.of(c -> c.index(INDEX_NAME))).count();
            if (count > 0) {
                log.info("医学知识库已有数据（{}条），跳过初始化", count);
                return;
            }

            // 4. 写入示例医学知识文档
            List<MedicalKnowledgeDocument> docs = buildSampleDocuments();
            for (MedicalKnowledgeDocument doc : docs) {
                esClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).document(doc)));
            }

            // 5. 刷新索引使文档可搜索
            esClient.indices().refresh(r -> r.index(INDEX_NAME));
            log.info("医学知识库初始化完成，共导入 {} 条知识文档", docs.size());

        } catch (Exception e) {
            log.warn("医学知识库初始化失败（ES可能未就绪），RAG检索将不可用: {}", e.getMessage());
        }
    }

    private List<MedicalKnowledgeDocument> buildSampleDocuments() {
        return List.of(
                MedicalKnowledgeDocument.builder()
                        .docId("MED-001").title("接触性皮炎诊疗指南").category("皮肤科")
                        .content("接触性皮炎是皮肤或黏膜单次或多次接触外源性物质后，在接触部位甚至接触部位以外的皮肤发生的炎症反应。"
                                + "临床表现为红斑、肿胀、丘疹、水疱甚至大疱。治疗原则：1.寻找并避免接触致敏物；"
                                + "2.局部治疗：急性期用3%硼酸溶液湿敷，亚急性期外用糖皮质激素软膏；"
                                + "3.全身治疗：口服抗组胺药如氯雷他定、西替利嗪，严重者可短期口服泼尼松。"
                                + "注意事项：避免搔抓，防止继发感染。常用外用药包括糠酸莫米松乳膏、丁酸氢化可的松乳膏等。")
                        .build(),
                MedicalKnowledgeDocument.builder()
                        .docId("MED-002").title("过敏性荨麻疹诊疗指南").category("皮肤科")
                        .content("荨麻疹俗称风疹块，是由于皮肤、黏膜小血管扩张及渗透性增加而出现的一种局限性水肿反应。"
                                + "病因包括食物过敏（海鲜、坚果等）、药物过敏、感染、物理因素等。"
                                + "治疗：1.首选第二代H1抗组胺药如西替利嗪、氯雷他定、依巴斯汀；"
                                + "2.慢性荨麻疹可联合使用H2受体拮抗剂；"
                                + "3.严重急性荨麻疹可肌注肾上腺素或静脉注射糖皮质激素。"
                                + "患者教育：记录饮食日记，避免已知过敏原，随身携带抗过敏药物。")
                        .build(),
                MedicalKnowledgeDocument.builder()
                        .docId("MED-003").title("上呼吸道感染诊疗指南").category("呼吸内科")
                        .content("上呼吸道感染是鼻腔、咽或喉部急性炎症的总称，包括普通感冒、病毒性咽炎、喉炎等。"
                                + "病原体以鼻病毒、冠状病毒、腺病毒等病毒为主，少数由细菌引起。"
                                + "临床表现：鼻塞、流涕、咽痛、咳嗽、低热等。"
                                + "治疗原则：1.对症治疗为主，注意休息、多饮水；"
                                + "2.发热可用对乙酰氨基酚或布洛芬退热；"
                                + "3.鼻塞可用减充血剂如伪麻黄碱；"
                                + "4.抗生素仅在明确细菌感染时使用。普通感冒病程一般5-7天自愈。")
                        .build(),
                MedicalKnowledgeDocument.builder()
                        .docId("MED-004").title("2型糖尿病慢病管理指南").category("内分泌科")
                        .content("2型糖尿病是一种以高血糖为特征的代谢性疾病，需要长期综合管理。"
                                + "管理五驾马车：1.糖尿病教育；2.饮食控制（低GI饮食，每日总热量计算）；"
                                + "3.运动治疗（每周至少150分钟中等强度有氧运动）；"
                                + "4.药物治疗（二甲双胍为一线用药，可联合SGLT2抑制剂、DPP-4抑制剂等）；"
                                + "5.血糖监测（空腹血糖目标4.4-7.0mmol/L，餐后2h<10.0mmol/L，HbA1c<7%）。"
                                + "并发症筛查：每年检查眼底、肾功能、足部、神经传导。")
                        .build(),
                MedicalKnowledgeDocument.builder()
                        .docId("MED-005").title("高血压患者用药指导").category("心血管内科")
                        .content("高血压诊断标准：收缩压≥140mmHg和/或舒张压≥90mmHg（非同日3次测量）。"
                                + "降压药物五大类：1.CCB（氨氯地平、硝苯地平控释片）；"
                                + "2.ACEI（依那普利、贝那普利）；3.ARB（缬沙坦、氯沙坦）；"
                                + "4.利尿剂（氢氯噻嗪、吲达帕胺）；5.β受体阻滞剂（美托洛尔、比索洛尔）。"
                                + "用药原则：从小剂量开始，优先选择长效制剂，联合用药优于大剂量单药。"
                                + "目标血压：一般患者<140/90mmHg，合并糖尿病或肾病患者<130/80mmHg。"
                                + "注意事项：ACEI与ARB不宜联用，ACEI可引起干咳，CCB可引起踝部水肿。")
                        .build(),
                MedicalKnowledgeDocument.builder()
                        .docId("MED-006").title("湿疹诊疗指南").category("皮肤科")
                        .content("湿疹是由多种内外因素引起的瘙痒性皮肤炎症，具有多形性皮损和渗出倾向。"
                                + "分类：急性湿疹（红斑、丘疹、水疱、渗出）、亚急性湿疹、慢性湿疹（苔藓样变）。"
                                + "治疗：1.基础治疗：保湿润肤，避免刺激因素；"
                                + "2.外用糖皮质激素：急性期用乳剂，慢性期用软膏（如糠酸莫米松、卤米松）；"
                                + "3.外用钙调磷酸酶抑制剂（他克莫司软膏）适用于面部和间擦部位；"
                                + "4.口服抗组胺药止痒（氯雷他定、西替利嗪）；"
                                + "5.严重泛发性湿疹可短期系统使用糖皮质激素。"
                                + "患者教育：避免热水烫洗、搔抓，穿着棉质衣物，使用温和洗涤用品。")
                        .build(),
                MedicalKnowledgeDocument.builder()
                        .docId("MED-007").title("胃食管反流病诊疗指南").category("消化内科")
                        .content("胃食管反流病(GERD)是指胃内容物反流入食管引起不适症状和/或并发症。"
                                + "典型症状：烧心、反酸。非典型症状：胸痛、慢性咳嗽、声嘶。"
                                + "诊断：主要依据典型症状，必要时行胃镜或24小时食管pH监测。"
                                + "治疗：1.生活方式调整：抬高床头、避免饱餐和睡前进食、减重、戒烟酒；"
                                + "2.药物治疗：质子泵抑制剂(PPI)为首选（奥美拉唑20mg bid，疗程8周）；"
                                + "3.H2受体拮抗剂（法莫替丁）适用于轻症；"
                                + "4.促胃动力药（多潘立酮）可辅助使用。"
                                + "注意事项：长期PPI使用需关注骨质疏松、低镁血症等风险。")
                        .build(),
                MedicalKnowledgeDocument.builder()
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
                        .build()
        );
    }
}
