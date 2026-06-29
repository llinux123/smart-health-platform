package com.smart.health.consultation.service.impl;

import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.config.FileUploadConfig;
import com.smart.health.consultation.dto.MultimodalAnalyzeResponse;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.service.MultimodalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 多模态图片分析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalServiceImpl implements MultimodalService {

    private final FileUploadConfig fileUploadConfig;
    private final ConsultationSessionMapper consultationSessionMapper;
    private final ChatClient chatClient;

    @Override
    public MultimodalAnalyzeResponse analyze(MultipartFile file, String type) {
        // 1. 参数校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (type == null || (!"IMAGE".equals(type) && !"REPORT".equals(type))) {
            throw new BusinessException("图片类型参数无效，应为 IMAGE 或 REPORT");
        }

        // 2. 保存文件到本地，生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        String fileUrl;
        try {
            Path targetPath = Paths.get(fileUploadConfig.getUploadPath(), uniqueFilename);
            Files.copy(file.getInputStream(), targetPath);
            fileUrl = targetPath.toString();
            log.info("文件上传成功: {}", fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        // 3. 生成 draftId: "draft_" + timestamp + random 4 digits
        String draftId = CommonConstants.DRAFT_ID_PREFIX
                + System.currentTimeMillis()
                + String.format("%04d", (int) (Math.random() * 10000));

        // 4. 调用 Spring AI 进行多模态分析
        // TODO: Spring AI 0.8.1 对多模态（图片）支持有限，当前仅支持文本 Prompt 调用。
        //       待升级至 Spring AI 1.0+ 后，使用 UserMessage + Media(MediaType.IMAGE, resource)
        //       实现真正的多模态图片分析调用。
        String symptomDraft = callAiAnalysis(type, uniqueFilename);

        // 5. 创建问诊会话记录
        ConsultationSession session = new ConsultationSession();
        session.setSessionSn(CommonConstants.SESSION_SN_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        session.setPatientId(0L); // TODO: 从当前登录用户上下文获取
        session.setDraftId(draftId);
        session.setSymptomDraft(symptomDraft);
        consultationSessionMapper.insert(session);
        log.info("问诊会话创建成功, sessionSn={}, draftId={}", session.getSessionSn(), draftId);

        // 6. 返回响应
        return MultimodalAnalyzeResponse.builder()
                .fileUrl(fileUrl)
                .draftId(draftId)
                .symptomDraft(symptomDraft)
                .build();
    }

    /**
     * 调用 AI 分析图片（模拟实现）
     * TODO: 升级 Spring AI 至 1.0+ 后替换为真实多模态调用
     */
    private String callAiAnalysis(String type, String filename) {
        String typeLabel = "IMAGE".equals(type) ? "症状图片" : "检查报告";
        try {
            // 通过 ChatClient 发送文本提示（图片传递在 0.8.1 中不支持）
            String promptText = String.format(
                    "你是一位专业的医学影像分析助手。请分析这张%s（文件名: %s），"
                            + "描述观察到的症状特征，给出初步可能的疾病方向和建议的进一步检查项目。"
                            + "请以结构化格式输出。",
                    typeLabel, filename
            );
            Prompt prompt = new Prompt(promptText);
            ChatResponse response = chatClient.call(prompt);
            if (response != null && response.getResult() != null) {
                AssistantMessage output = response.getResult().getOutput();
                if (output != null && output.getContent() != null) {
                    return output.getContent();
                }
            }
        } catch (Exception e) {
            log.warn("AI 调用失败，使用模拟响应: {}", e.getMessage());
        }

        // 模拟响应（AI 调用失败或不可用时返回）
        return generateMockAnalysis(typeLabel);
    }

    /**
     * 生成模拟的医学分析结果
     */
    private String generateMockAnalysis(String typeLabel) {
        if ("症状图片".equals(typeLabel)) {
            return """
                    ## 图片分析报告（模拟）
                    
                    ### 观察到的症状特征
                    - 皮肤表面可见局部红肿区域，边界较清晰
                    - 伴有轻微皮疹样改变
                    
                    ### 初步可能的疾病方向
                    1. 接触性皮炎
                    2. 过敏性皮肤反应
                    3. 局部感染
                    
                    ### 建议的进一步检查
                    - 血常规检查（排除感染）
                    - 过敏原检测
                    - 皮肤科专科检查
                    
                    > ⚠️ 以上为AI辅助分析结果，仅供参考，不作为诊断依据。请咨询专业医生。
                    """;
        } else {
            return """
                    ## 检查报告分析（模拟）
                    
                    ### 报告关键指标
                    - 白细胞计数: 略高于正常范围
                    - C反应蛋白: 轻度升高
                    
                    ### 初步解读
                    1. 存在轻度炎症反应迹象
                    2. 建议结合临床症状综合判断
                    
                    ### 建议
                    - 一周后复查血常规
                    - 如有持续症状，建议进一步影像学检查
                    - 注意饮食调理和休息
                    
                    > ⚠️ 以上为AI辅助分析结果，仅供参考，不作为诊断依据。请咨询专业医生。
                    """;
        }
    }
}
