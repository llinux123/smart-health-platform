<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showLoadingToast, closeToast, showToast } from 'vant'
import { useConsultationStore, type SessionInfo, type SessionListParams } from '@/stores/consultation'
import { formatRelativeTime, getConsultationStatus } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const store = useConsultationStore()

// ============ 分组 ============
const groupedSessions = computed(() => {
  const active: SessionInfo[] = []
  const completed: SessionInfo[] = []
  for (const s of store.sessionList) {
    if (s.status === 'COMPLETED') {
      completed.push(s)
    } else {
      active.push(s)
    }
  }
  const sortFn = (a: SessionInfo, b: SessionInfo) => (a.isPinned === b.isPinned ? 0 : a.isPinned ? -1 : 1)
  active.sort(sortFn)
  completed.sort(sortFn)
  return { active, completed }
})

// ============ 搜索与筛选 ============
const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const statusFilter = ref('')
const showDatePopup = ref(false)
const dateRangeType = ref<'start' | 'end'>('start')
const currentDate = new Date()
const pickedDate = ref([String(currentDate.getFullYear()), String(currentDate.getMonth() + 1).padStart(2, '0'), String(currentDate.getDate()).padStart(2, '0')])

// ============ 长按菜单 ============
const showActionSheet = ref(false)
const actionSheetSession = ref<SessionInfo | null>(null)
const actionSheetActions = computed(() => {
  if (!actionSheetSession.value) return []
  return [
    { name: actionSheetSession.value.isPinned ? '取消置顶' : '置顶' },
    { name: '删除', color: '#ee0a24' }
  ]
})
let longPressTimer: ReturnType<typeof setTimeout> | null = null

// ============ 分页 ============
const listRef = ref<HTMLElement | null>(null)
const finished = computed(() => store.sessionList.length >= store.sessionTotal)

// ============ 生命周期 ============
onMounted(async () => {
  await loadSessions()
  listRef.value?.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  listRef.value?.removeEventListener('scroll', handleScroll)
})

// ============ 数据加载 ============
async function loadSessions() {
  const params = buildParams()
  await store.fetchSessions(params)
}

async function onSearch() {
  const params = buildParams()
  await store.fetchSessions(params)
}

function onClearSearch() {
  keyword.value = ''
  startDate.value = ''
  endDate.value = ''
  statusFilter.value = ''
  loadSessions()
}

async function onLoadMore() {
  const params = buildParams()
  await store.loadMoreSessions(params)
}

function buildParams(): SessionListParams {
  return {
    keyword: keyword.value || undefined,
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    status: (statusFilter.value as 'IN_PROGRESS' | 'COMPLETED') || undefined,
    page: 1,
    size: 10
  }
}

// ============ 滚动分页 ============
function handleScroll() {
  if (!listRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = listRef.value
  if (scrollHeight - scrollTop - clientHeight < 100 && !store.sessionLoading && !finished.value) {
    onLoadMore()
  }
}

// ============ 日期选择 ============
function openDatePicker(type: 'start' | 'end') {
  dateRangeType.value = type
  showDatePopup.value = true
}

function onDateConfirm({ selectedValues }: { selectedValues: string[] }) {
  const dateStr = selectedValues.join('-')
  if (dateRangeType.value === 'start') {
    startDate.value = dateStr
  } else {
    endDate.value = dateStr
  }
  showDatePopup.value = false
  onSearch()
}

// ============ 导航 ============
function goToChat(session: SessionInfo) {
  router.push(`/consultation/chat/${session.sessionSn}`)
}

function createNewSession() {
  router.push('/consultation/upload')
}

function goToRecycleBin() {
  router.push('/consultation/recycle-bin')
}

// ============ 长按交互 ============
function onTouchStart(session: SessionInfo) {
  longPressTimer = setTimeout(() => {
    actionSheetSession.value = session
    showActionSheet.value = true
  }, 600)
}

function onTouchEnd() {
  if (longPressTimer) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }
}

function onActionSelect(action: { name: string }) {
  showActionSheet.value = false
  if (!actionSheetSession.value) return
  if (action.name === '删除') {
    handleDelete(actionSheetSession.value)
  } else {
    handleTogglePin(actionSheetSession.value)
  }
}

// ============ 操作 ============
async function handleTogglePin(session: SessionInfo) {
  showLoadingToast({ message: '处理中...', forbidClick: true })
  try {
    await store.toggleSessionPin(session.sessionSn)
    closeToast()
    showToast(session.isPinned ? '已取消置顶' : '已置顶')
  } catch {
    closeToast()
  }
}

