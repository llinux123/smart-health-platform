import { ref, onUnmounted } from 'vue'
import { getToken } from '@/utils/storage'

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

  async function send(requestBody) {
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

  function abort() {
    if (abortController) {
      abortController.abort()
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
