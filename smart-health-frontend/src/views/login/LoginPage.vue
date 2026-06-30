<template>
  <div class="login-page">
    <div class="login-header">
      <h1 class="login-title">智慧健康</h1>
      <p class="login-subtitle">您的健康管理伙伴</p>
    </div>

    <van-form
      v-model="form"
      class="login-form"
      @submit="onSubmit"
    >
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>

      <div class="login-actions">
        <van-button
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
        >
          登录
        </van-button>
        <p class="login-register-link">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </p>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)

async function onSubmit() {
  if (!form.username || !form.password) {
    showToast('请填写用户名和密码')
    return
  }

  loading.value = true
  try {
    const data = await login(form)
    userStore.setLoginInfo(data)
    showToast('登录成功')
    const redirect = route.query.redirect || '/home'
    router.push(redirect)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1890FF 0%, #36c 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100px;
}

.login-header {
  text-align: center;
  color: #fff;
  margin-bottom: 40px;
}

.login-title {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  opacity: 0.8;
}

.login-form {
  width: 100%;
  padding: 0 24px;
}

.login-form :deep(.van-cell-group) {
  border-radius: 12px;
  overflow: hidden;
}

.login-actions {
  margin-top: 24px;
  padding: 0 16px;
}

.login-register-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}

.login-register-link a {
  color: #1890FF;
}
</style>
