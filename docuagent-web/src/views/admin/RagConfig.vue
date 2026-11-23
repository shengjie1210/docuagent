<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const cfg = ref({
  chunkSize: 800,
  topK: 4,
  similarityThreshold: 0.55,
  maxChunksPerDoc: 2,
  vectorFileBytes: 0,
  totalChunks: 0,
  documentCount: 0
})

const vectorSizeText = computed(() => {
  const b = cfg.value.vectorFileBytes || 0
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
})

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.getRagConfig()
    if (res) Object.assign(cfg.value, res)
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const save = async () => {
  saving.value = true
  try {
    await adminApi.updateRagConfig({
      chunkSize: cfg.value.chunkSize,
      topK: cfg.value.topK,
      similarityThreshold: cfg.value.similarityThreshold,
      maxChunksPerDoc: cfg.value.maxChunksPerDoc
    })
    ElMessage.success('参数已热更新（重启后恢复为配置文件中的值）')
    await ElMessageBox.alert(
      'chunk_size / 切分规则只影响新上传的文档。若要让已有文档按新规则生效，请到「文档管理」对已有文档重新索引。',
      '需要重新索引已有文档才能生效',
      { confirmButtonText: '知道了', type: 'warning' }
    )
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="rag-layout" v-loading="loading">
    <div class="card config-card">
      <div class="card-header">
        <div>
          <h3>检索与切分参数</h3>
          <p>修改后立即在内存中生效，无需重启</p>
        </div>
      </div>

      <el-form label-width="150px" class="cfg-form">
        <el-form-item label="chunk_size">
          <div class="slider-row">
            <el-slider v-model="cfg.chunkSize" :min="200" :max="2000" :step="50" style="flex: 1" />
            <el-input-number v-model="cfg.chunkSize" :min="100" :max="5000" :step="50" controls-position="right" style="width: 110px; margin-left: 16px" />
          </div>
          <div class="hint">每个文本块最大字符数，越大则上下文越完整但检索粒度变粗</div>
        </el-form-item>

        <el-form-item label="top_k">
          <el-input-number v-model="cfg.topK" :min="1" :max="10" controls-position="right" />
          <span class="unit">最终返回给模型的文档片段数量</span>
        </el-form-item>

        <el-form-item label="similarity_threshold">
          <div class="slider-row">
            <el-slider v-model="cfg.similarityThreshold" :min="0.1" :max="0.9" :step="0.05" style="flex: 1" />
            <span class="threshold-val">{{ cfg.similarityThreshold.toFixed(2) }}</span>
          </div>
          <div class="hint">相似度低于此值的片段直接丢弃，越高越严格</div>
        </el-form-item>

        <el-form-item label="max_chunks_per_doc">
          <el-input-number v-model="cfg.maxChunksPerDoc" :min="1" :max="5" controls-position="right" />
          <span class="unit">同一文档最多保留的片段数，避免单篇刷屏</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card stats-card">
      <div class="card-header"><div><h3>知识库概况</h3><p>当前向量库与文档统计</p></div></div>
      <div class="stat-item">
        <span class="stat-label">vectors.json 大小</span>
        <span class="stat-value">{{ vectorSizeText }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">chunk 总数</span>
        <span class="stat-value">{{ cfg.totalChunks }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">文档总数</span>
        <span class="stat-value">{{ cfg.documentCount }}</span>
      </div>
      <el-alert type="info" :closable="false" class="stats-tip">
        修改切分规则后，已有文档需重新索引才能按新 chunk_size 生效。
      </el-alert>
    </div>
  </div>
</template>

<style scoped>
.rag-layout { display: grid; grid-template-columns: 1.6fr 1fr; gap: 20px; align-items: start; }
.card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.card-header { margin-bottom: 20px; }
.card-header h3 { margin: 0 0 4px; font-size: 16px; color: #1f2937; }
.card-header p { margin: 0; font-size: 12px; color: #9ca3af; }
.cfg-form { max-width: 640px; }
.slider-row { display: flex; align-items: center; width: 100%; }
.threshold-val { width: 46px; text-align: right; font-weight: 600; color: #667eea; }
.unit, .hint { font-size: 12px; color: #9ca3af; margin-left: 12px; }
.hint { margin-left: 0; margin-top: 4px; line-height: 1.5; }
.stat-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #f0f2f5; }
.stat-label { color: #6b7280; font-size: 14px; }
.stat-value { font-size: 18px; font-weight: 600; color: #1f2937; }
.stats-tip { margin-top: 20px; }
@media (max-width: 900px) { .rag-layout { grid-template-columns: 1fr; } }
</style>
