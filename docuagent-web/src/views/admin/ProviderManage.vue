<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const list = ref([])

const load = async () => {
  loading.value = true
  try {
    list.value = await adminApi.listProviders() || []
  } catch (e) {
    // 错误已由 api 拦截器提示
  } finally {
    loading.value = false
  }
}

// ---- 编辑弹窗 ----
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return { name: '', provider: 'dashscope', baseUrl: '', apiKey: '', chatModel: '', embedModel: '' }
}

const openAdd = () => {
  editingId.value = null
  form.value = emptyForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  form.value = {
    name: row.name,
    provider: row.provider,
    baseUrl: row.baseUrl,
    apiKey: '', // 留空 = 不修改（绝不回显明文）
    chatModel: row.chatModel,
    embedModel: row.embedModel || ''
  }
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.value.name || !form.value.baseUrl || !form.value.chatModel) {
    ElMessage.warning('请填写名称、API 地址和对话模型')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await adminApi.updateProvider(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      if (!form.value.apiKey) {
        ElMessage.warning('新增时请填写 API Key')
        saving.value = false
        return
      }
      await adminApi.saveProvider(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

// ---- 操作 ----
const enable = async (row) => {
  try {
    await adminApi.enableProvider(row.id)
    ElMessage.success('已切换为默认启用')
    await load()
  } catch (e) {}
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
    await adminApi.deleteProvider(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {}
  }
}

const testingId = ref(null)
const test = async (row) => {
  testingId.value = row.id
  try {
    const res = await adminApi.testProvider(row.id)
    if (res && res.success) {
      ElMessage.success(`连通正常 · 延迟 ${res.latencyMs}ms`)
    } else {
      ElMessage.error(res?.message || '连通失败')
    }
  } catch (e) {
  } finally {
    testingId.value = null
  }
}

const balance = async (row) => {
  try {
    const res = await adminApi.providerBalance(row.id)
    ElMessageBox.alert(
      `${res.message}\n延迟：${res.latencyMs}ms\n\n${res.note || ''}`,
      '额度 / 连通性',
      { confirmButtonText: '知道了', type: res.success ? 'success' : 'warning' }
    )
  } catch (e) {}
}

onMounted(load)
</script>

<template>
  <div class="card">
    <div class="card-header">
      <div>
        <h3>供应商配置</h3>
        <p>管理 AI 服务商连接参数，API Key 全程脱敏存储与展示</p>
      </div>
      <el-button type="primary" @click="openAdd">新增供应商</el-button>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    >
      <template #title>
        系统在用的配置来自这里：标记「启用」的那条即为当前生效供应商（首次启动已从 application.yml 自动导入）。修改 api_key / base_url / 模型后需<strong>重启后端</strong>才生效；更换 embed_model 后还需<strong>重新索引已有文档</strong>，否则旧向量与新模型不兼容。
      </template>
    </el-alert>

    <el-table :data="list" v-loading="loading" border style="width: 100%" size="default">
      <el-table-column prop="name" label="名称" min-width="130" />
      <el-table-column prop="provider" label="类型" width="110">
        <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.provider }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="baseUrl" label="base_url" min-width="220" show-overflow-tooltip />
      <el-table-column prop="chatModel" label="chat_model" width="130" show-overflow-tooltip />
      <el-table-column prop="embedModel" label="embed_model" width="140" show-overflow-tooltip />
      <el-table-column prop="apiKey" label="api_key" width="150">
        <template #default="{ row }"><code class="masked-key">{{ row.apiKey }}</code></template>
      </el-table-column>
      <el-table-column label="默认" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="success" size="small">启用</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="所属用户" width="100" />
      <el-table-column label="操作" width="290" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" size="small" :disabled="row.isDefault === 1" @click="enable(row)">启用</el-button>
          <el-button link type="info" size="small" :loading="testingId === row.id" @click="test(row)">测试</el-button>
          <el-button link type="warning" size="small" @click="balance(row)">额度</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><div class="empty">暂无配置，点击右上角新增</div></template>
    </el-table>

    <!-- 编辑 / 新增弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑供应商' : '新增供应商'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="配置名称"><el-input v-model="form.name" placeholder="如：通义千问" /></el-form-item>
        <el-form-item label="服务商类型">
          <el-select v-model="form.provider" style="width: 100%">
            <el-option label="dashscope（阿里云百炼）" value="dashscope" />
            <el-option label="openai" value="openai" />
            <el-option label="ollama" value="ollama" />
            <el-option label="other" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="base_url"><el-input v-model="form.baseUrl" placeholder="https://.../compatible-mode" /></el-form-item>
        <el-form-item label="chat_model"><el-input v-model="form.chatModel" placeholder="qwen-plus" /></el-form-item>
        <el-form-item label="embed_model"><el-input v-model="form.embedModel" placeholder="text-embedding-v3" /></el-form-item>
        <el-form-item label="api_key">
          <el-input v-model="form.apiKey" type="password" show-password
            :placeholder="editingId ? '留空表示不修改（原 Key 不会回显）' : '请输入 API Key'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.card-header h3 { margin: 0 0 4px; font-size: 16px; color: #1f2937; }
.card-header p { margin: 0; font-size: 12px; color: #9ca3af; }
.masked-key { font-size: 12px; color: #6b7280; background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
.empty { padding: 30px; color: #9ca3af; font-size: 13px; }
</style>
