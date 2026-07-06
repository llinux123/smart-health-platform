<script setup lang="ts">
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showLoadingToast, closeToast, showToast } from 'vant'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useConsultationStore } from '@/stores/consultation'
import { useSSE } from '@/composables/useSSE'
import { getSessionDetail, type SessionInfo } from '@/api/consult'
import RatingDialog from './RatingDialog.vue'

const route = useRoute()
const router = useRouter()
const store = useConsultationStore()
const sessionSn = route.params.sessionSn as string

// ============ 会话信息 ============
const sessionInfo = ref<SessionInfo | null>(null)
const isCompleted = computed(() => sessionInfo.value?.status === 'COMPLETED')

// ============ 初始分析结果（作为第一条对话） ============
const hasInitialAnalysis = computed(() => {
  return sessionInfo.value?.symptomDraft && sessionInfo.value.symptomDraft.length > 0
})

const initialFileUrls = computed(() => {
  if (!sessionInfo.value?.fileUrls) return []
  return sessionInfo.value.fileUrls.split(',').filter(Boolean)
})

/** 合并初始分析结果 + API轮次作为展示列表 */
const displayTurns = computed(() => {
  const result = []
  if (hasInitialAnalysis.value) {
    result.push({
      id: 0,
      turnNumber: 0,
      userMessage: '',
      assistantMessage: sessionInfo.value!.symptomDraft,
      citations: [],
      createTime: sessionInfo.value?.createTime || ''
    })
  }
  result.push(...store.turns)
  return result
})

// ============ 对话轮次 ============
const messagesRef = ref<HTMLElement | null>(null)
const currentTurnNumber = ref(0)

// ============ 输入 ============
const inputMessage = ref('')

// ============ SSE 流 ============
const { content: streamContent, citations: streamCitations, isStreaming, send } = useSSE()
const showStreamCitations = ref<string[]>([])

// ============ 评分弹窗 ============
const showRating = ref(false)

// ============ 重新生成 ============
const regenerating = ref(false)

// ============ 快捷问题 ============
const quickQuestions = [
  '这个症状严重吗？',
  '需要做什么检查？',
  '有什么治疗建议？',
  '日常需要注意什么？'
]

// marked 配置
marked.setOptions({ breaks: true, gfm: true })

function isImageUrl(url: string) {
  const ext = url.toLowerCase().split('.').pop() || ''
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)
}

function getFileName(url: string) {
  const parts = url.split('/')
  return parts[parts.length - 1]
}

function renderMarkdown(text: string) {
  if (!text) return ''
  return DOMPurify.sanitize(marked(text) as string)
}

// ============ 生命周期 ============
onMounted(async () => {
  // 加载会话详情
  try {
    sessionInfo.value = await getSessionDetail(sessionSn)
  } catch {
    // 错误已在拦截器中处理
  }

  // 加载最新轮次
  await store.fetchTurns(sessionSn, 1)
  if (store.turns.length > 0) {
    currentTurnNumber.value = store.turns[store.turns.length - 1].turnNumber
  }
  scrollToBottom()
})

// ============ 滚动 ============
function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function handleScroll() {
  if (!messagesRef.value) return
  // 滚动到顶部时加载更多历史轮次
  if (messagesRef.value.scrollTop < 50 && store.hasMoreTurns && !store.turnsLoading) {
    const prevHeight = messagesRef.value.scrollHeight
    store.loadOlderTurns(sessionSn).then(() => {
      nextTick(() => {
        if (messagesRef.value) {
          const newHeight = messagesRef.value.scrollHeight
          messagesRef.value.scrollTop = newHeight - prevHeight
        }
      })
    })
  }
}

// 流式内容变化时自动滚动
watch(streamContent, () => {
  scrollToBottom()
})

