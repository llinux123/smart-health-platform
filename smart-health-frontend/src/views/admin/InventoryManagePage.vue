<template>
  <div class="inventory-page page-container">
    <van-nav-bar title="库存管理" left-arrow @click-left="$router.back()" />

    <van-tabs v-model:active="activeTab">
      <!-- Tab 1: 库存概览 -->
      <van-tab title="库存概览">
        <van-loading v-if="invLoading" class="page-loading" />
        <template v-else>
          <EmptyState v-if="inventoryList.length === 0" description="暂无库存数据" />
          <div v-else class="list-section">
            <div v-for="item in inventoryList" :key="item.id" class="inv-item card">
              <div class="inv-item-top">
                <span class="inv-drug-name">{{ item.medicineName }}</span>
                <span class="inv-unit">{{ item.unit }}</span>
              </div>
              <div class="inv-stock-row">
                <span>实际库存：<em class="stock-num">{{ item.stock }}</em></span>
                <span>冻结：<em class="lock-num">{{ item.lockStock }}</em></span>
                <span>可用：<em class="avail-num">{{ item.stock - item.lockStock }}</em></span>
              </div>
            </div>
          </div>
        </template>
      </van-tab>

      <!-- Tab 2: 入库 -->
      <van-tab title="入库">
        <div class="form-section card">
          <van-field v-model="inboundForm.medicineName" label="药品" is-link readonly placeholder="点击选择药品" @click="showDrugPicker('inbound')" />
          <van-field v-model="inboundForm.quantity" label="入库数量" type="number" placeholder="请输入数量" :rules="[{ required: true }]" />
          <van-field v-model="inboundForm.reason" label="入库原因" placeholder="可选" />
          <van-button type="primary" block :loading="inboundLoading" @click="doInbound" style="margin-top:16px">确认入库</van-button>
        </div>
      </van-tab>

      <!-- Tab 3: 出库 -->
      <van-tab title="出库">
        <div class="form-section card">
          <van-field v-model="outboundForm.medicineName" label="药品" is-link readonly placeholder="点击选择药品" @click="showDrugPicker('outbound')" />
          <van-field v-model="outboundForm.quantity" label="出库数量" type="number" placeholder="请输入数量" :rules="[{ required: true }]" />
          <van-field v-model="outboundForm.reason" label="出库原因" placeholder="可选" />
          <van-button type="danger" block :loading="outboundLoading" @click="doOutbound" style="margin-top:16px">确认出库</van-button>
        </div>
      </van-tab>

      <!-- Tab 4: 盘点 -->
      <van-tab title="盘点">
        <div class="form-section card">
          <van-field v-model="reconcileForm.medicineName" label="药品" is-link readonly placeholder="点击选择药品" @click="showDrugPicker('reconcile')" />
          <van-field v-model="reconcileForm.actualStock" label="实际库存" type="number" placeholder="请输入盘点数量" :rules="[{ required: true }]" />
          <van-field v-model="reconcileForm.reason" label="盘点说明" placeholder="可选" />
          <van-button color="#1989fa" block :loading="reconcileLoading" @click="doReconcile" style="margin-top:16px">确认盘点</van-button>
        </div>
      </van-tab>

      <!-- Tab 5: 变动日志 -->
      <van-tab title="变动日志">
        <van-loading v-if="logLoading" class="page-loading" />
        <EmptyState v-else-if="logList.length === 0" description="暂无变动记录" />
        <div v-else class="list-section">
          <div v-for="log in logList" :key="log.id" class="log-item card">
            <div class="log-header">
              <van-tag :type="logTagType(log.changeType)">{{ log.changeType }}</van-tag>
              <span class="log-time">{{ log.createTime }}</span>
            </div>
            <div class="log-body">
              <span>药品ID: {{ log.medicineId }}</span>
              <span>变动: {{ log.quantityChange > 0 ? '+' : '' }}{{ log.quantityChange }}</span>
              <span>库存: {{ log.stockBefore }} → {{ log.stockAfter }}</span>
            </div>
            <div v-if="log.reason" class="log-reason">{{ log.reason }}</div>
          </div>
        </div>
      </van-tab>
    </van-tabs>

    <!-- 药品搜索弹出框 -->
    <van-popup v-model:show="showPicker" position="bottom" round style="max-height: 60vh">
      <div class="picker-header">
        <span class="picker-title">选择药品</span>
        <van-icon name="cross" @click="showPicker = false" />
      </div>
      <van-search v-model="searchKeyword" placeholder="输入药品名称搜索" @update:model-value="onPickerSearch" />
      <van-cell-group>
        <van-cell v-for="item in searchResults" :key="item.id" :title="item.name" :label="item.spec || ''" @click="selectPickerDrug(item)" clickable />
      </van-cell-group>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showSuccessToast, showFailToast } from 'vant'
import { listInventory, inbound, outbound, reconcile, listInventoryLogs } from '@/api/inventory'
import { searchMedicine } from '@/api/prescription'
import EmptyState from '@/components/EmptyState.vue'