async function handleDelete(session: SessionInfo) {
  showConfirmDialog({
    title: '删除问诊记录',
    message: '移入回收站后可在30天内恢复',
    confirmButtonText: '移入回收站',
    cancelButtonText: '直接删除',
    showCancelButton: true
  }).then((action: 'confirm' | 'cancel' | undefined) => {
    if (action === 'confirm') {
      doDelete(session, 'recycle')
    } else {
      confirmPermanentDelete(session)
    }
  }).catch(() => {})
}

async function confirmPermanentDelete(session: SessionInfo) {
  showConfirmDialog({
    title: '确认彻底删除？',
    message: '删除后无法恢复',
    confirmButtonText: '彻底删除',
    cancelButtonText: '取消'
  }).then(() => {
    doDelete(session, 'permanent')
  }).catch(() => {})
}

async function doDelete(session: SessionInfo, mode: 'recycle' | 'permanent') {
  showLoadingToast({ message: '删除中...', forbidClick: true })
  try {
    await store.deleteSessionItem(session.sessionSn, mode)
    closeToast()
    showToast(mode === 'recycle' ? '已移入回收站' : '已彻底删除')
  } catch {
    closeToast()
    showToast('删除失败，请重试')
  }
}

// ============ 辅助 ============
function getStatusText(status: string) {
  return getConsultationStatus(status).text
}

function getAccentBar(status: string) {
  if (status === 'IN_PROGRESS' || status === 'DOCTOR_ACTIVE') return 'accent--teal'
  if (status === 'PENDING_DOCTOR') return 'accent--amber'
  return 'accent--muted'
}

function getPulseDot(status: string) {
  if (status === 'IN_PROGRESS') return 'pulse--teal'
  if (status === 'DOCTOR_ACTIVE') return 'pulse--teal'
  if (status === 'PENDING_DOCTOR') return 'pulse--amber'
  return ''
}

function hasActiveFilter() {
  return keyword.value || startDate.value || endDate.value || statusFilter.value
}
</script>