// ============ 发送消息 ============
async function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message || isStreaming.value || isCompleted.value) return

  inputMessage.value = ''
  scrollToBottom()

  // 发送 SSE 请求
  await send({ sessionId: sessionSn, message })

  // 流式完成后，刷新轮次数据（延迟确保后端持久化完成）
  if (streamContent.value) {
    await new Promise(resolve => setTimeout(resolve, 500))
    await store.fetchTurns(sessionSn, 1)
    if (store.turns.length > 0) {
      currentTurnNumber.value = store.turns[store.turns.length - 1].turnNumber
    }
    scrollToBottom()
  }
}

function sendQuickQuestion(q: string) {
  inputMessage.value = q
  sendMessage()
}

// ============ 重新生成 ============
async function handleRegenerate() {
  if (regenerating.value || isStreaming.value) return
  regenerating.value = true
  showLoadingToast({ message: '重新生成中...', forbidClick: true })
  try {
    await store.regenerateTurn(sessionSn, currentTurnNumber.value)
    closeToast()
    showToast('已重新生成')
  } catch {
    closeToast()
  } finally {
    regenerating.value = false
  }
}

// ============ 结束问诊 ============
async function handleComplete() {
  showConfirmDialog({
    title: '确认结束问诊？',
    message: '结束后将无法继续对话',
    confirmButtonText: '结束问诊',
    cancelButtonText: '继续问诊'
  }).then(async () => {
    showLoadingToast({ message: '处理中...', forbidClick: true })
    try {
      await store.completeSessionItem(sessionSn)
      if (sessionInfo.value) {
        sessionInfo.value.status = 'COMPLETED'
      }
      closeToast()
      showToast('问诊已结束')
      setTimeout(() => {
        showRating.value = true
      }, 1000)
    } catch {
      closeToast()
      showToast('操作失败，请重试')
    }
  }).catch(() => {
    // 用户取消
  })
}

// ============ 评分完成 ============
function onRatingComplete() {
  showRating.value = false
  if (sessionInfo.value) {
    sessionInfo.value.hasRating = true
  }
}

// ============ 引用来源折叠 ============
const openCitations = ref<number[]>([])
</script>

