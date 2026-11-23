<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Delete, Document, ArrowLeft, ArrowRight, Refresh } from '@element-plus/icons-vue'
import { documentApi } from '@/api'

const documents = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const uploading = ref(false)
// 批量上传状态
const batchUploading = ref(false)
const batchProgress = ref({ current: 0, total: 0, success: 0, failure: 0 })
// el-upload 选中的文件（on-change 逐个触发，用 debounce 统一批量传）
const pendingFiles = ref([])
const uploadTimer = ref(null)

// el-upload 实例（用于手动清空已选列表）

const formatSize = (bytes) => {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n) => n.toString().padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
    ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

const statusMap = {
  ready: { text: '就绪', cls: 'ready' },
  processing: { text: '处理中', cls: 'processing' },
  failed: { text: '失败', cls: 'failed' }
}

const getStatus = (s) => statusMap[s] || { text: s || '-', cls: '' }

const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await documentApi.list({
      keyword: keyword.value,
      page: page.value,
      size: pageSize.value
    })
    documents.value = res?.records || []
    total.value = res?.total || 0
  } catch (err) {
    console.error('加载文档列表失败', err)
  } finally {
    loading.value = false
  }
}

// el-upload on-change 钩子：逐个文件触发，用 debounce 统一批量传
const handleFileSelected = (file) => {
  pendingFiles.value.push(file.raw)
  // 清除上一个 debounce，等当前所有文件都选完再统一传
  if (uploadTimer.value) clearTimeout(uploadTimer.value)
  uploadTimer.value = setTimeout(async () => {
    const files = [...pendingFiles.value]
    pendingFiles.value = []
    if (!files.length) return

    batchUploading.value = true
    batchProgress.value = { current: 0, total: files.length, success: 0, failure: 0 }
    try {
      const res = await documentApi.uploadBatch(files)
      batchProgress.value = {
        current: files.length,
        total: files.length,
        success: res?.successCount || 0,
        failure: res?.failureCount || 0
      }
      if (res?.failureCount === 0) {
        ElMessage.success(`全部上传成功，共 ${res.successCount} 份`)
      } else if (res?.successCount === 0) {
        ElMessage.error(`全部失败：${res.items?.map(i => i.filename + '(' + i.errorMsg + ')').join('；')}`)
      } else {
        ElMessage.warning(`成功 ${res.successCount} 份，失败 ${res.failureCount} 份`)
        const failed = res.items?.filter(i => !i.success) || []
        if (failed.length) console.warn('批量上传失败项:', failed)
      }
      loadDocuments()
      res?.items?.filter(i => i.success && i.document?.id).forEach(i => {
        startStatusPolling(i.document.id)
      })
    } catch (err) {
      console.error('批量上传失败', err)
      ElMessage.error('批量上传失败：' + (err?.message || '未知错误'))
    } finally {
      batchUploading.value = false
    }
  }, 200)
}

const startStatusPolling = (docId) => {
  if (!docId) return
  let count = 0
  const timer = setInterval(async () => {
    count++
    if (count > 30) {
      clearInterval(timer)
      return
    }
    try {
      const res = await documentApi.detail(docId)
      if (res && res.status && res.status !== 'processing') {
        clearInterval(timer)
        loadDocuments()
        if (res.status === 'ready') {
          ElMessage.success('文档处理完成')
        } else if (res.status === 'failed') {
          ElMessage.error('文档处理失败')
        }
      }
    } catch (e) {
      console.error('轮询状态失败', e)
    }
  }, 2000)
}

const handleDelete = async (doc) => {
  try {
    await ElMessageBox.confirm('确定删除这份文档吗？删除后无法恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await documentApi.delete(doc.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch (err) {
    if (err !== 'cancel') {
      console.error(err)
    }
  }
}

const handleReindex = async (doc) => {
  try {
    ElMessage.info('正在重新索引文档...')
    const res = await documentApi.reindex(doc.id)
    if (res && res.status === 'ready') {
      ElMessage.success('索引完成，切分 ' + (res.chunkCount || 0) + ' 块')
    } else {
      ElMessage.error('索引失败：' + (res?.status || '未知'))
    }
    loadDocuments()
  } catch (err) {
    console.error('重新索引失败', err)
    ElMessage.error('重新索引失败')
  }
}

const handleSearch = () => {
  page.value = 1
  loadDocuments()
}

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

// 上传按钮文字（单文件 / 批量两种状态）
const uploadBtnText = computed(() => {
  if (batchUploading.value) {
    return `批量上传中 (${batchProgress.value.current}/${batchProgress.value.total})`
  }
  if (uploading.value) return '上传中...'
  return '上传文档（可多选）'
})

const changePage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  loadDocuments()
}

onMounted(() => {
  loadDocuments()
})
</script>

