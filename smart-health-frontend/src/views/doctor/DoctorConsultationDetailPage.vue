<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { getDoctorConsultDetail, doctorReply, doctorResolve, type DoctorConsultDetail } from '@/api/consult'

const route = useRoute()
const router = useRouter()
const sessionSn = route.params.sessionSn as string

marked.setOptions({ breaks: true, gfm: true })

const detail = ref<DoctorConsultDetail | null>(null)
const replyText = ref('')
const sending = ref(false)

function genderLabel(g: number) {
  return g === 1 ? '男' : g === 2 ? '女' : '未知'
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING_DOCTOR: '⏳ 待接诊',
    DOCTOR_ACTIVE: '🩺 沟通中',
    COMPLETED: '✅ 已完成'
  }
  return map[s] || s
}

function isCompleted() {
  return detail.value?.status === 'COMPLETED'
}

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

function senderIcon(senderType: string) {
  if (senderType === 'DOCTOR') return '🩺 医生'
  if (senderType === 'AI') return '🤖 AI'
  return '🧑 患者'
}

async function loadDetail() {
  try {
    detail.value = await getDoctorConsultDetail(sessionSn)
  } catch {
    showToast('加载失败')
  }
}

async function handleReply(action: 'REPLY' | 'RESOLVE') {
  if (!replyText.value.trim() || sending.value) return
  sending.value = true
  try {
    await doctorReply(sessionSn, replyText.value.trim(), action)
    replyText.value = ''
    showToast(action === 'RESOLVE' ? '已回复并标记已解决' : '回复已发送')
    await loadDetail()
  } catch {
    showToast('回复失败')
  } finally {
    sending.value = false
  }
}

async function handleResolve() {
  showConfirmDialog({
    title: '标记已解决？',
    message: '确认将此问诊标记为已解决？',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await doctorResolve(sessionSn)
      showToast('已标记为已解决')
      await loadDetail()
    } catch {
      showToast('操作失败')
    }
  }).catch(() => {})
}

const showHistory = ref(true)

onMounted(loadDetail)
</script>

<template>
  <div class="doctor-consult-detail-page">
    <van-nav-bar title="问诊详情" left-arrow @click-left="$router.back()">
      <template #right>
        <van-button
          v-if="!isCompleted()"
          size="small"
          type="warning"
          plain
          round
          @click="handleResolve"
        >
          标记已解决
        </van-button>
      </template>
    </van-nav-bar>

    <div v-if="detail" class="detail-content">
      <!-- 患者信息 -->
      <div class="patient-card card">
        <div class="patient-header">
          <van-icon name="user-o" size="20" />
          <span class="patient-name">{{ detail.patientName }}</span>
          <van-tag>{{ genderLabel(detail.patientGender) }}</van-tag>
          <van-tag v-if="detail.patientAge != null" type="primary">{{ detail.patientAge }}岁</van-tag>
          <van-tag :type="isCompleted() ? 'success' : 'warning'">{{ statusLabel(detail.status) }}</van-tag>
        </div>
      </div>

      <!-- AI分析报告 -->
      <div class="analysis-card card">
        <h3>🤖 AI分析报告</h3>
        <div class="markdown-body" v-html="renderMarkdown(detail.symptomDraft || '(无)')"></div>
      </div>

      <!-- 附件 -->
      <div v-if="detail.fileUrls && detail.fileUrls.length" class="files-card card">
        <h3>📎 检查资料 ({{ detail.fileUrls.length }})</h3>
        <div class="file-list">
          <div v-for="(url, idx) in detail.fileUrls" :key="idx" class="file-item">
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

      <!-- 对话历史 -->
      <div class="history-card card">
        <h3 @click="showHistory = !showHistory" class="collapsible-header">
          💬 对话历史
          <van-icon :name="showHistory ? 'arrow-up' : 'arrow-down'" />
        </h3>
        <div v-if="showHistory && detail.turns && detail.turns.length">
          <div v-for="turn in detail.turns" :key="turn.id" class="turn-item">
            <div class="turn-header">
              <span class="turn-sender">{{ senderIcon(turn.senderType || 'AI') }}</span>
              <span class="turn-number">第 {{ turn.turnNumber }} 轮</span>
            </div>
            <div v-if="turn.userMessage" class="turn-user-msg">
              {{ turn.userMessage }}
            </div>
            <div v-if="turn.assistantMessage" class="turn-assistant-msg">
              <div class="markdown-body" v-html="renderMarkdown(turn.assistantMessage)"></div>
            </div>
          </div>
        </div>
        <div v-else class="empty-turns">暂无对话记录</div>
      </div>

      <!-- 回复区域 -->
      <div v-if="!isCompleted()" class="reply-area card">
        <h3>🩺 我的回复</h3>
        <van-field
          v-model="replyText"
          type="textarea"
          rows="3"
          placeholder="请输入回复内容..."
          :disabled="sending"
        />
        <div class="reply-actions">
          <van-button
            plain
            type="primary"
            size="small"
            round
            :loading="sending"
            :disabled="!replyText.trim()"
            @click="handleReply('REPLY')"
          >
            回复
          </van-button>
          <van-button
            plain
            type="warning"
            size="small"
            round
            :loading="sending"
            :disabled="!replyText.trim()"
            @click="handleReply('RESOLVE')"
          >
            回复并标记已解决
          </van-button>
        </div>
      </div>

      <!-- 已完成提示 -->
      <div v-else class="completed-hint">
        ✅ 此问诊已完成
      </div>
    </div>

    <van-loading v-else class="loading-center" size="24">加载中...</van-loading>
  </div>
</template>

<style scoped>
.doctor-consult-detail-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: env(safe-area-inset-bottom);
}

.detail-content {
  padding: var(--spacing-md);
}

.card {
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
  box-shadow: var(--shadow-sm);
}

.card h3 {
  margin: 0 0 var(--spacing-sm);
  font-size: var(--font-size-body);
  color: var(--color-text);
}

.patient-card .patient-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.patient-name {
  font-size: var(--font-size-title);
  font-weight: var(--font-weight-semibold);
}

.markdown-body {
  font-size: var(--font-size-body);
  line-height: var(--line-height-relaxed);
  word-break: break-word;
}

.markdown-body :deep(p) { margin: 4px 0; }
.markdown-body :deep(strong) { color: var(--color-primary); }

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-caption);
  padding: 8px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
}

.collapsible-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.turn-item {
  border-bottom: 1px solid var(--color-divider);
  padding: var(--spacing-sm) 0;
}

.turn-item:last-child {
  border-bottom: none;
}

.turn-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: var(--font-size-small);
  color: var(--color-text-tertiary);
}

.turn-sender {
  font-weight: var(--font-weight-medium);
}

.turn-user-msg {
  background: var(--color-primary-light);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  margin-bottom: 4px;
  font-size: var(--font-size-body);
}

.turn-assistant-msg {
  font-size: var(--font-size-body);
}

.empty-turns {
  text-align: center;
  padding: var(--spacing-md);
  color: var(--color-text-tertiary);
}

.reply-area {
  margin-bottom: var(--spacing-md);
}

.reply-area :deep(.van-field) {
  padding: 8px 0;
}

.reply-actions {
  display: flex;
  gap: 8px;
  margin-top: var(--spacing-sm);
}

.completed-hint {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-caption);
}

.loading-center {
  display: flex;
  justify-content: center;
  padding-top: 40vh;
}
</style>