<template>
  <div class="chat-page">
    <van-nav-bar
      :title="`AI 问诊`"
      left-arrow
      @click-left="$router.push('/consultation')"
    >
      <template #right>
        <span v-if="sessionInfo" :class="['nav-status', isCompleted ? 'nav-status--done' : 'nav-status--active']">
          {{ isCompleted ? '已结束' : '问诊中' }}
        </span>
      </template>
    </van-nav-bar>

    <!-- 对话消息区域 -->
    <div class="chat-messages" ref="messagesRef" @scroll="handleScroll">
      <!-- 加载更多提示 -->
      <div v-if="store.turnsLoading" class="load-more-hint">
        <van-loading size="16" />
        <span>加载历史对话...</span>
      </div>
      <div v-else-if="store.hasMoreTurns" class="load-more-hint clickable" @click="handleScroll">
        <span>↑ 上翻加载更多</span>
      </div>

      <!-- 对话轮次（包括初始分析结果） -->
      <div v-for="turn in displayTurns" :key="turn.id" class="turn-group">
        <!-- 轮次分隔线（真实轮次） -->
        <div v-if="turn.turnNumber > 0" class="turn-divider">
          <span>第 {{ turn.turnNumber }} 轮</span>
        </div>

        <!-- 初始分析结果：用户上传的文件 -->
        <div v-if="turn.turnNumber === 0" class="message-wrapper">
          <div class="message-bubble user">
            <div class="message-avatar">
              <van-icon name="manager-o" size="20" />
            </div>
            <div class="message-content">
              <div class="initial-upload-info">
                <van-icon name="uploader" />
                <span>上传了以下文件请AI分析</span>
              </div>
              <div v-if="initialFileUrls.length" class="initial-files">
                <div v-for="(url, idx) in initialFileUrls" :key="idx" class="initial-file-item">
                  <template v-if="isImageUrl(url)">
                    <van-image :src="url" width="100%" fit="contain" radius="4" />
                  </template>
                  <template v-else>
                    <van-icon name="description" />
                    <span>{{ getFileName(url) }}</span>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 初始分析结果：AI 分析报告 -->
        <div v-if="turn.turnNumber === 0" class="message-wrapper">
          <div class="message-bubble assistant">
            <div class="message-avatar">
              <van-icon name="service-o" size="20" />
            </div>
            <div class="message-content">
              <div class="markdown-body" v-html="renderMarkdown(turn.assistantMessage)"></div>
            </div>
          </div>
        </div>

        <!-- 普通用户消息 -->
        <div v-if="turn.turnNumber > 0" class="message-wrapper">
          <div class="message-bubble user">
            <div class="message-avatar">
              <van-icon name="manager-o" size="20" />
            </div>
            <div class="message-content">
              <div>{{ turn.userMessage }}</div>
            </div>
          </div>
        </div>

        <!-- AI 回复（仅真实轮次） -->
        <div v-if="turn.turnNumber > 0" class="message-wrapper">
          <div class="message-bubble assistant">
            <div class="message-avatar">
              <van-icon name="service-o" size="20" />
            </div>
            <div class="message-content">
              <div class="markdown-body" v-html="renderMarkdown(turn.assistantMessage)"></div>

              <!-- 引用来源 -->
              <div v-if="turn.citations && turn.citations.length" class="citations">
                <van-collapse v-model="openCitations">
                  <van-collapse-item title="📚 引用来源" :name="turn.turnNumber">
                    <div v-for="(cite, ci) in turn.citations" :key="ci" class="citation-item">
                      <strong>{{ cite.title }}</strong>
                      <van-tag type="primary">{{ cite.category }}</van-tag>
                      <p>{{ cite.snippet }}</p>
                    </div>
                  </van-collapse-item>
                </van-collapse>
              </div>

              <!-- 最后一轮 + IN_PROGRESS → 重新生成按钮 -->
              <div
                v-if="turn.turnNumber === currentTurnNumber && !isCompleted && !isStreaming"
                class="turn-actions"
              >
                <van-button
                  size="small"
                  plain
                  icon="replay"
                  :loading="regenerating"
                  @click="handleRegenerate"
                >
                  重新生成
                </van-button>
              </div>
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

      <!-- 流式引用来源 -->
      <div v-if="streamCitations.length && !isStreaming" class="stream-citations card">
        <van-collapse v-model="showStreamCitations">
          <van-collapse-item title="📚 引用来源" name="stream">
            <div v-for="(cite, ci) in streamCitations" :key="ci" class="citation-item">
              <strong>{{ cite.title }}</strong>
              <van-tag type="primary">{{ cite.category }}</van-tag>
              <p>{{ cite.snippet }}</p>
            </div>
          </van-collapse-item>
        </van-collapse>
      </div>
    </div>

    <!-- 底部区域 -->
    <div class="chat-bottom-area">
      <!-- 结束问诊按钮（IN_PROGRESS 且有对话时显示） -->
      <div v-if="!isCompleted && store.turns.length > 0 && !isStreaming" class="complete-bar">
        <van-button
          plain
          type="primary"
          size="small"
          round
          block
          @click="handleComplete"
        >
          结束本次问诊
        </van-button>
      </div>

      <!-- 评分入口（COMPLETED 且未评分） -->
      <div v-if="isCompleted && sessionInfo && !sessionInfo.hasRating" class="rating-entry">
        <van-button
          plain
          type="warning"
          size="small"
          round
          icon="star-o"
          @click="showRating = true"
        >
          为本次问诊评分
        </van-button>
      </div>

      <!-- 已结束提示 -->
      <div v-if="isCompleted" class="completed-hint">
        <van-icon name="info-o" size="14" />
        <span>本次问诊已结束</span>
      </div>

      <!-- 输入区域（仅 IN_PROGRESS 可用） -->
      <div v-if="!isCompleted" class="chat-input-area">
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
        <div v-if="store.turns.length <= 1 && !isStreaming" class="quick-questions">
          <van-tag
            v-for="q in quickQuestions"
            :key="q"
            plain
            type="primary"
            round
            class="quick-tag"
            @click="sendQuickQuestion(q)"
          >
            {{ q }}
          </van-tag>
        </div>
      </div>
    </div>

    <!-- 评分弹窗 -->
    <RatingDialog
      v-if="sessionInfo"
      :show="showRating"
      :session-sn="sessionSn"
      @close="showRating = false"
      @complete="onRatingComplete"
    />
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg);
}