<template>
  <div class="session-list-page">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="问诊记录" fixed placeholder>
      <template #left>
        <van-icon name="delete-o" size="18" @click="goToRecycleBin" />
      </template>
      <template #right>
        <van-icon name="plus" size="20" @click="createNewSession" />
      </template>
    </van-nav-bar>

    <!-- 搜索与筛选区 -->
    <div class="header-area">
      <van-search
        v-model="keyword"
        placeholder="搜索症状描述或AI总结"
        shape="round"
        @search="onSearch"
        @clear="onClearSearch"
      />
      <div class="filter-bar">
        <div class="filter-scroll">
          <button
            class="chip"
            :class="{ 'chip--active': statusFilter === 'IN_PROGRESS' }"
            @click="statusFilter = statusFilter === 'IN_PROGRESS' ? '' : 'IN_PROGRESS'; onSearch()"
          >
            <span v-if="statusFilter === 'IN_PROGRESS'" class="chip__dot chip__dot--teal" />
            问诊中
          </button>
          <button
            class="chip"
            :class="{ 'chip--active': statusFilter === 'COMPLETED' }"
            @click="statusFilter = statusFilter === 'COMPLETED' ? '' : 'COMPLETED'; onSearch()"
          >
            <span v-if="statusFilter === 'COMPLETED'" class="chip__dot chip__dot--muted" />
            已结束
          </button>
          <button
            class="chip chip--default"
            @click="openDatePicker('start')"
          >
            <van-icon name="calendar-o" size="12" />
            {{ startDate || '开始日期' }}
          </button>
          <button
            class="chip chip--default"
            @click="openDatePicker('end')"
          >
            <van-icon name="calendar-o" size="12" />
            {{ endDate || '结束日期' }}
          </button>
        </div>
        <button
          v-if="hasActiveFilter()"
          class="chip-clear"
          @click="onClearSearch"
        >
          清除
        </button>
      </div>
    </div>

    <!-- 列表区域 -->
    <div class="list-area" ref="listRef">
      <van-loading v-if="store.sessionLoading && store.sessionList.length === 0" class="page-loading" />

      <EmptyState
        v-else-if="!store.sessionLoading && store.sessionList.length === 0"
        description="还没有问诊记录"
        action-text="开始首次问诊"
        @action="createNewSession"
      />

      <div v-else class="session-groups">
        <!-- 进行中 -->
        <template v-if="groupedSessions.active.length">
          <div class="section-label">进行中</div>
          <div
            v-for="session in groupedSessions.active"
            :key="session.sessionSn"
            class="session-card"
            @click="goToChat(session)"
            @touchstart="onTouchStart(session)"
            @touchend="onTouchEnd"
            @touchcancel="onTouchEnd"
          >
            <van-swipe-cell :stop-propagation="true">
              <div class="session-card__inner">
                <div :class="['accent-bar', getAccentBar(session.status)]" />

                <div class="session-card__body">
                  <div class="session-card__header">
                    <h3 class="session-card__title">
                      <span v-if="session.isPinned" class="pin-mark">📌</span>
                      {{ session.symptomDraftSummary || '新问诊' }}
                    </h3>
                    <div class="status-cell">
                      <span v-if="getPulseDot(session.status)" :class="['pulse-dot', getPulseDot(session.status)]" />
                      <span :class="['status-tag', session.status === 'COMPLETED' ? 'status-tag--done' : 'status-tag--active']">
                        {{ getStatusText(session.status) }}
                      </span>
                    </div>
                  </div>

                  <p v-if="session.aiSummary" class="session-card__summary">
                    {{ session.aiSummary }}
                  </p>

                  <div class="session-card__meta">
                    <span>{{ session.turnCount || 0 }} 轮对话</span>
                    <span class="meta-sep" />
                    <span>{{ formatRelativeTime(session.lastChatTime || session.createTime) }}</span>
                    <span v-if="session.hasRating" class="rating-mark">
                      <van-icon name="star" size="11" /> 已评
                    </span>
                  </div>
                </div>

                <van-icon name="arrow" class="session-card__chevron" />
              </div>

              <template #right>
                <van-button square type="danger" class="swipe-btn" @click.stop="handleDelete(session)">
                  <van-icon name="delete-o" size="18" />
                </van-button>
              </template>
            </van-swipe-cell>
          </div>
        </template>

        <!-- 已完成 -->
        <template v-if="groupedSessions.completed.length">
          <div class="section-label section-label--muted">历史记录</div>
          <div
            v-for="session in groupedSessions.completed"
            :key="session.sessionSn"
            class="session-card"
            @click="goToChat(session)"
            @touchstart="onTouchStart(session)"
            @touchend="onTouchEnd"
            @touchcancel="onTouchEnd"
          >
            <van-swipe-cell :stop-propagation="true">
              <div class="session-card__inner">
                <div :class="['accent-bar', getAccentBar(session.status)]" />

                <div class="session-card__body">
                  <div class="session-card__header">
                    <h3 class="session-card__title">
                      <span v-if="session.isPinned" class="pin-mark">📌</span>
                      {{ session.symptomDraftSummary || '新问诊' }}
                    </h3>
                    <div class="status-cell">
                      <span :class="['status-tag', 'status-tag--done']">
                        {{ getStatusText(session.status) }}
                      </span>
                    </div>
                  </div>

                  <p v-if="session.aiSummary" class="session-card__summary">
                    {{ session.aiSummary }}
                  </p>

                  <div class="session-card__meta">
                    <span>{{ session.turnCount || 0 }} 轮对话</span>
                    <span class="meta-sep" />
                    <span>{{ formatRelativeTime(session.lastChatTime || session.createTime) }}</span>
                    <span v-if="session.hasRating" class="rating-mark">
                      <van-icon name="star" size="11" /> 已评
                    </span>
                  </div>
                </div>

                <van-icon name="arrow" class="session-card__chevron" />
              </div>

              <template #right>
                <van-button square type="danger" class="swipe-btn" @click.stop="handleDelete(session)">
                  <van-icon name="delete-o" size="18" />
                </van-button>
              </template>
            </van-swipe-cell>
          </div>
        </template>

        <!-- 加载更多 -->
        <div class="load-more">
          <van-loading v-if="store.sessionLoading" size="20" />
          <p v-else-if="finished && store.sessionList.length > 0" class="load-more__text">没有更多了</p>
        </div>
      </div>
    </div>

    <!-- 长按操作菜单 -->
    <van-action-sheet
      v-model:show="showActionSheet"
      :actions="actionSheetActions"
      cancel-text="取消"
      @select="onActionSelect"
    />

    <!-- 日期选择弹窗 -->
    <van-popup v-model:show="showDatePopup" position="bottom" round>
      <van-date-picker
        v-model="pickedDate"
        title="选择日期"
        @confirm="onDateConfirm"
        @cancel="showDatePopup = false"
      />
    </van-popup>
  </div>
