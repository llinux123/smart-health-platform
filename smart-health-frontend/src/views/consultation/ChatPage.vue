<template>
  <div class="chat-page">
    <van-nav-bar
      :title="`AI 问诊 (${sessionSn?.slice(0, 12)}...)`"
      left-arrow
      @click-left="$router.back()"
    />

    <div class="chat-messages" ref="messagesRef">
      <!-- 历史消息 -->
      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
        <div :class="['message-bubble', msg.role]">
          <div class="message-avatar">
            <van-icon v-if="msg.role === 'user'" name="manager-o" size="20" />
            <van-icon v-else name="service-o" size="20" />
          </div>
          <div class="message-content">
            <div v-if="msg.role === 'assistant'" class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
            <div v-else>{{ msg.content }}</div>

            <!-- 引用来源 -->
            <div v-if="msg.citations && msg.citations.length" class="citations">
              <van-collapse v-model="msg.showCitations">
                <van-collapse-item title="📚 引用来源" :name="index">
                  <div v-for="(cite, ci) in msg.citations" :key="ci" class="citation-item">
                    <strong>{{ cite.title }}</strong>
                    <van-tag type="primary" size="mini">{{ cite.category }}</van-tag>
                    <p>{{ cite.snippet }}</p>
                  </div>
                </van-collapse-item>
              </van-collapse>
            </div>
          </div>
        </div>
      </div>

      <!-- 当前 AI 流式回复 -->
      <div v-if="isStreaming" class="message-wrapper">
        <div class="message-bubble assistant">
          <div class="message-avatar">
            <van-icon name="service-o" size="20" />
          </div>
          <div class="message-content">
            <div class="markdown-body" v-html="renderMarkdown(streamContent)"></div>
            <van-loading v-if="!streamContent" size="20">AI 思考中...</van-loading>
          </div>
        </div>
      </div>
    </div>

    <!-- 引用来源（流式完成后显示） -->
    <div v-if="streamCitations.length && !isStreaming" class="stream-citations card">
      <van-collapse v-model="showStreamCitations">
        <van-collapse-item title="📚 引用来源" name="stream">
          <div v-for="(cite, ci) in streamCitations" :key="ci" class="citation-item">
            <strong>{{ cite.title }}</strong>
            <van-tag type="primary" size="mini">{{ cite.category }}</van-tag>
            <p>{{ cite.snippet }}</p>
          </div>
        </van-collapse-item>
      </van-collapse>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <van-field
        v-model="inputMessage"
        placeholder="请输入您的问题..."
        :disabled="isStreaming"
        @keyup.enter="sendMessage"
      >
        <template #button>
          <van-button
            size="small"
            type="primary"
            :loading="isStreaming"
            :disabled="!inputMessage.trim()"
            @click="sendMessage"
          >
            发送
          </van-button>
        </template>
      </van-field>

      <!-- 推荐问题 -->
      <div v-if="messages.length <= 1 && !isStreaming" class="quick-questions">
        <van-tag
          v-for="q in quickQuestions"
          :key="q"
          plain
          type="primary"
          class="quick-tag"
          @click="inputMessage = q; sendMessage()"
        >
          {{ q }}
        </van-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { getSessionHistory } from '@/api/consult'
import { useSSE } from '@/composables/useSSE'

const route = useRoute()
const sessionSn = route.params.sessionSn

const messages = ref([])
const inputMessage = ref('')
const messagesRef = ref(null)
const showStreamCitations = ref([])

const { content: streamContent, citations: streamCitations, isStreaming, send } = useSSE()

const quickQuestions = [
  '这个症状严重吗？',
  '需要做什么检查？',
  '有什么治疗建议？',
  '日常需要注意什么？'
]

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true
})

function renderMarkdown(text) {
  if (!text) return ''
  return marked(text)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 监听流式内容变化，自动滚动
watch(streamContent, () => {
  scrollToBottom()
})

onMounted(async () => {
  // 加载历史消息
  try {
    const history = await getSessionHistory(sessionSn)
    messages.value = history.map(msg => ({
      ...msg,
      showCitations: []
    }))
    scrollToBottom()
  } catch (err) {
    // 错误已在拦截器中处理
  }
})

async function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message || isStreaming.value) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: message,
    citations: [],
    showCitations: []
  })
  inputMessage.value = ''
  scrollToBottom()

  // 发送 SSE 请求
  await send({
    sessionId: sessionSn,
    message
  })

  // 流式完成后，将 AI 回复添加到消息列表
  if (streamContent.value) {
    messages.value.push({
      role: 'assistant',
      content: streamContent.value,
      citations: streamCitations.value || [],
      showCitations: []
    })
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 8px;
}

.message-wrapper {
  margin-bottom: 16px;
}

.message-bubble {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.message-bubble.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #e8e8e8;
}

.message-bubble.user .message-avatar {
  background: #1890FF;
  color: #fff;
}

.message-bubble.assistant .message-avatar {
  background: #52C41A;
  color: #fff;
}

.message-content {
  max-width: 75%;
  background: #fff;
  padding: 12px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.message-bubble.user .message-content {
  background: #1890FF;
  color: #fff;
}

.markdown-body {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.markdown-body :deep(p) {
  margin: 4px 0;
}

.markdown-body :deep(strong) {
  color: #1890FF;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 16px;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid #1890FF;
  padding-left: 8px;
  margin: 8px 0;
  color: #666;
}

.citations {
  margin-top: 8px;
  border-top: 1px dashed #e8e8e8;
  padding-top: 8px;
}

.citation-item {
  margin-bottom: 8px;
  font-size: 12px;
}

.citation-item strong {
  display: block;
  margin-bottom: 2px;
}

.citation-item p {
  color: #666;
  margin-top: 4px;
}

.stream-citations {
  margin: 0 16px 8px;
}

.citation-item {
  margin-bottom: 8px;
  font-size: 12px;
}

.chat-input-area {
  background: #fff;
  border-top: 1px solid #e8e8e8;
  padding-bottom: env(safe-area-inset-bottom);
}

.quick-questions {
  padding: 8px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-tag {
  cursor: pointer;
}
</style>
