<template>
  <van-tabbar v-model="active" route class="app-tabbar" active-color="var(--color-primary)" inactive-color="var(--color-text-tertiary)">
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
    <!-- 员工角色（医生 / 药师 / 管理员）底部栏仅展示首页，通过首页工作台或快捷入口访问各功能 -->
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
  '/my': 1,
  '/admin/schedule': 1,
  '/admin/inventory': 1,
  '/admin/employees': 1
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
  background: var(--color-card) !important;
  border-top: 1px solid var(--color-card-border);
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
