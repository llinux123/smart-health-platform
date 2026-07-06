<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showLoadingToast, closeToast, showToast } from 'vant'
import { useConsultationStore, type SessionInfo, type SessionListParams } from '@/stores/consultation'
import { formatRelativeTime, getConsultationStatus } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const store = useConsultationStore()

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

function clearDate(type: 'start' | 'end') {
  if (type === 'start') startDate.value = ''
  else endDate.value = ''
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
  }).catch(() => {
    // 用户通过遮罩层或返回键关闭弹窗，不做处理
  })
}

async function confirmPermanentDelete(session: SessionInfo) {
  showConfirmDialog({
    title: '确认彻底删除？',
    message: '删除后无法恢复',
    confirmButtonText: '彻底删除',
    cancelButtonText: '取消'
  }).then(() => {
    doDelete(session, 'permanent')
  }).catch(() => {
    // 用户取消，不做处理
  })
}

async function doDelete(session: SessionInfo, mode: 'recycle' | 'permanent') {
  showLoadingToast({ message: '删除中...', forbidClick: true })
  try {
    await store.deleteSessionItem(session.sessionSn, mode)
    closeToast()
    showToast(mode === 'recycle' ? '已移入回收站' : '已彻底删除')
  } catch (e) {
    closeToast()
    showToast('删除失败，请重试')
  }
}

// ============ 辅助 ============
function getStatusText(status: string) {
  return getConsultationStatus(status).text
}

function getStatusClass(status: string) {
  if (status === 'IN_PROGRESS' || status === 'DOCTOR_ACTIVE') return 'status--active'
  if (status === 'PENDING_DOCTOR') return 'status--handoff'
  return 'status--done'
}

function hasActiveFilter() {
  return keyword.value || startDate.value || endDate.value || statusFilter.value
}
</script>

<template>
  <div class="session-list-page page-container">
    <!-- 顶部导航栏 -->
    <van-nav-bar title="问诊记录">
      <template #left>
        <van-icon name="replay" size="18" @click="goToRecycleBin" />
      </template>
      <template #right>
        <van-icon name="plus" size="20" @click="createNewSession" />
      </template>
    </van-nav-bar>

    <!-- 搜索栏 -->
    <div class="search-section">
      <van-search
        v-model="keyword"
        placeholder="搜索症状描述或AI总结"
        shape="round"
        @search="onSearch"
        @clear="onClearSearch"
      />
      <div class="filter-row">
        <div class="filter-chips">
          <van-tag
            :plain="statusFilter !== 'IN_PROGRESS'"
            type="primary"
            round
            class="filter-chip"
            @click="statusFilter = statusFilter === 'IN_PROGRESS' ? '' : 'IN_PROGRESS'; onSearch()"
          >
            问诊中
          </van-tag>
          <van-tag
            :plain="statusFilter !== 'COMPLETED'"
            color="var(--color-text-secondary)"
            text-color="#fff"
            round
            class="filter-chip"
            @click="statusFilter = statusFilter === 'COMPLETED' ? '' : 'COMPLETED'; onSearch()"
          >
            已结束
          </van-tag>
          <van-tag
            plain
            type="default"
            round
            class="filter-chip"
            @click="openDatePicker('start')"
          >
            {{ startDate || '开始日期' }}
          </van-tag>
          <van-tag
            plain
            type="default"
            round
            class="filter-chip"
            @click="openDatePicker('end')"
          >
            {{ endDate || '结束日期' }}
          </van-tag>
        </div>
        <van-button
          v-if="hasActiveFilter()"
          size="mini"
          plain
          type="default"
          round
          @click="onClearSearch"
        >
          清除
        </van-button>
      </div>
    </div>

    <!-- 列表区域 -->
    <div class="session-list-wrapper" ref="listRef">
      <van-loading v-if="store.sessionLoading && store.sessionList.length === 0" class="page-loading" />

      <EmptyState
        v-else-if="!store.sessionLoading && store.sessionList.length === 0"
        description="暂无问诊记录"
        action-text="开始新问诊"
        @action="createNewSession"
      />

      <div v-else class="session-list">
        <div
          v-for="session in store.sessionList"
          :key="session.sessionSn"
          class="session-card"
          :class="{ 'session-card--pinned': session.isPinned }"
          @click="goToChat(session)"
          @touchstart="onTouchStart(session)"
          @touchend="onTouchEnd"
          @touchcancel="onTouchEnd"
        >
          <van-swipe-cell :stop-propagation="true">
            <div class="session-card__content">
              <!-- 置顶标记 -->
              <div v-if="session.isPinned" class="pin-indicator">
                <van-icon name="flag-o" size="12" />
                <span>已置顶</span>
              </div>

              <!-- 主体内容 -->
              <div class="session-card__body">
                <div class="session-card__header">
                  <h3 class="session-card__title">
                    {{ session.symptomDraftSummary || '新问诊' }}
                  </h3>
                  <span :class="['session-card__status', getStatusClass(session.status)]">
                    {{ getStatusText(session.status) }}
                  </span>
                </div>
                <p v-if="session.aiSummary" class="session-card__summary">
                  {{ session.aiSummary }}
                </p>
                <div class="session-card__meta">
                  <span>{{ session.turnCount || 0 }} 轮对话</span>
                  <span class="meta-dot">·</span>
                  <span>{{ formatRelativeTime(session.lastChatTime || session.createTime) }}</span>
                  <span v-if="session.hasRating" class="rating-badge">
                    <van-icon name="star" size="12" /> 已评
                  </span>
                </div>
              </div>

              <!-- 右箭头 -->
              <van-icon name="arrow" class="session-card__arrow" />
            </div>

            <!-- 左滑删除 -->
            <template #right>
              <van-button
                square
                type="danger"
                class="swipe-delete-btn"
                @click.stop="handleDelete(session)"
              >
                <van-icon name="delete-o" size="18" />
              </van-button>
            </template>
          </van-swipe-cell>
        </div>

        <!-- 加载更多 -->
        <div class="load-more-area">
          <van-loading v-if="store.sessionLoading" size="20" />
          <van-empty v-else-if="finished && store.sessionList.length > 0" description="没有更多了" image="search" />
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
.session-list-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg);
}

