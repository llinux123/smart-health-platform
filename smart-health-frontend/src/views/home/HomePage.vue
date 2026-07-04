<template>
  <div class="home-page">
    <!-- 生长曲线签名区 -->
    <div class="hero">
      <div class="hero-decoration">
        <svg class="growth-line" viewBox="0 0 360 28" fill="none" preserveAspectRatio="none">
          <path d="M0,20 C36,20 54,4 90,4 C126,4 144,22 180,22 C216,22 234,6 270,6 C306,6 324,18 360,18"
                stroke="var(--color-primary)" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
      </div>
      <div class="hero-body">
        <div class="hero-top">
          <div class="hero-avatar">
            <van-image round width="48" height="48" :src="'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
          </div>
          <div class="hero-text">
            <h1 class="hero-greeting">你好，{{ userStore.realName || '用户' }}</h1>
            <p class="hero-prompt">今天感觉如何？让我来帮您</p>
          </div>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-value">{{ stats.consultCount }}</span>
            <span class="hero-stat-label">问诊</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ stats.appointmentCount }}</span>
            <span class="hero-stat-label">挂号</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ stats.prescriptionCount }}</span>
            <span class="hero-stat-label">处方</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 功能区 -->
    <div class="content">
      <div class="feature-grid">
        <div
          v-for="(item, index) in menuItems"
          :key="item.path"
          class="feature-card"
          :style="{ animationDelay: `${0.3 + index * 0.08}s` }"
          @click="$router.push(item.path)"
        >
          <div class="feature-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path v-if="item.icon === 'ai'" d="M12 2L2 7L12 12L22 7L12 2Z" />
              <path v-if="item.icon === 'ai'" d="M2 17L12 22L22 17" />
              <path v-if="item.icon === 'ai'" d="M2 12L12 17L22 12" />
              <circle v-if="item.icon === 'calendar'" cx="12" cy="12" r="9" />
              <polyline v-if="item.icon === 'calendar'" points="12 7 12 12 16 14" />
              <path v-if="item.icon === 'prescription'" d="M4 19.5C4 18.837 4.26339 18.2011 4.73223 17.7322C5.20107 17.2634 5.83696 17 6.5 17H20" />
              <path v-if="item.icon === 'prescription'" d="M6.5 2H20V20C20 20.5304 19.7893 21.0391 19.4142 21.4142C19.0391 21.7893 18.5304 22 18 22H6.5C5.83696 22 5.20107 21.7366 4.73223 21.2678C4.26339 20.7989 4 20.163 4 19.5V4.5C4 3.83696 4.26339 3.20107 4.73223 2.73223C5.20107 2.26339 5.83696 2 6.5 2Z" />
              <line v-if="item.icon === 'prescription'" x1="8" y1="7" x2="16" y2="7" />
              <line v-if="item.icon === 'prescription'" x1="8" y1="11" x2="14" y2="11" />
              <line v-if="item.icon === 'prescription'" x1="8" y1="15" x2="12" y2="15" />
              <path v-if="item.icon === 'orders'" d="M9 5H7C6.46957 5 5.96086 5.21071 5.58579 5.58579C5.21071 5.96086 5 6.46957 5 7V19C5 19.5304 5.21071 20.0391 5.58579 20.4142C5.96086 20.7893 6.46957 21 7 21H17C17.5304 21 18.0391 20.7893 18.4142 20.4142C18.7893 20.0391 19 19.5304 19 19V7C19 6.46957 18.7893 5.96086 18.4142 5.58579C18.0391 5.21071 17.5304 5 17 5H15" />
              <rect v-if="item.icon === 'orders'" x="9" y="3" width="6" height="4" rx="1" />
              <line v-if="item.icon === 'orders'" x1="9" y1="12" x2="15" y2="12" />
              <line v-if="item.icon === 'orders'" x1="9" y1="16" x2="13" y2="16" />
            </svg>
          </div>
          <div class="feature-info">
            <span class="feature-title">{{ item.text }}</span>
            <span class="feature-desc">{{ item.desc }}</span>
          </div>
        </div>
      </div>

      <!-- 患者快捷入口 -->
      <section v-if="userStore.isPatient" class="list-section">
        <h2 class="list-section-label">快捷入口</h2>
        <div class="list-card">
          <div class="list-item" @click="$router.push('/registration/orders')">
            <div class="list-item-icon list-icon-orders">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 5H7C6.46957 5 5.96086 5.21071 5.58579 5.58579C5.21071 5.96086 5 6.46957 5 7V19C5 19.5304 5.21071 20.0391 5.58579 20.4142C5.96086 20.7893 6.46957 21 7 21H17C17.5304 21 18.0391 20.7893 18.4142 20.4142C18.7893 20.0391 19 19.5304 19 19V7C19 6.46957 18.7893 5.96086 18.4142 5.58579C18.0391 5.21071 17.5304 5 17 5H15" />
                <rect x="9" y="3" width="6" height="4" rx="1" />
              </svg>
            </div>
            <div class="list-item-body">
              <span class="list-item-title">挂号订单</span>
              <span class="list-item-desc">查看预约与支付状态</span>
            </div>
            <svg class="list-item-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
          <div class="list-divider"></div>
          <div class="list-item" @click="$router.push('/prescriptions')">
            <div class="list-item-icon list-icon-prescription">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 19.5C4 18.837 4.26339 18.2011 4.73223 17.7322C5.20107 17.2634 5.83696 17 6.5 17H20" />
                <path d="M6.5 2H20V20C20 20.5304 19.7893 21.0391 19.4142 21.4142C19.0391 21.7893 18.5304 22 18 22H6.5C5.83696 22 5.20107 21.7366 4.73223 21.2678C4.26339 20.7989 4 20.163 4 19.5V4.5C4 3.83696 4.26339 3.20107 4.73223 2.73223C5.20107 2.26339 5.83696 2 6.5 2Z" />
                <line x1="8" y1="7" x2="16" y2="7" />
                <line x1="8" y1="11" x2="14" y2="11" />
                <line x1="8" y1="15" x2="12" y2="15" />
              </svg>
            </div>
            <div class="list-item-body">
              <span class="list-item-title">我的处方</span>
              <span class="list-item-desc">查看用药记录与详情</span>
            </div>
            <svg class="list-item-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
          <div class="list-divider"></div>
          <div class="list-item" @click="$router.push('/consultation')">
            <div class="list-item-icon list-icon-chat">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15C21 15.5304 20.7893 16.0391 20.4142 16.4142C20.0391 16.7893 19.5304 17 19 17H7L3 21V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H19C19.5304 3 20.0391 3.21071 20.4142 3.58579C20.7893 3.96086 21 4.46957 21 5V15Z" />
              </svg>
            </div>
            <div class="list-item-body">
              <span class="list-item-title">问诊记录</span>
              <span class="list-item-desc">查看历史 AI 问诊</span>
            </div>
            <svg class="list-item-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </div>
      </section>

      <!-- 工作台（按角色动态显示） -->
      <section v-if="adminMenuItems.length > 0" class="list-section">
        <h2 class="list-section-label">工作台</h2>
        <div class="list-card">
          <template v-for="(item, idx) in adminMenuItems" :key="item.path">
            <div v-if="idx > 0" class="list-divider"></div>
            <div class="list-item" @click="$router.push(item.path)">
              <div class="list-item-icon" :class="item.iconClass">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <template v-if="item.icon === 'schedule'">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </template>
                  <template v-else-if="item.icon === 'review'">
                    <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" y1="13" x2="8" y2="13" />
                    <line x1="16" y1="17" x2="8" y2="17" />
                  </template>
                  <template v-else-if="item.icon === 'issue'">
                    <path d="M12 20H9C8.46957 20 7.96086 19.7893 7.58579 19.4142C7.21071 19.0391 7 18.5304 7 18V6C7 5.46957 7.21071 4.96086 7.58579 4.58579C7.96086 4.21071 8.46957 4 9 4H12" />
                    <path d="M16 2L20 6L16 10" />
                    <line x1="10" y1="12" x2="20" y2="12" />
                  </template>
                </svg>
              </div>
              <div class="list-item-body">
                <span class="list-item-title">{{ item.title }}</span>
                <span class="list-item-desc">{{ item.desc }}</span>
              </div>
              <svg class="list-item-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
          </template>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getPatientStats } from '@/api/dashboard'