const activeTab = ref(0)

// Tab 1: 库存概览
const inventoryList = ref([])
const invLoading = ref(true)

// Tab 2-4: 操作表单
const pickerMode = ref('')
const showPicker = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])

const inboundForm = ref({ medicineName: '', medicineId: null, quantity: '', reason: '' })
const outboundForm = ref({ medicineName: '', medicineId: null, quantity: '', reason: '' })
const reconcileForm = ref({ medicineName: '', medicineId: null, actualStock: '', reason: '' })
const inboundLoading = ref(false)
const outboundLoading = ref(false)
const reconcileLoading = ref(false)

// Tab 5: 变动日志
const logList = ref([])
const logLoading = ref(false)

onMounted(() => loadInventory())

// ============ 库存概览 ============
async function loadInventory() {
  invLoading.value = true
  try {
    inventoryList.value = await listInventory(1)
  } catch { /* 静默 */ }
  finally { invLoading.value = false }
}

// ============ 药品选择器 ============
function showDrugPicker(mode) {
  pickerMode.value = mode
  searchKeyword.value = ''
  searchResults.value = []
  showPicker.value = true
  setTimeout(() => doPickerSearch(), 100)
}

let pickerTimer = null
function onPickerSearch() {
  if (pickerTimer) clearTimeout(pickerTimer)
  pickerTimer = setTimeout(() => doPickerSearch(), 300)
}

async function doPickerSearch() {
  try {
    searchResults.value = await searchMedicine(searchKeyword.value, 20)
  } catch { /* 静默 */ }
}

function selectPickerDrug(item) {
  const form = pickerMode.value === 'inbound' ? inboundForm
    : pickerMode.value === 'outbound' ? outboundForm
    : reconcileForm
  form.value.medicineName = item.name
  form.value.medicineId = item.id
  showPicker.value = false
}

// ============ 入库 ============
async function doInbound() {
  if (!inboundForm.value.medicineName || !inboundForm.value.quantity) {
    showFailToast('请填写完整信息')
    return
  }
  inboundLoading.value = true
  try {
    await inbound({ pharmacyId: 1, medicineId: inboundForm.value.medicineId, quantity: Number(inboundForm.value.quantity), reason: inboundForm.value.reason })
    showSuccessToast('入库成功')
    inboundForm.value = { medicineName: '', medicineId: null, quantity: '', reason: '' }
    loadInventory()
  } catch { /* 静默 */ }
  finally { inboundLoading.value = false }
}

// ============ 出库 ============
async function doOutbound() {
  if (!outboundForm.value.medicineName || !outboundForm.value.quantity) {
    showFailToast('请填写完整信息')
    return
  }
  outboundLoading.value = true
  try {
    await outbound({ pharmacyId: 1, medicineId: outboundForm.value.medicineId, quantity: Number(outboundForm.value.quantity), reason: outboundForm.value.reason })
    showSuccessToast('出库成功')
    outboundForm.value = { medicineName: '', medicineId: null, quantity: '', reason: '' }
    loadInventory()
  } catch { /* 静默 */ }
  finally { outboundLoading.value = false }
}

// ============ 盘点 ============
async function doReconcile() {
  if (!reconcileForm.value.medicineName || reconcileForm.value.actualStock === '') {
    showFailToast('请填写完整信息')
    return
  }
  reconcileLoading.value = true
  try {
    await reconcile({ pharmacyId: 1, medicineId: reconcileForm.value.medicineId, actualStock: Number(reconcileForm.value.actualStock), reason: reconcileForm.value.reason })
    showSuccessToast('盘点成功')
    reconcileForm.value = { medicineName: '', medicineId: null, actualStock: '', reason: '' }
    loadInventory()
  } catch { /* 静默 */ }
  finally { reconcileLoading.value = false }
}

// ============ 日志 ============
function logTagType(type) {
  const map = { INBOUND: 'success', OUTBOUND: 'danger', RECONCILE: 'primary', DEDUCT: 'warning', RESTORE: 'info' }
  return map[type] || 'default'
}
</script>

<style scoped>
.inventory-page {
  animation: fade-in 0.3s ease;
}

.list-section {
  padding: 12px 16px;
}

.inv-item, .log-item {
  margin-bottom: 12px;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-card);
}

.inv-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.inv-drug-name {
  font-weight: var(--font-weight-semibold);
  font-size: 15px;
}

.inv-unit {
  font-size: 12px;
  color: var(--color-text-tertiary);
  background: var(--color-bg);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.inv-stock-row {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.stock-num { color: var(--color-primary); font-weight: var(--font-weight-semibold); font-style: normal; }
.lock-num { color: var(--color-warning); font-weight: var(--font-weight-semibold); font-style: normal; }
.avail-num { color: var(--color-success); font-weight: var(--font-weight-semibold); font-style: normal; }

.form-section {
  margin: 16px;
  padding: 16px;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.log-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.log-body {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.log-reason {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.page-loading {
  padding: 80px 0;
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
</style>
