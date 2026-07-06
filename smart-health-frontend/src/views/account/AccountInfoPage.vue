<template>
  <div class="account-info-page">
    <van-nav-bar title="我的" fixed placeholder />

    <div class="content">
      <div class="info-card">
        <div class="info-item info-item-avatar" @click="onAvatarClick">
          <span class="info-label">头像</span>
          <div class="info-value-wrapper">
            <van-image round width="48" height="48" :src="avatarUrl" class="info-avatar" />
            <svg class="info-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </div>
        <div class="info-divider"></div>

        <div class="info-item" @click="onUsernameClick">
          <span class="info-label">用户名</span>
          <div class="info-value-wrapper">
            <span class="info-value">{{ profile?.username || '-' }}</span>
            <svg class="info-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </div>
        <div class="info-divider"></div>

        <div class="info-item" @click="onPhoneClick">
          <span class="info-label">手机号</span>
          <div class="info-value-wrapper">
            <span class="info-value" :class="{ 'info-value--empty': !profile?.phone }">
              {{ profile?.phone || '绑定手机号' }}
            </span>
            <svg class="info-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </div>
        <div class="info-divider"></div>

        <div class="info-item" @click="onEmailClick">
          <span class="info-label">邮箱</span>
          <div class="info-value-wrapper">
            <span class="info-value" :class="{ 'info-value--empty': !profile?.email }">
              {{ profile?.email || '绑定邮箱' }}
            </span>
            <svg class="info-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </div>
        <div class="info-divider"></div>

        <div class="info-item" @click="onIdCardClick">
          <span class="info-label">实名认证</span>
          <div class="info-value-wrapper">
            <span class="info-value" :class="{ 'info-value--empty': !isIdCardBound }">
              {{ idCardDisplay }}
            </span>
            <svg class="info-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </div>
        <div class="info-divider"></div>

        <div class="info-item">
          <span class="info-label">性别</span>
          <div class="info-value-wrapper">
            <span class="info-value" :class="{ 'info-value--empty': !genderText }">
              {{ genderText || '绑定身份证后自动获取' }}
            </span>
          </div>
        </div>
        <div class="info-divider"></div>

        <div class="info-item">
          <span class="info-label">年龄</span>
          <div class="info-value-wrapper">
            <span class="info-value" :class="{ 'info-value--empty': !age }">
              {{ age ? `${age} 岁` : '绑定身份证后自动获取' }}
            </span>
          </div>
        </div>
        <div class="info-divider"></div>

        <div class="info-item">
          <span class="info-label">生日</span>
          <div class="info-value-wrapper">
            <span class="info-value" :class="{ 'info-value--empty': !profile?.birthday }">
              {{ profile?.birthday || '绑定身份证后自动获取' }}
            </span>
          </div>
        </div>
      </div>

      <div class="logout-section">
        <van-button block round plain hairline @click="handleLogout">
          退出登录
        </van-button>
      </div>
    </div>
  </div>

  <!-- 头像上传 -->
  <van-uploader v-model="avatarFileList" :after-read="onAvatarRead" ref="uploaderRef" style="display: none" />

  <!-- 修改用户名弹窗 -->
  <van-dialog v-model:show="showUsernameDialog" title="修改用户名" show-cancel-button @confirm="onUsernameConfirm">
    <van-field v-model="newUsername" placeholder="请输入用户名" maxlength="20" />
  </van-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import type { UploaderFileListItem, UploaderInstance } from 'vant'
import { useUserStore } from '@/stores/user'
import { DEFAULT_AVATAR, getProfile, updateUsername, updateAvatar, logout as logoutApi } from '@/api/auth'
import type { ProfileData } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const profile = computed<Partial<ProfileData>>(() => userStore.profile || {})
const avatarUrl = computed(() => profile.value.avatar || DEFAULT_AVATAR)

const isIdCardBound = computed(() => {
  const status = profile.value.idCardStatus
  return status === 1 || status === 2
})

