<template>
  <div class="register-page">
    <van-nav-bar
      title="注册账号"
      left-arrow
      @click-left="$router.back()"
    />

    <div class="register-content">
      <div class="register-hero">
        <h2 class="register-hero-title">创建您的健康档案</h2>
        <p class="register-hero-desc">完善信息以便提供更准确的健康服务</p>
      </div>

      <van-form
        ref="formRef"
        class="register-form"
        @submit="onSubmit"
      >
        <div class="form-card">
          <van-field
            v-model="form.username"
            label="用户名"
            placeholder="请输入用户名"
            :rules="[{ required: true, message: '请输入用户名' }]"
            clearable
          />
          <van-field
            v-model="form.password"
            type="password"
            label="密码"
            placeholder="6-20位，含字母和数字"
            :rules="[
              { required: true, message: '请输入密码' },
              { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/, message: '密码需包含字母和数字，6-20位' }
            ]"
            clearable
          />
          <van-field
            v-model="form.confirmPassword"
            type="password"
            label="确认密码"
            placeholder="请再次输入密码"
            :rules="[
              { required: true, message: '请确认密码' },
              { validator: (val) => val === form.password, message: '两次密码不一致' }
            ]"
            clearable
          />
          <van-field
            v-model="form.realName"
            label="真实姓名"
            placeholder="请输入真实姓名"
            :rules="[{ required: true, message: '请输入真实姓名' }]"
            clearable
          />
          <van-field
            v-model="form.idCard"
            label="身份证号"
            placeholder="请输入身份证号"
            :rules="[
              { required: true, message: '请输入身份证号' },
              { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确' }
            ]"
            clearable
          />
          <van-field
            v-model="form.phone"
            label="手机号"
            placeholder="请输入手机号"
            type="tel"
            maxlength="11"
            :rules="[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
            ]"
            clearable
          />
          <van-field name="gender" label="性别">
            <template #input>
              <van-radio-group v-model="form.gender" direction="horizontal">
                <van-radio :name="1">男</van-radio>
                <van-radio :name="2">女</van-radio>
              </van-radio-group>
            </template>
          </van-field>
        </div>

        <div class="register-actions">
          <van-button
            block
            type="primary"
            native-type="submit"
            :loading="loading"
            loading-text="注册中..."
            round
            size="large"
          >
            注册
          </van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  idCard: '',
  phone: '',
  gender: 1
})

async function onSubmit() {
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      realName: form.realName,
      idCard: form.idCard,
      phone: form.phone,
      gender: form.gender
    })
    showSuccessToast('注册成功')
    router.push('/login')
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: var(--color-bg-gradient);
}

.register-content {
  animation: fade-in 0.4s ease;
}

.register-hero {
  padding: 32px 20px 24px;
  text-align: center;
}

.register-hero-title {
  font-size: var(--font-size-title);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  margin-bottom: 8px;
}

.register-hero-desc {
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
}

.register-form {
  padding: 0 20px;
}

.form-card {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  padding: 4px 0;
}

.register-form :deep(.van-field) {
  padding: 14px 16px;
}

.register-form :deep(.van-field__label) {
  width: 70px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.register-actions {
  margin-top: 24px;
  padding-bottom: 40px;
}

.register-actions :deep(.van-button--primary) {
  height: 48px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  box-shadow: var(--shadow-elevated);
}
</style>
