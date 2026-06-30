/**
 * AI 问诊模块 Mock 数据
 */
export function mockMultimodalAnalyze() {
  return Promise.resolve({
    fileUrl: 'https://via.placeholder.com/300x200?text=Medical+Image',
    draftId: 'draft_001',
    symptomDraft: '患者上传了一张皮肤照片，图片显示左前臂有一处约2cm×1.5cm的红色斑块，边缘不规则，表面轻微脱屑。根据图片分析：\n\n1. **主要症状**：局部皮肤红斑、脱屑、边界不清\n2. **可能疾病**：湿疹（概率60%）、体癣（概率25%）、接触性皮炎（概率15%）\n3. **建议检查**：皮肤刮片检查、过敏原检测\n4. **注意事项**：避免抓挠，保持患处干燥清洁，避免接触刺激性物质'
  })
}

export function mockCreateSession() {
  return Promise.resolve('SESSION_' + Date.now())
}

export function mockListSessions() {
  return Promise.resolve([
    {
      id: 1,
      sessionSn: 'SESSION_001',
      symptomDraftSummary: '皮肤红斑分析，可能为湿疹或体癣...',
      turnCount: 3,
      createTime: '2026-06-28 14:30:00'
    },
    {
      id: 2,
      sessionSn: 'SESSION_002',
      symptomDraftSummary: '头痛伴恶心，持续两天...',
      turnCount: 5,
      createTime: '2026-06-27 09:15:00'
    },
    {
      id: 3,
      sessionSn: 'SESSION_003',
      symptomDraftSummary: '咳嗽一周，有黄痰...',
      turnCount: 2,
      createTime: '2026-06-26 16:45:00'
    }
  ])
}

export function mockGetSessionHistory() {
  return Promise.resolve([
    {
      role: 'user',
      content: '我上传了一张皮肤照片，帮我分析一下',
      timestamp: '2026-06-28T14:30:00'
    },
    {
      role: 'assistant',
      content: '根据您上传的图片，我观察到左前臂有一处红色斑块，伴有脱屑。初步分析可能是湿疹或体癣。建议您注意以下几点：\n\n1. 避免抓挠患处\n2. 保持皮肤干燥清洁\n3. 建议到医院做皮肤刮片检查确认',
      citations: [
        { title: '湿疹诊疗指南', category: '临床指南', snippet: '湿疹是一种常见的炎症性皮肤病，表现为红斑、丘疹、水疱...' },
        { title: '体癣诊断标准', category: '诊断标准', snippet: '体癣由真菌感染引起，典型表现为环形红斑...' }
      ],
      timestamp: '2026-06-28T14:30:05'
    },
    {
      role: 'user',
      content: '湿疹和体癣有什么区别？',
      timestamp: '2026-06-28T14:31:00'
    },
    {
      role: 'assistant',
      content: '湿疹和体癣的主要区别：\n\n- **病因**：湿疹是过敏性炎症，体癣是真菌感染\n- **传染性**：湿疹不传染，体癣有传染性\n- **外观**：湿疹边界不清，体癣多为环形红斑\n- **确诊**：需要做真菌镜检来区分\n\n建议您尽早检查，以便对症用药。',
      citations: [],
      timestamp: '2026-06-28T14:31:10'
    }
  ])
}
