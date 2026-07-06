<template>
  <div class="employee-page page-container">
    <van-nav-bar title="员工管理" left-arrow @click-left="$router.back()">
      <template #right>
        <van-icon name="plus" size="18" @click="showCreateDialog" />
      </template>
    </van-nav-bar>

    <van-loading v-if="loading" class="page-loading" />
    <template v-else>
      <EmptyState v-if="staffList.length === 0" description="暂无员工数据" />
      <div v-else class="list-section">
        <div v-for="staff in staffList" :key="staff.id" class="staff-card card">
          <div class="staff-header">
            <div class="staff-info">
              <span class="staff-name">{{ staff.realName }}</span>
              <van-tag :type="roleTagType(staff.role) as any" :size="'small' as any">{{ roleLabel(staff.role) }}</van-tag>
            </div>
            <div class="staff-actions">
              <van-icon name="edit" size="16" @click="showEditDialog(staff)" />
              <van-icon name="delete" size="16" @click="handleDelete(staff)" />
            </div>
          </div>
          <div class="staff-meta">
            <span>用户名: {{ staff.username }}</span>
            <span v-if="staff.phone">电话: {{ staff.phone }}</span>
            <span v-if="staff.doctorId">医生ID: {{ staff.doctorId }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- 创建/编辑弹窗 -->
    <van-popup v-model:show="showDialog" position="bottom" round style="max-height: 85vh">
      <div class="dialog-header">
        <span class="dialog-title">{{ isEditing ? '编辑员工' : '新建员工' }}</span>
        <van-icon name="cross" @click="showDialog = false" />
      </div>
      <van-form ref="formRef" @submit="onSubmit" class="dialog-form">
        <van-field v-model="form.username" label="用户名" placeholder="请输入用户名" :rules="[{ required: true, message: '请输入用户名' }]" :disabled="isEditing" />
        <van-field v-model="form.realName" label="真实姓名" placeholder="请输入真实姓名" :rules="[{ required: true, message: '请输入真实姓名' }]" />
        <van-field v-model="form.password" label="密码" :type="isEditing ? 'text' : 'password'" :placeholder="isEditing ? '留空则不修改' : '请输入密码'" :rules="isEditing ? [] : [{ required: true, message: '请输入密码' }]" />
        <van-field v-model="form.phone" label="手机号" placeholder="请输入手机号" />
        <van-field v-model="form.roleLabel" is-link readonly label="角色" placeholder="请选择角色" @click="showRolePicker = true" :rules="[{ required: true, message: '请选择角色' }]" />
        <van-field v-if="form.role === 'DOCTOR'" :model-value="form.doctorId ?? ''" @update:model-value="form.doctorId = Number($event) || undefined" label="医生ID" type="number" placeholder="关联的医生ID" />
        <div class="dialog-action">
          <van-button round block type="primary" native-type="submit" :loading="submitting">
            {{ isEditing ? '保存修改' : '创建员工' }}
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 角色选择器 -->
    <van-popup v-model:show="showRolePicker" position="bottom" round>
      <van-picker
        :columns="roleOptions"
        @confirm="onRoleConfirm"
        @cancel="showRolePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { showConfirmDialog, showSuccessToast, showFailToast } from 'vant'
import { listStaff, createStaff, updateStaff, deleteStaff, type StaffVO, type StaffRequest } from '@/api/staff'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(true)
const staffList = ref<StaffVO[]>([])
const submitting = ref(false)
const formRef = ref<any>(null)
const showDialog = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const showRolePicker = ref(false)

const form = reactive<StaffRequest & { roleLabel: string }>({
  username: '',
  realName: '',
  password: '',
  phone: '',
  role: 'DOCTOR',
  roleLabel: '医生',
  doctorId: null
})

const roleOptions = [
  { text: '医生', value: 'DOCTOR' },
  { text: '药师', value: 'PHARMACIST' },
  { text: '管理员', value: 'ADMIN' }
]

function roleLabel(role: string): string {
  const map: Record<string, string> = { DOCTOR: '医生', PHARMACIST: '药师', ADMIN: '管理员' }
  return map[role] || role
}

function roleTagType(role: string): string {
  const map: Record<string, string> = { DOCTOR: 'primary', PHARMACIST: 'warning', ADMIN: 'danger' }
  return map[role] || 'default'
}

onMounted(async () => {
  await loadStaff()
})

async function loadStaff() {
  loading.value = true
  try {
    staffList.value = await listStaff()
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.username = ''
  form.realName = ''
  form.password = ''
  form.phone = ''
  form.role = 'DOCTOR'
  form.roleLabel = '医生'
  form.doctorId = null
}

function showCreateDialog() {
  isEditing.value = false
  editingId.value = null
  resetForm()
  showDialog.value = true
}

function showEditDialog(staff: StaffVO) {
  isEditing.value = true
  editingId.value = staff.id
  form.username = staff.username
  form.realName = staff.realName
  form.password = ''
  form.phone = staff.phone || ''
  form.role = staff.role as StaffRequest['role']
  form.roleLabel = roleLabel(staff.role)
  form.doctorId = staff.doctorId
  showDialog.value = true
}

function onRoleConfirm({ selectedOptions }: any) {
  form.role = selectedOptions[0].value
  form.roleLabel = selectedOptions[0].text
  showRolePicker.value = false
}

async function onSubmit() {
  submitting.value = true
  try {
    const payload: StaffRequest = {
      username: form.username,
      realName: form.realName,
      phone: form.phone || undefined,
      role: form.role,
      doctorId: form.doctorId || null
    }
    if (form.password) {
      payload.password = form.password
    }

    if (isEditing.value && editingId.value) {
      await updateStaff(editingId.value, payload)
      showSuccessToast('修改成功')
    } else {
      if (!payload.password) {
        showFailToast('新建员工时密码为必填')
        return
      }
      await createStaff(payload)
      showSuccessToast('创建成功')
    }
    showDialog.value = false
    await loadStaff()
  } catch {
    // 静默
  } finally {
    submitting.value = false
  }
}

async function handleDelete(staff: StaffVO) {
  try {
    await showConfirmDialog({
      title: '删除确认',
      message: `确定要删除员工「${staff.realName}」吗？`,
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await deleteStaff(staff.id)
    showSuccessToast('删除成功')
    await loadStaff()
  } catch {
    // 静默
  }
}
</script>

<style scoped>
.employee-page {
  animation: fade-in 0.3s ease;
}

.page-loading {
  padding: 80px 0;
}

.list-section {
  padding: 12px 16px;
}

.staff-card {
  margin-bottom: 12px;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-card);
}

.staff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.staff-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.staff-name {
  font-weight: var(--font-weight-semibold);
  font-size: 15px;
}

.staff-actions {
  display: flex;
  gap: 12px;
  color: var(--color-text-tertiary);
}

.staff-actions .van-icon {
  cursor: pointer;
  transition: color var(--transition-fast);
}

.staff-actions .van-icon:hover {
  color: var(--color-text);
}

.staff-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--color-divider);
}

.dialog-title {
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
}

.dialog-form {
  padding: 8px 16px 24px;
}

.dialog-action {
  padding: 16px 0;
}
</style>