import type { PatientStats } from '@/api/dashboard'

const userStore = useUserStore()

const stats = ref<PatientStats>({
  consultCount: 0,
  appointmentCount: 0,
  prescriptionCount: 0
})

onMounted(async () => {
  if (userStore.isPatient) {
    try {
      stats.value = await getPatientStats()
    } catch {
      // 获取失败时保持默认值 0
    }
  }
})

/** 患者功能菜单 */
const patientMenuItems = [
  { text: 'AI 智能问诊', desc: '上传照片，AI 初步分析', icon: 'ai', path: '/consultation/upload' },
  { text: '挂号预约', desc: '选择医生，快速挂号', icon: 'calendar', path: '/registration/schedules' },
  { text: '我的处方', desc: '查看处方和用药记录', icon: 'prescription', path: '/prescriptions' },
  { text: '挂号记录', desc: '查看我的挂号订单', icon: 'orders', path: '/registration/orders' }
]

/** 医生功能菜单 */
const doctorMenuItems = [
  { text: '开具处方', desc: '为患者开具电子处方', icon: 'issue', path: '/admin/prescription/issue' },
  { text: '挂号排班', desc: '查看出诊排班信息', icon: 'calendar', path: '/registration/schedules' }
]

/** 药师功能菜单 */
const pharmacistMenuItems = [
  { text: '处方审核', desc: '审核待处理的处方', icon: 'review', path: '/admin/prescription/review' }
]