/* ============ 导航栏状态 ============ */
.nav-status {
  font-size: var(--font-size-caption);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-medium);
}

.nav-status--active {
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.nav-status--done {
  background: var(--color-bg-alt);
  color: var(--color-text-tertiary);
}

/* ============ 消息区域 ============ */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  -webkit-overflow-scrolling: touch;
}

.load-more-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: var(--spacing-sm) 0;
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}

.load-more-hint.clickable {
  cursor: pointer;
}

.load-more-hint.clickable:hover {
  color: var(--color-primary);
}

/* ============ 初始分析结果 ============ */
.initial-upload-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
}

.initial-files {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.initial-file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  padding: 4px;
}

.initial-file-item :deep(.van-image) {
  border-radius: 4px;
}

/* ============ 轮次分隔线 ============ */
.turn-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-sm) 0;
}

.turn-divider span {
  font-size: var(--font-size-small);
  color: var(--color-text-tertiary);
  background: var(--color-bg-alt);
  padding: 2px 12px;
  border-radius: var(--radius-full);
}

/* ============ 消息气泡 ============ */
.message-wrapper {
  margin-bottom: var(--spacing-md);
}

.message-bubble {
  display: flex;
  gap: 10px;
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
  background: var(--color-bg);
}

.message-bubble.user .message-avatar {
  background: var(--color-primary);
  color: #fff;
}

.message-bubble.assistant .message-avatar {
  background: var(--color-success);
  color: #fff;
}

.message-content {
  max-width: 75%;
  background: var(--color-card);
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  font-size: var(--font-size-body);
  line-height: var(--line-height-relaxed);
}

.message-bubble.user .message-content {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: var(--radius-sm);
}

.message-bubble.assistant .message-content {
  border-bottom-left-radius: var(--radius-sm);
}

/* ============ Markdown ============ */
.markdown-body {
  font-size: var(--font-size-body);
  line-height: var(--line-height-relaxed);
  word-break: break-word;
}

.markdown-body :deep(p) {
  margin: 6px 0;
}

.markdown-body :deep(strong) {
  color: var(--color-primary);
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 18px;
  margin: 6px 0;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--color-primary);
  padding-left: 10px;
  margin: 8px 0;
  color: var(--color-text-secondary);
}

/* ============ 引用来源 ============ */
.citations {
  margin-top: 10px;
  border-top: 1px dashed var(--color-divider);
  padding-top: 10px;
}

.citation-item {
  margin-bottom: 8px;
  font-size: var(--font-size-caption);
}

.citation-item strong {
  display: block;
  margin-bottom: 2px;
  color: var(--color-text);
}

.citation-item p {
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.stream-citations {
  margin: 0 var(--spacing-md) var(--spacing-sm);
}

/* ============ 轮次操作 ============ */
.turn-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

/* ============ 底部区域 ============ */
.chat-bottom-area {
  background: var(--color-card);
  border-top: 1px solid var(--color-divider);
}

.complete-bar {
  padding: var(--spacing-sm) var(--spacing-md);
}

.rating-entry {
  padding: var(--spacing-sm) var(--spacing-md);
  text-align: center;
}

.completed-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: var(--spacing-sm);
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}

.chat-input-area {
  border-top: 1px solid var(--color-divider);
  padding-bottom: env(safe-area-inset-bottom);
}

.chat-input-area :deep(.van-field) {
  padding: 8px 16px;
}

.quick-questions {
  padding: 4px 16px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-tag {
  cursor: pointer;
  font-size: var(--font-size-caption);
}
</style>