/* ============ 搜索区域 ============ */
.search-section {
  background: var(--color-card);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-divider);
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-md);
  gap: var(--spacing-sm);
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-chip {
  cursor: pointer;
  font-size: var(--font-size-caption);
}

/* ============ 列表区域 ============ */
.session-list-wrapper {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.session-list {
  padding: var(--spacing-md);
}

/* ============ 会话卡片 ============ */
.session-card {
  margin-bottom: var(--spacing-sm);
  border-radius: var(--radius-md);
  background: var(--color-card);
  box-shadow: var(--shadow-card);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.session-card:active {
  transform: scale(0.98);
}

.session-card--pinned {
  border-left: 3px solid var(--color-primary);
}

.session-card__content {
  display: flex;
  align-items: center;
  padding: var(--spacing-md);
  gap: var(--spacing-sm);
}

.pin-indicator {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: var(--font-size-small);
  color: var(--color-primary);
  margin-bottom: 4px;
}

.session-card__body {
  flex: 1;
  min-width: 0;
}

.session-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
}

.session-card__title {
  font-size: var(--font-size-card-title);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
  line-height: var(--line-height-tight);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.session-card__status {
  flex-shrink: 0;
  font-size: var(--font-size-small);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-medium);
}

.status--active {
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.status--done {
  background: var(--color-bg-alt);
  color: var(--color-text-tertiary);
}

.status--handoff {
  background: #fff3e0;
  color: #e65100;
}

.session-card__summary {
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: var(--line-height-normal);
}

.session-card__meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}

.meta-dot {
  font-size: 8px;
}

.rating-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--color-warning);
  margin-left: 4px;
}

.session-card__arrow {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
  font-size: 16px;
}

/* ============ 左滑删除按钮 ============ */
.swipe-delete-btn {
  height: 100%;
}

/* ============ 加载更多 ============ */
.load-more-area {
  padding: var(--spacing-md) 0;
  display: flex;
  justify-content: center;
}

.load-more-area :deep(.van-empty) {
  padding: 0;
}

.load-more-area :deep(.van-empty__image) {
  display: none;
}

.load-more-area :deep(.van-empty__description) {
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}
</style>
