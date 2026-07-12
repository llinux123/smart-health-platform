<template>
  <div class="knowledge-page page-container">
    <van-nav-bar title="知识库管理" left-arrow @click-left="$router.back()">
      <template #right>
        <van-icon name="plus" size="18" @click="showCreateDialog" />
      </template>
    </van-nav-bar>

    <!-- 搜索栏 -->
    <van-search
      v-model="keyword"
      placeholder="搜索标题或内容"
      shape="round"
      @search="onSearch"
      @clear="onClear"
    />

    <!-- 分类过滤 -->
    <div class="filter-bar">
      <van-field
        v-model="categoryLabel"
        is-link
        readonly
        label="分类"
        placeholder="全部"
        @click="showCategoryPicker = true"
      />
    </div>

    <van-loading v-if="loading" class="page-loading" />
    <template v-else>
      <EmptyState v-if="knowledgeList.length === 0" description="暂无知识库数据" />
      <div v-else class="list-section">
        <div
          v-for="doc in knowledgeList"
          :key="doc.id"
          class="knowledge-card card"
          @click="openDetail(doc)"
        >
          <div class="knowledge-header">
            <div class="knowledge-info">
              <span class="knowledge-title">{{ doc.title }}</span>
              <van-tag type="primary" size="medium">{{ doc.category }}</van-tag>
            </div>
            <div class="knowledge-actions" @click.stop>
              <van-icon name="delete-o" size="16" @click="handleDelete(doc)" />
            </div>
          </div>
          <div class="knowledge-content">{{ truncate(doc.content, 120) }}</div>
          <div v-if="doc.updateTime" class="knowledge-time">
            {{ formatTime(doc.updateTime) }}
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > size" class="pagination">
        <van-pagination
          v-model="currentPage"
          :total-items="total"
          :items-per-page="size"
          :show-page-size="5"
          force-ellipses
          @change="loadKnowledge"
        />
      </div>
    </template>

    <!-- 新建弹窗（Create） -->
    <van-popup v-model:show="showCreate" position="bottom" round style="max-height: 85vh">
      <div class="dialog-header">
        <span class="dialog-title">导入医学知识</span>
        <van-icon name="cross" @click="showCreate = false" />
      </div>
      <van-form ref="createFormRef" @submit="onCreateSubmit" class="dialog-form">
        <van-field
          v-model="createForm.title"
          label="标题"
          placeholder="请输入文档标题"
          :rules="[{ required: true, message: '请输入标题' }]"
        />
        <van-field
          v-model="createForm.categoryLabel"
          is-link
          readonly
          label="分类"
          placeholder="请选择分类"
          :rules="[{ required: true, message: '请选择分类' }]"
          @click="onCreateCategoryClick"
        />
        <van-field
          v-model="createForm.content"
          label="内容"
          type="textarea"
          rows="4"
          autosize
          placeholder="请输入医学知识内容"
          :rules="[{ required: true, message: '请输入内容' }]"
        />
        <div class="dialog-action">
          <van-button round block type="primary" native-type="submit" :loading="submitting">
            确认导入
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 详情/编辑弹窗（Detail / Edit） -->
    <van-popup v-model:show="showDetail" position="bottom" round style="max-height: 85vh">
      <div class="dialog-header">
        <span class="dialog-title">{{ isEditing ? '编辑医学知识' : '知识详情' }}</span>
        <van-icon name="cross" @click="closeDetail" />
      </div>
      <van-form v-if="detailDoc" @submit="onEditSubmit" class="dialog-form">
        <van-field
          v-model="detailDoc.title"
          label="标题"
          placeholder="请输入文档标题"
          :readonly="!isEditing"
          :rules="isEditing ? [{ required: true, message: '请输入标题' }] : []"
        />
        <van-field
          v-model="detailDoc.category"
          :is-link="isEditing"
          :readonly="!isEditing"
          label="分类"
          placeholder="请选择分类"
          :rules="isEditing ? [{ required: true, message: '请选择分类' }] : []"
          @click="isEditing ? onEditCategoryClick() : undefined"
        />
        <van-field
          v-model="detailDoc.content"
          label="内容"
          type="textarea"
          rows="5"
          autosize
          :readonly="!isEditing"
          placeholder="请输入医学知识内容"
          :rules="isEditing ? [{ required: true, message: '请输入内容' }] : []"
        />
        <div v-if="detailDoc.updateTime" class="detail-time">
          最后更新: {{ formatTime(detailDoc.updateTime) }}
        </div>
        <div class="dialog-action" v-if="isEditing">
          <van-button round block type="primary" native-type="submit" :loading="submitting">
            保存修改
          </van-button>
        </div>
        <div class="dialog-action" v-else>
          <van-button round block type="primary" @click="isEditing = true">编辑</van-button>
          <van-button round block type="default" style="margin-top:8px" @click="closeDetail">关闭</van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 分类选择器（Picker） -->
    <van-popup v-model:show="showCategoryPicker" position="bottom" round>
      <van-picker
        :columns="categoryColumns"
        @confirm="onCategoryConfirm"
        @cancel="showCategoryPicker = false"
      />
    </van-popup>

    <!-- 编辑弹窗中的分类选择器 -->
    <van-popup v-model:show="showEditCategoryPicker" position="bottom" round>
      <van-picker
        :columns="categoryColumns"
        @confirm="onEditCategoryConfirm"
        @cancel="showEditCategoryPicker = false"
      />
    </van-popup>

    <!-- 新建弹窗中的分类选择器 -->
    <van-popup v-model:show="showCreateCategoryPicker" position="bottom" round>
      <van-picker
        :columns="categoryColumns"
        @confirm="onCreateCategoryConfirm"
        @cancel="showCreateCategoryPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { showConfirmDialog, showSuccessToast, showFailToast } from 'vant'