/** 管理员功能菜单 */
const adminRoleMenuItems = [
  { text: '排班管理', desc: '管理医生出诊排班', icon: 'schedule', path: '/admin/schedule' },
  { text: '处方审核', desc: '审核待处理的处方', icon: 'review', path: '/admin/prescription/review' },
  { text: '开具处方', desc: '为患者开具电子处方', icon: 'issue', path: '/admin/prescription/issue' }
]

/** 根据角色动态计算功能菜单 */
const menuItems = computed(() => {
  switch (userStore.role) {
    case 'DOCTOR': return doctorMenuItems
    case 'PHARMACIST': return pharmacistMenuItems
    case 'ADMIN': return adminRoleMenuItems
    default: return patientMenuItems
  }
})

/** 管理后台菜单项（按角色） */
const adminMenuItems = computed(() => {
  const items = []
  const role = userStore.role
  if (role === 'ADMIN') {
    items.push(
      { title: '排班管理', desc: '管理医生出诊排班', icon: 'schedule', iconClass: 'list-icon-admin', path: '/admin/schedule' },
      { title: '处方审核', desc: '审核待处理的处方', icon: 'review', iconClass: 'list-icon-review', path: '/admin/prescription/review' },
      { title: '开具处方', desc: '为患者开具电子处方', icon: 'issue', iconClass: 'list-icon-issue', path: '/admin/prescription/issue' }
    )
  } else if (role === 'DOCTOR') {
    items.push(
      { title: '开具处方', desc: '为患者开具电子处方', icon: 'issue', iconClass: 'list-icon-issue', path: '/admin/prescription/issue' }
    )
  } else if (role === 'PHARMACIST') {
    items.push(
      { title: '处方审核', desc: '审核待处理的处方', icon: 'review', iconClass: 'list-icon-review', path: '/admin/prescription/review' }
    )
  }
  return items
})
</script>

<style scoped>
/* ============ Hero ============ */
.hero {
  position: relative;
  background: var(--color-primary-light);
  padding: 0 0 24px;
  overflow: hidden;
}

.hero-decoration {
  height: 28px;
  display: flex;
  align-items: flex-end;
  padding: 0 20px;
}

.growth-line {
  width: 100%;
  height: 28px;
  color: var(--color-primary);
  stroke-dasharray: 520;
  stroke-dashoffset: 520;
  animation: draw-growth 2.2s ease-out 0.1s forwards;
  opacity: 0.55;
}

.hero-body {
  padding: 0 20px;
}

.hero-top {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.hero-avatar {
  flex-shrink: 0;
}

.hero-text {
  flex: 1;
}

.hero-greeting {
  font-size: 20px;
  font-weight: var(--font-weight-bold);
  color: var(--color-primary-dark);
  line-height: 1.25;
  margin-bottom: 3px;
}

.hero-prompt {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.hero-stats {
  display: flex;
  align-items: center;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 14px 8px;
  box-shadow: var(--shadow-sm);
}

.hero-stat {
  flex: 1;
  text-align: center;
}

.hero-stat-value {
  display: block;
  font-size: 20px;
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
  line-height: 1.2;
}

.hero-stat-label {
  display: block;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.hero-stat-divider {
  width: 1px;
  height: 28px;
  background: var(--color-divider);
}

/* ============ Content ============ */
.content {
  padding: 20px 16px 60px;
}

/* ============ Feature Grid ============ */
.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 24px;
}

.feature-card {
  background: var(--color-card);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-lg);
  padding: 20px 16px;
  cursor: pointer;
  animation: fade-in-up 0.5s ease backwards;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.feature-card:hover {
  box-shadow: var(--shadow-md);
}

.feature-card:active {
  transform: scale(0.97);
}

.feature-icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.feature-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.feature-title {
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  line-height: 1.2;
}

.feature-desc {
  font-size: 11px;
  color: var(--color-text-secondary);
  line-height: 1.3;
}

/* ============ List Section ============ */
.list-section {
  margin-bottom: 20px;
}

.list-section-label {
  font-size: 15px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  margin-bottom: 10px;
  padding: 0 4px;
}

.list-card {
  background: var(--color-card);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.list-item:active {
  background: var(--color-bg);
}

.list-item-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.list-icon-orders {
  background: var(--color-warm-light);
  color: var(--color-warm);
}

.list-icon-prescription {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.list-icon-chat {
  background: #EEF2FF;
  color: #4F6FBF;
}

.list-icon-admin {
  background: #F3E8FF;
  color: #7C3AED;
}

.list-icon-review {
  background: #FEF3C7;
  color: #B8860B;
}

.list-icon-issue {
  background: #FEE2E2;
  color: #DC2626;
}

.list-item-body {
  flex: 1;
  min-width: 0;
}

.list-item-title {
  display: block;
  font-size: 14px;
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
  margin-bottom: 2px;
}

.list-item-desc {
  display: block;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.list-item-arrow {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.list-divider {
  height: 1px;
  margin: 0 16px;
  background: var(--color-divider);
}

.home-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 60px;
}
</style>
