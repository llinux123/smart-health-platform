<template>
  <div class="issue-prescription-page page-container">
    <van-nav-bar title="开具处方" left-arrow @click-left="$router.back()" />

    <div class="form-section card">
      <h3 class="section-title">患者信息</h3>
      <van-field
        v-model="form.patientId"
        label="患者ID"
        type="number"
        placeholder="请输入患者ID"
        :rules="[{ required: true, message: '请输入患者ID' }]"
      />
      <van-field
        v-model="form.doctorId"
        label="医生ID"
        type="number"
        placeholder="请输入医生ID"
      />
    </div>

    <div class="form-section card">
      <h3 class="section-title">诊断信息</h3>
      <van-field
        v-model="form.diagnosis"
        type="textarea"
        rows="2"
        placeholder="请输入临床诊断结论"
        :rules="[{ required: true, message: '请输入诊断结论' }]"
      />
    </div>

    <div class="form-section card">
      <h3 class="section-title">
        药品列表
        <van-button size="mini" type="primary" plain @click="addMedicine">
          + 添加
        </van-button>
      </h3>

      <div v-for="(med, idx) in form.medicines" :key="idx" class="medicine-item">
        <div class="medicine-header">
          <span class="medicine-index">药品 {{ idx + 1 }}</span>
          <van-icon v-if="form.medicines.length > 1" name="cross" class="remove-btn" @click="removeMedicine(idx)" />
        </div>
        <van-field v-model="med.medicineId" label="药品ID" type="number" placeholder="药品ID" />
        <van-field v-model="med.medicineName" label="药品名称" placeholder="药品名称" />
        <van-field v-model="med.pharmacyId" label="药房ID" type="number" placeholder="药房ID" />
        <van-field v-model="med.quantity" label="数量" type="number" placeholder="数量" />
        <van-field v-model="med.unit" label="单位" placeholder="如：盒、支、瓶" />
      </div>
    </div>

    <div class="action-bar">
      <van-button type="primary" block :loading="submitting" @click="onSubmit">
        开具处方
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { issuePrescription } from '@/api/prescription'

const router = useRouter()
const submitting = ref(false)

function createEmptyMed() {
  return { medicineId: '', medicineName: '', pharmacyId: '', quantity: '', unit: '' }
}

const form = ref({
  patientId: '',
  doctorId: '',
  diagnosis: '',
  medicines: [createEmptyMed()]
})

function addMedicine() {
  form.value.medicines.push(createEmptyMed())
}

function removeMedicine(idx) {
  form.value.medicines.splice(idx, 1)
}

async function onSubmit() {
  // 基本校验
  if (!form.value.patientId) return
  if (!form.value.diagnosis) return
  if (form.value.medicines.length === 0) return

  const validMeds = form.value.medicines.every(m =>
    m.medicineId && m.medicineName && m.pharmacyId && m.quantity && m.unit
  )
  if (!validMeds) return

  submitting.value = true
  try {
    const payload = {
      patientId: Number(form.value.patientId),
      diagnosis: form.value.diagnosis,
      medicines: form.value.medicines.map(m => ({
        medicineId: Number(m.medicineId),
        medicineName: m.medicineName,
        pharmacyId: Number(m.pharmacyId),
        quantity: Number(m.quantity),
        unit: m.unit
      }))
    }

    const params = {}
    if (form.value.doctorId) {
      params.doctorId = Number(form.value.doctorId)
    }

    await issuePrescription(payload, params.doctorId)
    showSuccessToast('处方开具成功')
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
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.medicine-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.medicine-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.medicine-index {
  font-size: 13px;
  font-weight: 500;
  color: #1989fa;
}

.remove-btn {
  color: #ee0a24;
  font-size: 16px;
  cursor: pointer;
}

.action-bar {
  padding: 16px;
}
</style>
