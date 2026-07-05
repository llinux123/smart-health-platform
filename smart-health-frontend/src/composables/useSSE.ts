import { ref, onUnmounted, type Ref } from 'vue'
import { getToken } from '@/utils/storage'

export interface Citation {
  title: string
  category: string
  snippet: string
}

interface SSEEvent {
  content?: string
  citations?: Citation[]
  error?: string
}

export interface UseSSEReturn {
  content: Ref<string>
  citations: Ref<Citation[]>
  isStreaming: Ref<boolean>
  error: Ref<string>
  send: (requestBody: any) => Promise<void>
  abort: () => void
}

export function useSSE(): UseSSEReturn {
  const content = ref<string>('')
  const citations = ref<Citation[]>([])
  const isStreaming = ref<boolean>(false)
  const error = ref<string>('')

  let abortController: AbortController | null = null

  async function send(requestBody: Record<string, any>): Promise<void> {
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

      if (!response.body) {
        throw new Error('浏览器不支持流式读取')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        const events = buffer.split('\n\n')
        buffer = events.pop() || ''

        for (const event of events) {
          const lines = event.split('\n')
          for (const line of lines) {
            if (line.startsWith('data:')) {
              const jsonStr = line.slice(5).trim()
              if (!jsonStr || jsonStr === '[DONE]') continue
              try {
                const data: SSEEvent = JSON.parse(jsonStr)
                if (data.content) {
                  content.value += data.content
                }
                if (data.citations && data.citations.length > 0) {
                  citations.value = data.citations
                }
                if (data.error) {
                  error.value = data.error
                }
              } catch {
                // ignore JSON parse errors
              }
            }
          }
        }
      }
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        error.value = err.message || '连接失败'
      }
    } finally {
      isStreaming.value = false
    }
  }

  function abort(): void {
    if (abortController) {
      abortController.abort()
    }
    isStreaming.value = false
  }

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
