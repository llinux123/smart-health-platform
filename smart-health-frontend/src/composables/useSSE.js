import { ref, onUnmounted } from 'vue'
import { getToken } from '@/utils/storage'
import { isMockEnabled } from '@/mock'

/**
 * SSE 流式连接 composable
 * 使用 fetch + ReadableStream 处理 POST SSE（EventSource 仅支持 GET）
 */
export function useSSE() {
  const content = ref('')
  const citations = ref([])
  const isStreaming = ref(false)
  const error = ref('')

  let abortController = null
  let mockTimer = null

  // Mock 模式下的预设文本
  const mockResponses = [
    '根据您描述的症状和图片分析结果，我为您做进一步的分析：\n\n',
    '1. **初步判断**：从图片来看，皮损表现为边界较清楚的红斑，表面有少量鳞屑，这更符合**体癣**的特征。\n\n',
    '2. **建议检查**：\n   - 真菌镜检（KOH 制备）：取皮损边缘鳞屑进行检查\n   - 伍德灯检查：排除其他皮肤病变\n\n',
    '3. **治疗建议**：\n   - 外用抗真菌药物：如特比萘芬乳膏，每日 1-2 次\n   - 疗程：至少 2-4 周，症状消失后继续用药 1 周\n   - 注意保持患处干燥，避免穿紧身衣物\n\n',
    '4. **注意事项**：\n   - 避免与家人共用毛巾等个人物品\n   - 如 2 周后症状无改善，建议复诊调整治疗方案\n\n',
    '> 以上建议基于 RAG 医学知识库检索，仅供参考，具体诊疗请遵医嘱。'
  ]

  async function send(requestBody) {
    // Mock 模式
    if (isMockEnabled()) {
      return sendMock()
    }

    // 重置状态
    content.value = ''
    citations.value = []
    error.value = ''
    isStreaming.value = true

    abortController = new AbortController()
    const token = getToken()

    try {
      const response = await fetch('/api/v1/ai/consult/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(requestBody),
        signal: abortController.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // 按双换行分割 SSE 事件
        const events = buffer.split('\n\n')
        buffer = events.pop() || '' // 保留未完成的最后一个事件

        for (const event of events) {
          const lines = event.split('\n')
          for (const line of lines) {
            if (line.startsWith('data:')) {
              const jsonStr = line.slice(5).trim()
              if (!jsonStr || jsonStr === '[DONE]') continue
              try {
                const data = JSON.parse(jsonStr)
                if (data.content) {
                  content.value += data.content
                }
                if (data.citations && data.citations.length > 0) {
                  citations.value = data.citations
                }
                if (data.error) {
                  error.value = data.error
                }
              } catch (e) {
                // 忽略 JSON 解析错误（可能是不完整的行）
              }
            }
          }
        }
      }
    } catch (err) {
      if (err.name !== 'AbortError') {
        error.value = err.message || '连接失败'
      }
    } finally {
      isStreaming.value = false
    }
  }

  function sendMock() {
    content.value = ''
    citations.value = []
    error.value = ''
    isStreaming.value = true

    const fullText = mockResponses.join('')
    let index = 0

    return new Promise((resolve) => {
      mockTimer = setInterval(() => {
        if (index < fullText.length) {
          // 每次追加 1-3 个字符
          const step = Math.floor(Math.random() * 3) + 1
          content.value += fullText.slice(index, index + step)
          index += step
        } else {
          clearInterval(mockTimer)
          isStreaming.value = false
          citations.value = [
            { title: '湿疹诊疗指南（2024版）', category: '临床指南', snippet: '湿疹是一种常见的炎症性皮肤病...' },
            { title: '体癣诊断与治疗共识', category: '专家共识', snippet: '体癣由皮肤癣菌感染引起...' }
          ]
          resolve()
        }
      }, 50) // 50ms 每个 chunk，模拟流式效果
    })
  }

  function abort() {
    if (abortController) {
      abortController.abort()
    }
    if (mockTimer) {
      clearInterval(mockTimer)
    }
    isStreaming.value = false
  }

  // 组件卸载时自动断开
  onUnmounted(() => {
    abort()
  })

  return {
    content,
    citations,
    isStreaming,
    error,
    send,
    abort
  }
}
