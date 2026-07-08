<template>
  <div class="knowledge-page page-container">
    <van-nav-bar title="知识库管理" left-arrow @click-left="$router.back()">
      <template #right>
        <van-icon name="plus" size="18" @click="showImportDialog" />
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

    <van-loading v-if="loading" class="page-loading" />
    <template v-else>
      <EmptyState v-if="knowledgeList.length === 0" description="暂无知识库数据" />
      <div v-else class="list-section">
        <div v-for="doc in knowledgeList" :key="doc.id" class="knowledge-card card">
          <div class="knowledge-header">
            <div class="knowledge-info">
              <span class="knowledge-title">{{ doc.title }}</span>
              <van-tag type="primary" size="medium">{{ doc.category }}</van-tag>
            </div>
            <div class="knowledge-actions">
              <van-icon name="delete" size="16" @click="handleDelete(doc)" />
            </div>
          </div>
          <div class="knowledge-content">{{ truncate(doc.content, 120) }}</div>
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

    <!-- 导入弹窗 -->
    <van-popup v-model:show="showDialog" position="bottom" round style="max-height: 85vh">
      <div class="dialog-header">
        <span class="dialog-title">导入医学知识</span>
        <van-icon name="cross" @click="showDialog = false" />
      </div>
      <van-form ref="formRef" @submit="onSubmit" class="dialog-form">
        <van-field
          v-model="form.title"
          label="标题"
          placeholder="请输入文档标题"
          :rules="[{ required: true, message: '请输入标题' }]"
        />
        <van-field
          v-model="form.category"
          label="分类"
          placeholder="如：内科、外科"
          :rules="[{ required: true, message: '请输入分类' }]"
        />
        <van-field
          v-model="form.content"
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { showConfirmDialog, showSuccessToast } from 'vant'
import {
  listKnowledge,
  importKnowledge,
  deleteKnowledge,
  type KnowledgeDocument,
  type KnowledgeImportRequest
} from '@/api/knowledge'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(true)
const knowledgeList = ref<KnowledgeDocument[]>([])
const total = ref(0)
const currentPage = ref(1)
const size = ref(10)
const keyword = ref('')

const showDialog = ref(false)
const submitting = ref(false)
const formRef = ref<any>(null)

const form = reactive<KnowledgeImportRequest>({
  title: '',
  content: '',
  category: ''
})

onMounted(async () => {
  await loadKnowledge()
})

async function loadKnowledge() {
  loading.value = true
  try {
    const res = await listKnowledge(currentPage.value, size.value, keyword.value || undefined)
    knowledgeList.value = res.list
    total.value = res.total
  } catch {
    // 静默
  } finally {
    loading.value = false
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

function showImportDialog() {
  form.title = ''
  form.content = ''
  form.category = ''
  showDialog.value = true
}

async function onSubmit() {
  submitting.value = true
  try {
    await importKnowledge({ ...form })
    showSuccessToast('导入成功')
    showDialog.value = false
    currentPage.value = 1
    await loadKnowledge()
  } catch {
    // 静默
  } finally {
    submitting.value = false
  }
}

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
  } catch {
    // 静默
  }
}

function truncate(text: string, maxLen: number): string {
  if (!text) return ''
  if (text.length <= maxLen) return text
  return text.substring(0, maxLen) + '...'
}
</script>

<style scoped>
.knowledge-page {
  animation: fade-in 0.3s ease;
}

.page-loading {
  padding: 80px 0;
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
</style>