</template>

<style scoped>
/* ============================================
   问诊记录列表 — SessionListPage
   设计概念：生命体征（Vital Sign）
   患者健康对话历史的平静、精确视图
   ============================================ */

.session-list-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg);
}

/* ============ 搜索与筛选区 ============ */
.header-area {
  background: var(--color-card);
  border-bottom: 1px solid var(--color-card-border);
  flex-shrink: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 0 var(--spacing-md) var(--spacing-sm);
}

.filter-scroll {
  display: flex;
  gap: 6px;
  flex: 1;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  padding-bottom: 2px;
}

.filter-scroll::-webkit-scrollbar {
  display: none;
}

/* ============ 筛选标签（自定义 chip） ============ */
.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-caption);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  background: var(--color-bg-alt);
  border: 1px solid transparent;
  white-space: nowrap;
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.chip--active {
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

.chip--default {
  color: var(--color-text-tertiary);
  background: transparent;
  border: 1px dashed var(--color-card-border);
}

.chip__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.chip__dot--teal {
  background: var(--color-primary);
}

.chip__dot--muted {
  background: var(--color-text-tertiary);
}

.chip-clear {
  flex-shrink: 0;
  padding: 5px 10px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-caption);
  color: var(--color-signal);
  background: none;
  border: 1px solid var(--color-signal);
  cursor: pointer;
  white-space: nowrap;
}

/* ============ 列表区域 ============ */
.list-area {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.session-groups {
  padding: var(--spacing-md);
}

/* ============ 分组标签 ============ */
.section-label {
  font-size: var(--font-size-caption);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: var(--letter-spacing-wide);
  padding: var(--spacing-sm) 0;
  margin-bottom: 2px;
}

.section-label--muted {
  color: var(--color-text-tertiary);
  margin-top: var(--spacing-lg);
}

/* ============ 会话卡片 ============ */
.session-card {
  margin-bottom: var(--spacing-sm);
  border-radius: var(--radius-md);
  background: var(--color-card);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
}

.session-card:active {
  transform: scale(0.985);
}

.session-card__inner {
  display: flex;
  align-items: stretch;
  min-height: 72px;
}

/* 状态色条（左侧竖线） */
.accent-bar {
  width: 3px;
  flex-shrink: 0;
  border-radius: 2px 0 0 2px;
  margin: 10px 0;
}

.accent--teal {
  background: var(--color-primary);
}

.accent--amber {
  background: #f59e0b;
}

.accent--muted {
  background: var(--color-card-border);
}

/* 主体内容 */
.session-card__body {
  flex: 1;
  min-width: 0;
  padding: var(--spacing-sm) var(--spacing-md);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}

.session-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
}

.session-card__title {
  font-size: var(--font-size-card-title);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  line-height: var(--line-height-tight);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin: 0;
}

.pin-mark {
  margin-right: 2px;
  font-size: 13px;
}

/* 状态标签 + 脉冲点 */
.status-cell {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
}

.status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
}

.status-tag--active {
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.status-tag--done {
  background: var(--color-bg-alt);
  color: var(--color-text-tertiary);
}

/* 脉冲圆点 */
.pulse-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}

.pulse--teal {
  background: var(--color-primary);
  animation: pulse-teal 2s ease-in-out infinite;
}

.pulse--amber {
  background: #f59e0b;
  animation: pulse-amber 2s ease-in-out infinite;
}

@keyframes pulse-teal {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.35; transform: scale(0.85); }
}

@keyframes pulse-amber {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(0.8); }
}

/* AI 摘要 */
.session-card__summary {
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
  line-height: var(--line-height-normal);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
}

/* 元信息行 */
.session-card__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}

.meta-sep {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--color-card-border);
  flex-shrink: 0;
}

.rating-mark {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--color-warning);
  margin-left: 2px;
}

/* 右箭头 */
.session-card__chevron {
  flex-shrink: 0;
  align-self: center;
  margin-right: var(--spacing-sm);
  color: var(--color-text-tertiary);
  font-size: 14px;
}

/* ============ 左滑删除按钮 ============ */
.swipe-btn {
  height: 100%;
}

/* ============ 加载更多 ============ */
.load-more {
  padding: var(--spacing-lg) 0;
  display: flex;
  justify-content: center;
}

.load-more__text {
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
  margin: 0;
}

/* ============ 全页加载 ============ */
.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 120px;
}
</style>