import {
  listKnowledge,
  importKnowledge,
  deleteKnowledge,
  getKnowledge,
  updateKnowledge,
  listCategories,
  type KnowledgeDocument,
  type KnowledgeImportRequest,
  type KnowledgeUpdateRequest
} from '@/api/knowledge'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(true)
const knowledgeList = ref<KnowledgeDocument[]>([])
const total = ref(0)
const currentPage = ref(1)
const size = ref(10)
const keyword = ref('')
const selectedCategory = ref('')

const submitting = ref(false)

// ====== Category ======
const showCategoryPicker = ref(false)
const showEditCategoryPicker = ref(false)
const showCreateCategoryPicker = ref(false)
const categories = ref<string[]>([])
const categoryColumns = computed(() => {
  const cols = categories.value.map(c => ({ text: c, value: c }))
  cols.unshift({ text: '全部', value: '' })
  return cols
})
const categoryLabel = computed(() => selectedCategory.value || '全部')

// ====== Detail / Edit ======
const showDetail = ref(false)
const isEditing = ref(false)
const detailDoc = ref<KnowledgeDocument | null>(null)

// ====== Create ======
const showCreate = ref(false)
const createFormRef = ref<any>(null)
const createForm = reactive<KnowledgeImportRequest & { categoryLabel: string }>({
  title: '',
  content: '',
  category: '',
  categoryLabel: ''
})

onMounted(async () => {
  await Promise.all([loadKnowledge(), loadCategories()])
})

async function loadKnowledge() {
  loading.value = true
  try {
    const res = await listKnowledge(
      currentPage.value,
      size.value,
      keyword.value || undefined,
      selectedCategory.value || undefined
    )
    knowledgeList.value = res.list
    total.value = res.total
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await listCategories()
  } catch {
    // 静默
  }
}

function onSearch() {
  currentPage.value = 1
  loadKnowledge()
}