<template>
  <div class="doc-page">
    <div class="page-header">
      <div class="header-left">
        <h2>文档管理</h2>
        <span class="doc-count">共 {{ total }} 份文档</span>
      </div>
      <div class="header-right">
        <div class="search-box">
          <el-icon class="search-icon" :size="16" color="#9ca3af"><Search /></el-icon>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索文档..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
        </div>
        <el-upload
          :auto-upload="false"
          multiple
          accept=".pdf,.docx,.doc,.md,.txt"
          :on-change="handleFileSelected"
          :show-file-list="false"
          class="upload-el"
        >
          <el-button type="primary" :loading="batchUploading" :disabled="batchUploading">
            <el-icon :size="14" style="margin-right: 4px"><Upload /></el-icon>
            {{ uploadBtnText }}
          </el-button>
        </el-upload>
      </div>
    </div>

    <div class="doc-grid">
      <div
        v-for="doc in documents"
        :key="doc.id"
        class="doc-card"
      >
        <div class="card-top">
          <div class="doc-icon">
            <el-icon :size="32" color="#667eea"><Document /></el-icon>
          </div>
          <span class="status-badge" :class="'status-' + getStatus(doc.status).cls">
            {{ getStatus(doc.status).text }}
          </span>
        </div>
        <div class="doc-name" :title="doc.filename">{{ doc.filename }}</div>
        <div class="doc-summary">
          {{ doc.summary || '（暂无摘要）' }}
        </div>
        <div class="doc-meta">
          <span class="meta-item">
            <span class="meta-label">类型</span>
            {{ (doc.fileType || '').toUpperCase() }}
          </span>
          <span class="meta-item">
            <span class="meta-label">大小</span>
            {{ formatSize(doc.size) }}
          </span>
        </div>
        <div class="doc-meta">
          <span class="meta-item">
            <span class="meta-label">分块</span>
            {{ doc.chunkCount || 0 }} 块
          </span>
          <span class="meta-item">
            <span class="meta-label">上传</span>
            {{ formatDate(doc.createdAt) }}
          </span>
        </div>
        <div class="card-footer">
          <button class="reindex-btn" @click="handleReindex(doc)">
            <el-icon :size="12"><Refresh /></el-icon>
            重新索引
          </button>
          <button class="del-btn" @click="handleDelete(doc)">
            <el-icon :size="12"><Delete /></el-icon>
            删除
          </button>
        </div>
      </div>

      <div v-if="!loading && documents.length === 0" class="empty-state">
        <el-icon :size="56" color="#d1d5db"><Document /></el-icon>
        <p class="empty-text">暂无文档</p>
        <p class="empty-hint">点击右上角按钮上传你的第一份文档</p>
      </div>

      <div v-if="loading" class="loading-state">加载中...</div>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" @click="changePage(page - 1)" :disabled="page <= 1">
        <el-icon :size="14"><ArrowLeft /></el-icon>
        上一页
      </button>
      <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
      <button class="page-btn" @click="changePage(page + 1)" :disabled="page >= totalPages">
        下一页
        <el-icon :size="14"><ArrowRight /></el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped>
.doc-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px 32px;
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.5px;
}

.doc-count {
  font-size: 13px;
  color: #6b7280;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  z-index: 1;
}

.search-input {
  width: 240px;
  height: 38px;
  padding: 0 14px 0 36px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  background: white;
  transition: all 0.2s;
}

.search-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.search-input::placeholder {
  color: #9ca3af;
}

.upload-btn {
  height: 38px;
  padding: 0 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.upload-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.upload-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.doc-grid {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  align-content: start;
}

.doc-card {
  background: white;
  border: 1.5px solid #e5e7eb;
  border-radius: 12px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.2s;
}

.doc-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.doc-icon {
  display: flex;
  align-items: center;
}

.status-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
}

.status-ready {
  background: #d1fae5;
  color: #065f46;
}

.status-processing {
  background: #fef3c7;
  color: #92400e;
}

.status-failed {
  background: #fee2e2;
  color: #991b1b;
}

.doc-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 42px;
}

.doc-summary {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  min-height: 58px;
}

.doc-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #9ca3af;
}

.meta-item {
  display: flex;
  gap: 4px;
  align-items: center;
}

.meta-label {
  color: #d1d5db;
}

.card-footer {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #f3f4f6;
  padding-top: 10px;
}

.reindex-btn {
  padding: 6px 14px;
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.reindex-btn:hover {
  border-color: #667eea;
  color: #667eea;
  background: #eef2ff;
}

.del-btn {
  padding: 6px 14px;
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.del-btn:hover {
  border-color: #fca5a5;
  color: #ef4444;
  background: #fef2f2;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-text {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
}

.empty-hint {
  margin: 0;
  font-size: 13px;
  color: #9ca3af;
}

.loading-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px;
  color: #9ca3af;
  font-size: 14px;
}

.pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding-top: 20px;
  margin-top: 10px;
  border-top: 1px solid #e5e7eb;
}

.page-btn {
  padding: 6px 14px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}
</style>
