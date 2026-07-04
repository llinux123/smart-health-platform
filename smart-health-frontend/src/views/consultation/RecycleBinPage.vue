<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { showConfirmDialog, showLoadingToast, closeToast, showToast } from 'vant'
import { useConsultationStore, type SessionInfo } from '@/stores/consultation'
import { formatRelativeTime } from '@/utils/format'

const store = useConsultationStore()

const listRef = ref<HTMLElement | null>(null)
const finished = computed(() => store.recycleBinList.length >= store.recycleBinTotal)

onMounted(async () => {
  await loadList()
  listRef.value?.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  listRef.value?.removeEventListener('scroll', handleScroll)
})

async function loadList() {
  await store.fetchRecycleBin(1)
}

async function onLoadMore() {
  const nextPage = store.recycleBinPage + 1
  await store.fetchRecycleBin(nextPage, true)
}

function handleScroll() {
  if (!listRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = listRef.value
  if (scrollHeight - scrollTop - clientHeight < 100 && !store.recycleBinLoading && !finished.value) {
    onLoadMore()
  }
}

async function handleRestore(session: SessionInfo) {
  showLoadingToast({ message: '恢复中...', forbidClick: true })
  try {
    await store.restoreSessionItem(session.sessionSn)
    closeToast()
    showToast('已恢复')
  } catch {
    closeToast()
  }
}

async function handlePermanentDelete(session: SessionInfo) {
  showConfirmDialog({
    title: '确认彻底删除？',
    message: '删除后无法恢复',
    confirmButtonText: '彻底删除',
    cancelButtonText: '取消'
  }).then(async () => {
    showLoadingToast({ message: '删除中...', forbidClick: true })
    try {
      await store.permanentDeleteSessionItem(session.sessionSn)
      closeToast()
      showToast('已彻底删除')
    } catch {
      closeToast()
      showToast('删除失败，请重试')
    }
  }).catch(() => {
    // 用户取消
  })
}
</script>

<template>
  <div class="recycle-bin-page page-container">
    <van-nav-bar title="回收站" left-arrow @click-left="$router.back()">
      <template #right>
        <span class="hint-text">30天自动清除</span>
      </template>
    </van-nav-bar>

    <div class="recycle-list-wrapper" ref="listRef">
      <van-loading v-if="store.recycleBinLoading && store.recycleBinList.length === 0" class="page-loading" />

      <div v-else-if="!store.recycleBinLoading && store.recycleBinList.length === 0" class="empty-recycle">
        <van-icon name="delete-o" size="64" color="var(--color-text-quaternary, #ccc)" />
        <p class="empty-recycle__title">回收站空空如也</p>
        <p class="empty-recycle__hint">删除的问诊记录会在这里保留 30 天</p>
      </div>

      <div v-else class="recycle-list">
        <div
          v-for="session in store.recycleBinList"
          :key="session.sessionSn"
          class="recycle-card"
        >
          <div class="recycle-card__body">
            <h3 class="recycle-card__title">
              {{ session.symptomDraftSummary || '问诊记录' }}
            </h3>
            <div class="recycle-card__meta">
              <span>{{ session.turnCount || 0 }} 轮对话</span>
              <span class="meta-dot">·</span>
              <span>删除于 {{ formatRelativeTime(session.createTime) }}</span>
            </div>
          </div>
          <div class="recycle-card__actions">
            <van-button
              size="small"
              plain
              type="primary"
              round
              @click="handleRestore(session)"
            >
              恢复
            </van-button>
            <van-button
              size="small"
              plain
              type="danger"
              round
              @click="handlePermanentDelete(session)"
            >
              彻底删除
            </van-button>
          </div>
        </div>

        <!-- 加载更多 -->
        <div class="load-more-area">
          <van-loading v-if="store.recycleBinLoading" size="20" />
          <van-empty v-else-if="finished && store.recycleBinList.length > 0" description="没有更多了" image="search" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recycle-bin-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg);
}

.hint-text {
  font-size: var(--font-size-caption);
  color: var(--color-text-tertiary);
}

.empty-recycle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 32px;
  text-align: center;
}

.empty-recycle__title {
  margin-top: 16px;
  font-size: var(--font-size-body, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-secondary, #666);
}

.empty-recycle__hint {
  margin-top: 6px;
  font-size: var(--font-size-caption, 12px);
  color: var(--color-text-tertiary, #999);
}

.recycle-list-wrapper {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.recycle-list {
  padding: var(--spacing-md);
}

.recycle-card {
  background: var(--color-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.recycle-card__body {
  flex: 1;
  min-width: 0;
}

.recycle-card__title {
  font-size: var(--font-size-card-title);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
  line-height: var(--line-height-tight);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recycle-card__meta {
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

.recycle-card__actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}

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