function onClear() {
  keyword.value = ''
  currentPage.value = 1
  loadKnowledge()
}

function onCategoryConfirm({ selectedOptions }: any) {
  selectedCategory.value = selectedOptions[0].value
  showCategoryPicker.value = false
  currentPage.value = 1
  loadKnowledge()
}

// ====== Detail ======
async function openDetail(doc: KnowledgeDocument) {
  try {
    const full = await getKnowledge(doc.id)
    detailDoc.value = full
  } catch {
    detailDoc.value = doc
  }
  isEditing.value = false
  showDetail.value = true
}

function closeDetail() {
  showDetail.value = false
  detailDoc.value = null
  isEditing.value = false
}

// ====== Edit ======
function onEditCategoryClick() {
  showEditCategoryPicker.value = true
}

function onEditCategoryConfirm({ selectedOptions }: any) {
  if (detailDoc.value) {
    detailDoc.value.category = selectedOptions[0].value
  }
  showEditCategoryPicker.value = false
}

async function onEditSubmit() {
  if (!detailDoc.value) return
  submitting.value = true
  try {
    await updateKnowledge(detailDoc.value.id, {
      title: detailDoc.value.title,
      content: detailDoc.value.content,
      category: detailDoc.value.category
    })
    showSuccessToast('修改成功')
    closeDetail()
    await loadKnowledge()
  } catch {
    // 静默
  } finally {
    submitting.value = false
  }
}

// ====== Create ======
function showCreateDialog() {
  createForm.title = ''
  createForm.content = ''
  createForm.category = ''
  createForm.categoryLabel = ''
  showCreate.value = true
}

function onCreateCategoryClick() {
  showCreateCategoryPicker.value = true
}

function onCreateCategoryConfirm({ selectedOptions }: any) {
  const val = selectedOptions[0].value
  createForm.category = val
  createForm.categoryLabel = val
  showCreateCategoryPicker.value = false
}

async function onCreateSubmit() {
  submitting.value = true
  try {
    await importKnowledge({
      title: createForm.title,
      content: createForm.content,
      category: createForm.category
    })
    showSuccessToast('导入成功')
    showCreate.value = false
    currentPage.value = 1
    await loadKnowledge()
    await loadCategories()
  } catch {
    // 静默
  } finally {
    submitting.value = false
  }
}

// ====== Delete ======
async function handleDelete(doc: KnowledgeDocument) {
  try {
    await showConfirmDialog({
      title: '删除确认',
      message: `确定要删除「${doc.title}」吗？`,
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await deleteKnowledge(doc.id)
    showSuccessToast('删除成功')
    await loadKnowledge()
    await loadCategories()
  } catch {
    // 静默
  }
}

function truncate(text: string, maxLen: number): string {
  if (!text) return ''
  if (text.length <= maxLen) return text
  return text.substring(0, maxLen) + '...'
}

function formatTime(t: string): string {
  if (!t) return ''
  const d = new Date(t)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${dd} ${hh}:${mm}`
}
</script>

<style scoped>
.knowledge-page {
  animation: fade-in 0.3s ease;
}

.page-loading {
  padding: 80px 0;
}

.filter-bar {
  --van-cell-horizontal-padding: 16px;
  background: var(--color-bg);
}

.list-section {
  padding: 12px 16px;
}

.knowledge-card {
  margin-bottom: 12px;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.knowledge-card:active {
  transform: scale(0.98);
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.knowledge-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.knowledge-title {
  font-weight: var(--font-weight-semibold);
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-actions {
  display: flex;
  gap: 12px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.knowledge-actions .van-icon {
  cursor: pointer;
  transition: color var(--transition-fast);
}

.knowledge-actions .van-icon:hover {
  color: var(--color-danger, #ee0a24);
}

.knowledge-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.knowledge-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 6px;
}

.pagination {
  padding: 12px 16px 24px;
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

.detail-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
  padding: 8px 16px;
}
</style>