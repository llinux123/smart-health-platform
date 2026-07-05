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
        <!-- 药品搜索选择 -->
        <van-field
          :model-value="med.medicineName || '点击选择药品'"
          is-link
          readonly
          label="药品"
          :class="{ 'field-placeholder': !med.medicineName }"
          @click="showDrugPicker(idx)"
        />
        <van-field v-model="med.pharmacyId" label="药房ID" type="number" placeholder="药房ID" />
        <van-field v-model="med.quantity" label="数量" type="number" placeholder="数量" />
        <van-field v-model="med.unit" label="单位" placeholder="如：盒、支、瓶" readonly />
        <van-field v-model="med.spec" label="规格" placeholder="如：15g/支、0.5g*24粒" readonly />
        <van-field v-model="med.usage" label="用法用量" placeholder="如：口服，每日3次" />
        <van-field v-model="med.price" label="单价" type="number" placeholder="如：25.50" readonly />
      </div>
    </div>

    <div class="action-bar">
      <van-button type="primary" block :loading="submitting" @click="onSubmit">
        开具处方
      </van-button>
    </div>

    <!-- 药品搜索弹出框 -->
    <van-popup v-model:show="showPicker" position="bottom" round style="max-height: 70vh">
      <div class="picker-header">
        <span class="picker-title">选择药品</span>
        <van-icon name="cross" @click="showPicker = false" />
      </div>
      <van-search
        v-model="searchKeyword"
        placeholder="输入药品名称搜索"
        @search="doSearch"
        @update:model-value="onSearchInput"
      />
      <van-loading v-if="searchLoading" class="search-loading" size="24" />
      <van-cell-group v-else-if="searchResults.length > 0">
        <van-cell
          v-for="item in searchResults"
          :key="item.id"
          :title="item.name"
          :label="`${item.spec || ''} ${item.manufacturer || ''}`"
          :value="item.price ? `¥${item.price}` : ''"
          @click="selectMedicine(item)"
          clickable
        />
      </van-cell-group>
      <div v-else-if="searchKeyword && !searchLoading" class="empty-result">
        未找到匹配药品
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { issuePrescription, searchMedicine } from '@/api/prescription'

const router = useRouter()
const submitting = ref(false)

// 药品搜索
const showPicker = ref(false)
const searchKeyword = ref('')
const searchLoading = ref(false)
const searchResults = ref([])
let pickerTargetIdx = -1
let searchTimer: ReturnType<typeof setTimeout> | null = null

function createEmptyMed() {
  return { medicineId: '', medicineName: '', pharmacyId: '', quantity: '', unit: '', spec: '', usage: '', price: '' }
}

const form = ref({
  patientId: '',
  doctorId: '',
  diagnosis: '',
  medicines: [createEmptyMed()]
})

function showDrugPicker(idx: number) {
  pickerTargetIdx = idx
  searchKeyword.value = ''
  searchResults.value = []
  showPicker.value = true
  // 自动加载所有药品
  doSearch()
}

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => doSearch(), 300)
}

async function doSearch() {
  searchLoading.value = true
  try {
    searchResults.value = await searchMedicine(searchKeyword.value, 20)
  } catch {
    // 静默
  } finally {
    searchLoading.value = false
  }
}

function selectMedicine(item: any) {
  const med = form.value.medicines[pickerTargetIdx]
  if (med) {
    med.medicineId = item.id
    med.medicineName = item.name
    med.unit = item.unit || ''
    med.spec = item.spec || ''
    med.price = item.price != null ? String(item.price) : ''
  }
  showPicker.value = false
}

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
        unit: m.unit,
        spec: m.spec || null,
        usage: m.usage || null,
        price: m.price ? Number(m.price) : null
      }))
    }

    const doctorId = form.value.doctorId ? Number(form.value.doctorId) : undefined
    await issuePrescription(payload, doctorId!)
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
.issue-prescription-page {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.medicine-item {
  border: 1px solid var(--color-divider);
  border-radius: var(--radius-md);
  padding: 12px;
  margin-bottom: 12px;
  background: var(--color-bg);
}

.medicine-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.medicine-index {
  font-size: var(--font-size-caption);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
}

.remove-btn {
  color: var(--color-danger);
  font-size: 16px;
  cursor: pointer;
}

.action-bar {
  padding: 16px;
}

.action-bar :deep(.van-button--primary) {
  height: 48px;
  border-radius: var(--radius-lg);
  font-weight: var(--font-weight-semibold);
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--color-divider);
}

.picker-title {
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
}

.search-loading {
  padding: 40px 0;
}

.empty-result {
  text-align: center;
  padding: 40px 16px;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.field-placeholder :deep(.van-field__control) {
  color: var(--color-text-tertiary);
}
</style>
