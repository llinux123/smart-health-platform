<template>
  <div class="register-page">
    <van-nav-bar
      title="注册账号"
      left-arrow
      @click-left="$router.back()"
    />

    <van-form
      ref="formRef"
      class="register-form"
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
          placeholder="6-20位，含字母和数字"
          :rules="[
            { required: true, message: '请输入密码' },
            { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/, message: '密码需包含字母和数字，6-20位' }
          ]"
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
        />
        <van-field
          v-model="form.realName"
          label="真实姓名"
          placeholder="请输入真实姓名"
          :rules="[{ required: true, message: '请输入真实姓名' }]"
        />
        <van-field
          v-model="form.idCard"
          label="身份证号"
          placeholder="请输入身份证号"
          :rules="[
            { required: true, message: '请输入身份证号' },
            { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确' }
          ]"
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
        />
      </van-cell-group>

      <div class="register-actions">
        <van-button
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="注册中..."
        >
          注册
        </van-button>
      </div>
    </van-form>
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
  background-color: var(--color-bg);
}

.register-form {
  padding-top: 16px;
}

.register-form :deep(.van-cell-group) {
  border-radius: 12px;
  overflow: hidden;
}

.register-actions {
  margin-top: 24px;
  padding: 0 24px;
}
</style>
