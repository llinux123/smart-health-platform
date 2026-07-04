package com.smart.health.consultation.service.impl;

import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.config.FileUploadConfig;
import com.smart.health.consultation.dto.MultimodalAnalyzeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 多模态图片分析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalServiceImpl implements com.smart.health.consultation.service.MultimodalService {

    private final FileUploadConfig fileUploadConfig;
    @Qualifier("multimodalChatModel")
    private final ChatModel chatModel;

    @Override
    public MultimodalAnalyzeResponse analyze(List<MultipartFile> files, String type, Long patientId) {
        // 1. 参数校验
        if (files == null || files.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (type == null || (!"IMAGE".equals(type) && !"REPORT".equals(type))) {
            throw new BusinessException("图片类型参数无效，应为 IMAGE 或 REPORT");
        }

        // 2. 保存所有文件，生成路径列表和URL列表
        List<Path> savedPaths = new ArrayList<>();
        List<String> fileUrls = new ArrayList<>();
        StringBuilder fileDescBuilder = new StringBuilder();

        for (MultipartFile file : files) {
            Path targetPath = saveFile(file);
            savedPaths.add(targetPath);
            String fileUrl = "/api/v1/files/" + targetPath.getFileName().toString();
            fileUrls.add(fileUrl);

            String originalFilename = file.getOriginalFilename();
            String ext = getExtension(originalFilename);
            if (isImageFile(ext)) {
                fileDescBuilder.append("- 图片文件: ").append(originalFilename).append("\n");
            } else {
                fileDescBuilder.append("- 文档文件: ").append(originalFilename).append("\n");
            }
        }

        // 3. 调用 AI 多模态分析（图片作为Media，文档在Prompt中描述）
        String symptomDraft = callAiAnalysis(type, savedPaths, fileDescBuilder.toString());

        // 4. 生成 draftId（不再创建session，交由AnalysisPage统一创建）
        String draftId = generateDraftId();

        // 5. 返回响应
        return MultimodalAnalyzeResponse.builder()
                .fileUrls(fileUrls)
                .draftId(draftId)
                .symptomDraft(symptomDraft)
                .build();
    }

    /**
     * 保存单个文件到本地
     */
    private Path saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetPath = Paths.get(fileUploadConfig.getUploadPath(), uniqueFilename);
        try {
            Files.copy(file.getInputStream(), targetPath);
            log.info("文件上传成功: {} -> /api/v1/files/{}", targetPath, uniqueFilename);
        } catch (IOException e) {
            log.error("文件上传失败: {}", originalFilename, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
        return targetPath;
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")
                || ext.equals(".gif") || ext.equals(".webp") || ext.equals(".bmp");
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /**
     * 生成 draftId: "draft_" + timestamp + random 4 digits
     */
    private String generateDraftId() {
        return CommonConstants.DRAFT_ID_PREFIX
                + System.currentTimeMillis()
                + String.format("%04d", (int) (Math.random() * 10000));
    }

    /**
     * 调用 AI 多模态分析：将图片文件通过 UserMessage + Media 发送给视觉模型
     */
    private String callAiAnalysis(String type, List<Path> savedPaths, String fileDesc) {
        String typeLabel = "IMAGE".equals(type) ? "症状图片" : "检查报告";
        try {
            // 构建包含多张图片的多模态 UserMessage
            var builder = UserMessage.builder();

            // 构建文本提示
            StringBuilder textBuilder = new StringBuilder();
            textBuilder.append("你是一位专业的医学影像分析助手。请分析以下上传的")
                    .append(typeLabel).append("，描述观察到的症状特征，")
                    .append("给出初步可能的疾病方向和建议的进一步检查项目。")
                    .append("请以 Markdown 结构化格式输出。\n\n");
            textBuilder.append("【上传文件清单】\n").append(fileDesc);

            builder.text(textBuilder.toString());

            // 将所有图片文件作为 Media 加入
            int imageCount = 0;
            for (Path path : savedPaths) {
                String ext = getExtension(path.getFileName().toString());
                if (isImageFile(ext)) {
                    Resource imageResource = new FileSystemResource(path);
                    MimeType mimeType = determineMimeType(path);
                    builder.media(new Media(mimeType, imageResource));
                    imageCount++;
                }
            }
            log.info("调用多模态 AI 分析, type={}, 图片{}张, 文件{}个",
                    type, imageCount, savedPaths.size());

            UserMessage userMessage = builder.build();
            ChatResponse response = chatModel.call(new Prompt(userMessage));
            if (response != null && response.getResult() != null) {
                String text = response.getResult().getOutput().getText();
                if (text != null && !text.isBlank()) {
                    log.info("多模态 AI 分析成功, 响应长度={}", text.length());
                    return text;
                }
            }
        } catch (Exception e) {
            log.warn("多模态 AI 调用失败，使用模拟响应: {}", e.getMessage());
        }

        // 降级兜底（AI 调用失败或不可用时返回）
        return generateMockAnalysis(typeLabel);
    }

    /**
     * 根据文件扩展名推断 MIME 类型
     */
    private MimeType determineMimeType(Path imagePath) {
        String filename = imagePath.getFileName().toString().toLowerCase();
        if (filename.endsWith(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (filename.endsWith(".gif")) return MimeTypeUtils.IMAGE_GIF;
        if (filename.endsWith(".webp")) return MimeTypeUtils.parseMimeType("image/webp");
        // 默认 JPEG（覆盖 .jpg/.jpeg/.bmp 等）
        return MimeTypeUtils.IMAGE_JPEG;
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
