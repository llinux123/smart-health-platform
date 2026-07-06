<template>
  <van-tabbar v-model="active" route class="app-tabbar" active-color="var(--color-warm)" inactive-color="var(--color-text-tertiary)">
    <van-tabbar-item to="/home">
      <span class="tab-icon-wrapper">
        <svg class="tab-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 12L5 10.5V5C5 4.44772 5.44772 4 6 4H10C10.5523 4 11 4.44772 11 5V8.5" />
          <path d="M15 4H18C18.5523 4 19 4.44772 19 5V10.5L21 12" />
          <path d="M3 12L12 19L21 12" />
          <path d="M17 21V15C17 14.4477 16.5523 14 16 14H14C13.4477 14 13 14.4477 13 15V19" />
        </svg>
      </span>
      首页
    </van-tabbar-item>
    <!-- 患者导航 -->
    <template v-if="userStore.isPatient || !userStore.role">
      <van-tabbar-item to="/consultation">
        <span class="tab-icon-wrapper">
          <svg class="tab-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15C21 15.5304 20.7893 16.0391 20.4142 16.4142C20.0391 16.7893 19.5304 17 19 17H7L3 21V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H19C19.5304 3 20.0391 3.21071 20.4142 3.58579C20.7893 3.96086 21 4.46957 21 5V15Z" />
          </svg>
        </span>
        问诊
      </van-tabbar-item>
      <van-tabbar-item to="/registration/orders">
        <span class="tab-icon-wrapper">
          <svg class="tab-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 5H7C6.46957 5 5.96086 5.21071 5.58579 5.58579C5.21071 5.96086 5 6.46957 5 7V19C5 19.5304 5.21071 20.0391 5.58579 20.4142C5.96086 20.7893 6.46957 21 7 21H17C17.5304 21 18.0391 20.7893 18.4142 20.4142C18.7893 20.0391 19 19.5304 19 19V7C19 6.46957 18.7893 5.96086 18.4142 5.58579C18.0391 5.21071 17.5304 5 17 5H15" />
            <rect x="9" y="3" width="6" height="4" rx="1" />
            <line x1="9" y1="12" x2="15" y2="12" />
            <line x1="9" y1="16" x2="13" y2="16" />
          </svg>
        </span>
        订单
      </van-tabbar-item>
      <van-tabbar-item to="/prescriptions">
        <span class="tab-icon-wrapper">
          <svg class="tab-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21V7C20 6.46957 19.7893 5.96086 19.4142 5.58579C19.0391 5.21071 18.5304 5 18 5H14" />
            <path d="M10 5H6C5.46957 5 4.96086 5.21071 4.58579 5.58579C4.21071 5.96086 4 6.46957 4 7V21" />
            <path d="M10 5C10 3.89543 10.8954 3 12 3C13.1046 3 14 3.89543 14 5C14 5.55228 14.4477 6 15 6H16C16.5523 6 17 6.44772 17 7V8" />
            <line x1="8" y1="12" x2="12" y2="12" />
            <line x1="8" y1="16" x2="14" y2="16" />
            <line x1="8" y1="20" x2="16" y2="20" />
          </svg>
        </span>
        处方
      </van-tabbar-item>
      <van-tabbar-item to="/my">
        <span class="tab-icon-wrapper">
          <svg class="tab-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </span>
        我的
      </van-tabbar-item>
    </template>
    <!-- 医生导航 -->
    <template v-else-if="userStore.isDoctor">
      <van-tabbar-item to="/admin/prescription/issue">
        <span class="tab-icon-wrapper">
          <svg class="tab-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20H9C8.46957 20 7.96086 19.7893 7.58579 19.4142C7.21071 19.0391 7 18.5304 7 18V6C7 5.46957 7.21071 4.96086 7.58579 4.58579C7.96086 4.21071 8.46957 4 9 4H12" />
            <path d="M16 2L20 6L16 10" />
            <line x1="10" y1="12" x2="20" y2="12" />
          </svg>
        </span>
        开方
      </van-tabbar-item>
    </template>
    <!-- 药师导航 -->
    <template v-else-if="userStore.isPharmacist">
      <van-tabbar-item to="/admin/prescription/review">
        <span class="tab-icon-wrapper">
          <svg class="tab-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
          </svg>
        </span>
        审核
      </van-tabbar-item>
    </template>
    <!-- 管理员导航 -->
    <template v-else-if="userStore.isAdmin">
      <van-tabbar-item to="/admin/schedule">
        <span class="tab-icon-wrapper">
          <svg class="tab-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
            <line x1="16" y1="2" x2="16" y2="6" />
            <line x1="8" y1="2" x2="8" y2="6" />
            <line x1="3" y1="10" x2="21" y2="10" />
          </svg>
        </span>
        排班
      </van-tabbar-item>
    </template>
  </van-tabbar>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const active = ref(0)

const tabMap: Record<string, number> = {
  '/home': 0,
  '/consultation': 1,
  '/registration/orders': 2,
  '/prescriptions': 3,
  '/my': 4,
  '/admin/prescription/issue': 1,
  '/admin/prescription/review': 1,
  '/admin/schedule': 1
}

watch(
  () => route.path,
  (path) => {
    active.value = tabMap[path] ?? 0
  },
  { immediate: true }
)
</script>

<style scoped>
.app-tabbar {
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  background: var(--glass-bg) !important;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2px;
  transition: transform var(--transition-fast);
}

.van-tabbar-item--active .tab-icon-wrapper {
  transform: scale(1.1);
}

.tab-icon {
  display: block;
}

:deep(.van-tabbar-item__icon) {
  margin-bottom: 0;
}

:deep(.van-tabbar-item__text) {
  font-size: 11px;
}
</style>