const idCardDisplay = computed(() => {
  const status = profile.value.idCardStatus
  if (status === 1) return '审核中'
  if (status === 2) return `${profile.value.realName || ''} ${profile.value.idCard || ''}`
  if (status === 3) return '认证失败，请联系客服'
  return '绑定身份证'
})

const genderText = computed(() => {
  const gender = profile.value.gender
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return ''
})

const age = computed(() => {
  const birthday = profile.value.birthday
  if (!birthday) return null
  const birth = new Date(birthday)
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  if (now.getMonth() < birth.getMonth() || (now.getMonth() === birth.getMonth() && now.getDate() < birth.getDate())) {
    age--
  }
  return age
})

onMounted(async () => {
  try {
    const data = await getProfile()
    userStore.setProfile(data)
  } catch (e) {
    console.warn('[AccountInfoPage] 获取 profile 失败', e)
  }
})

function onAvatarClick() {
  // 触发隐藏的上传组件
  uploaderRef.value?.chooseFile()
}

const avatarFileList = ref([])
const uploaderRef = ref<UploaderInstance | null>(null)

async function onAvatarRead(items: UploaderFileListItem | UploaderFileListItem[]) {
  const file = Array.isArray(items) ? items[0] : items
  if (!file?.file) return
  try {
    // TODO: 需要先通过文件上传接口获取永久 URL，再调用 updateAvatar
    const url = URL.createObjectURL(file.file)
    const data = await updateAvatar(url)
    userStore.setProfile(data)
    showToast('头像更新成功')
  } catch (e) {
    console.warn('[AccountInfoPage] 头像更新失败', e)
  }
}

const showUsernameDialog = ref(false)
const newUsername = ref('')

function onUsernameClick() {
  newUsername.value = profile.value.username || ''
  showUsernameDialog.value = true
}

async function onUsernameConfirm() {
  const name = newUsername.value.trim()
  if (!name) {
    showToast('用户名不能为空')
    return
  }
  try {
    const data = await updateUsername(name)
    userStore.setProfile(data)
    showToast('用户名更新成功')
  } catch (e) {
    console.warn('[AccountInfoPage] 更新用户名失败', e)
  }
}

function onPhoneClick() {
  const hasPhone = !!profile.value.phone
  showToast(hasPhone ? '更换手机号功能即将上线' : '绑定手机号功能即将上线')
}

function onEmailClick() {
  const hasEmail = !!profile.value.email
  showToast(hasEmail ? '更换邮箱功能即将上线' : '绑定邮箱功能即将上线')
}

function onIdCardClick() {
  if (isIdCardBound.value) {
    showToast('身份证已绑定，如需修改请联系客服')
    return
  }
  router.push('/account/bind-identity')
}

async function handleLogout() {
  try {
    await showConfirmDialog({
      title: '退出登录',
      message: '确定要退出登录吗？',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await logoutApi()
  } catch (e) {
    console.warn('[AccountInfoPage] 登出接口失败', e)
  }

  userStore.logout()
  showToast('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.account-info-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 80px;
}

.content {
  padding: 16px;
}

.info-card {
  background: var(--color-card);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.info-item:active {
  background: var(--color-bg);
}

.info-item-avatar {
  padding-top: 12px;
  padding-bottom: 12px;
}

.info-label {
  font-size: 15px;
  color: var(--color-text);
  font-weight: var(--font-weight-medium);
}

.info-value-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.info-value {
  font-size: 14px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.info-value--empty {
  color: var(--color-text-tertiary);
}

.info-avatar {
  flex-shrink: 0;
}

.info-arrow {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.info-divider {
  height: 1px;
  margin: 0 16px;
  background: var(--color-divider);
}

.logout-section {
  padding: 0 16px;
}

:deep(.van-button--plain) {
  color: var(--color-text);
  border-color: var(--color-card-border);
}
</style>
