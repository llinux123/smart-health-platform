<template>
  <div class="schedule-manage-page page-container">
    <van-nav-bar title="排班管理" left-arrow @click-left="$router.back()" />

    <div class="form-section card">
      <h3 class="section-title">新建排班</h3>
      <van-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <van-field
          v-model="form.doctorId"
          label="医生ID"
          type="number"
          placeholder="请输入医生ID"
          :rules="[{ required: true, message: '请输入医生ID' }]"
          @blur="onDoctorIdBlur"
        />
        <van-field
          v-model="doctorNameLabel"
          label="医生姓名"
          readonly
          placeholder="输入医生ID后自动显示"
        />
        <van-field
          v-model="form.deptName"
          label="科室"
          placeholder="输入医生ID后自动填充"
          :rules="[{ required: true, message: '科室不能为空' }]"
        />
        <van-field
          v-model="form.workDate"
          is-link
          readonly
          label="出诊日期"
          placeholder="请选择日期"
          @click="showDatePicker = true"
          :rules="[{ required: true, message: '请选择出诊日期' }]"
        />
        <van-field
          v-model="shiftLabel"
          is-link
          readonly
          label="班次"
          placeholder="请选择班次"
          @click="showShiftPicker = true"
          :rules="[{ required: true, message: '请选择班次' }]"
        />
        <van-field
          v-model="form.totalCount"
          label="号源量"
          type="number"
          placeholder="请输入总号源量"
          :rules="[{ required: true, message: '请输入号源量' }]"
        />
        <van-field
          v-model="form.price"
          label="挂号费"
          type="number"
          placeholder="请输入挂号费（元）"
          :rules="[{ required: true, message: '请输入挂号费' }]"
        />
      </van-form>
    </div>

    <div class="action-bar">
      <van-button type="primary" block :loading="submitting" @click="onSubmit">
        创建排班
      </van-button>
    </div>

    <!-- 日期选择器 -->
    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker
        v-model="datePickerValue"
        title="选择出诊日期"
        :min-date="minDate"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>

    <!-- 班次选择器 -->
    <van-popup v-model:show="showShiftPicker" position="bottom" round>
      <van-picker
        :columns="shiftOptions"
        @confirm="onShiftConfirm"
        @cancel="showShiftPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { createSchedule, getDoctorDetail } from '@/api/registration'
import { getShiftName } from '@/utils/format'

const router = useRouter()
const formRef = ref<any>(null)
const submitting = ref(false)

const form = ref({
  doctorId: '',
  departmentId: null as number | null,
  deptName: '',
  workDate: '',
  shift: null as number | null,
  totalCount: '',
  price: ''
})

const doctorNameLabel = ref('')
const doctorLoading = ref(false)

/** 医生ID失焦后自动查询医生详情，回填科室 */
async function onDoctorIdBlur() {
  const id = Number(form.value.doctorId)
  if (!id || isNaN(id)) {
    doctorNameLabel.value = ''
    form.value.deptName = ''
    form.value.departmentId = null
    return
  }

  doctorLoading.value = true
  try {
    const res = await getDoctorDetail(id)
    const data = res?.data ?? res
    if (data) {
      doctorNameLabel.value = data.name || ''
      form.value.deptName = data.deptName || ''
      form.value.departmentId = data.primaryDepartmentId ?? null
    } else {
      doctorNameLabel.value = ''
      form.value.deptName = ''
      form.value.departmentId = null
      showFailToast({ message: '未找到该医生', duration: 1500 })
    }
  } catch {
    doctorNameLabel.value = ''
    form.value.deptName = ''
    form.value.departmentId = null
  } finally {
    doctorLoading.value = false
  }
}

const rules = {
  doctorId: [{ required: true, message: '请输入医生ID' }],
  deptName: [{ required: true, message: '请输入科室名称' }],
  workDate: [{ required: true, message: '请选择出诊日期' }],
  shift: [{ required: true, message: '请选择班次' }],
  totalCount: [{ required: true, message: '请输入号源量' }],
  price: [{ required: true, message: '请输入挂号费' }]
}

const shiftLabel = computed(() => form.value.shift ? getShiftName(form.value.shift) : '')

// 日期选择
const showDatePicker = ref(false)
const today = new Date()
const minDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
const datePickerValue = ref([
  String(today.getFullYear()),
  String(today.getMonth() + 1).padStart(2, '0'),
  String(today.getDate()).padStart(2, '0')
])

function onDateConfirm({ selectedValues }: { selectedValues: string[] }) {
  form.value.workDate = selectedValues.join('-')
  showDatePicker.value = false
}

// 班次选择
const showShiftPicker = ref(false)
const shiftOptions = [
  { text: '上午', value: 1 },
  { text: '下午', value: 2 },
  { text: '晚上', value: 3 }
]

function onShiftConfirm({ selectedOptions }: { selectedOptions: { text: string; value: number }[] }) {
  form.value.shift = selectedOptions[0]?.value
  showShiftPicker.value = false
}

async function onSubmit() {
  try {
    if (!formRef.value) return
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await createSchedule({
      doctorId: Number(form.value.doctorId),
      departmentId: form.value.departmentId,
      deptName: form.value.deptName,
      workDate: form.value.workDate,
      shift: form.value.shift,
      totalCount: Number(form.value.totalCount),
      price: Number(form.value.price)
    })
    showSuccessToast({ message: '排班创建成功', duration: 1500 })
    setTimeout(() => router.back(), 1500)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.schedule-manage-page {
  animation: fade-in 0.3s ease;
}

.form-section {
  margin: 16px;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 16px;
  margin-bottom: 16px;
}

.form-section .section-title {
  font-size: 15px;
  font-weight: var(--font-weight-semibold);
  margin-bottom: 12px;
  color: var(--color-text);
}

.action-bar {
  padding: 16px;
}

.action-bar :deep(.van-button--primary) {
  height: 48px;
  border-radius: var(--radius-lg);
  font-weight: var(--font-weight-semibold);
}
</style>
