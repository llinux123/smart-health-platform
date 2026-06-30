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
        />
        <van-field
          v-model="form.deptName"
          label="科室"
          placeholder="如：内科、外科"
          :rules="[{ required: true, message: '请输入科室名称' }]"
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

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import { createSchedule } from '@/api/registration'
import { getShiftName } from '@/utils/format'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

const form = ref({
  doctorId: '',
  deptName: '',
  workDate: '',
  shift: null,
  totalCount: '',
  price: ''
})

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

function onDateConfirm({ selectedValues }) {
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

function onShiftConfirm({ selectedOptions }) {
  form.value.shift = selectedOptions[0]?.value
  showShiftPicker.value = false
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await createSchedule({
      doctorId: Number(form.value.doctorId),
      deptName: form.value.deptName,
      workDate: form.value.workDate,
      shift: form.value.shift,
      totalCount: Number(form.value.totalCount),
      price: Number(form.value.price)
    })
    showSuccessToast('排班创建成功')
    setTimeout(() => router.back(), 1500)
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-section {
  margin: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #333;
}

.action-bar {
  padding: 16px;
}
</style>
