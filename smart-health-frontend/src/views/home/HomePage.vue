<template>
  <div class="home-page">
    <div class="home-header">
      <div class="home-user">
        <van-image
          round
          width="48"
          height="48"
          :src="'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
        />
        <div class="home-user-info">
          <p class="home-greeting">你好，{{ userStore.realName || '用户' }}</p>
          <p class="home-subtitle">关注健康，享受生活</p>
        </div>
      </div>
    </div>

    <div class="home-grid">
      <van-grid :column-num="2" :border="false" :gutter="12">
        <van-grid-item
          v-for="item in menuItems"
          :key="item.path"
          :icon="item.icon"
          :text="item.text"
          @click="$router.push(item.path)"
        >
          <template #text>
            <span class="grid-text">{{ item.text }}</span>
            <span class="grid-desc">{{ item.desc }}</span>
          </template>
        </van-grid-item>
      </van-grid>
    </div>

    <div class="home-section">
      <h3 class="section-title">快捷功能</h3>
      <van-cell-group inset>
        <van-cell
          title="我的挂号订单"
          icon="orders-o"
          is-link
          @click="$router.push('/registration/orders')"
        />
        <van-cell
          title="我的处方"
          icon="bill-o"
          is-link
          @click="$router.push('/prescriptions')"
        />
        <van-cell
          title="问诊记录"
          icon="chat-o"
          is-link
          @click="$router.push('/consultation')"
        />
      </van-cell-group>
    </div>

    <div class="home-section">
      <h3 class="section-title">管理后台</h3>
      <van-cell-group inset>
        <van-cell
          title="排班管理"
          icon="calendar-o"
          is-link
          @click="$router.push('/admin/schedule')"
        />
        <van-cell
          title="处方审核"
          icon="passed"
          is-link
          @click="$router.push('/admin/prescription/review')"
        />
        <van-cell
          title="开具处方"
          icon="edit"
          is-link
          @click="$router.push('/admin/prescription/issue')"
        />
      </van-cell-group>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const menuItems = [
  { text: 'AI 智能问诊', desc: '上传照片，AI 分析', icon: 'search', path: '/consultation/upload' },
  { text: '挂号预约', desc: '选择医生，快速挂号', icon: 'todo-list-o', path: '/registration/schedules' },
  { text: '我的处方', desc: '查看处方和用药', icon: 'bill-o', path: '/prescriptions' },
  { text: '挂号记录', desc: '查看订单状态', icon: 'orders-o', path: '/registration/orders' }
]
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: var(--color-bg);
  padding-bottom: 60px;
}

.home-header {
  background: linear-gradient(135deg, #1890FF 0%, #36c 100%);
  padding: 24px 16px 32px;
  color: #fff;
}

.home-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.home-user-info {
  flex: 1;
}

.home-greeting {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 4px;
}

.home-subtitle {
  font-size: 12px;
  opacity: 0.8;
}

.home-grid {
  margin: -16px 16px 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.grid-text {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-top: 8px;
}

.grid-desc {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.home-section {
  margin: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
  color: var(--color-text);
}
</style>
