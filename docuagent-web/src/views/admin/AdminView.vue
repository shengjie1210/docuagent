<script setup>
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'

const router = useRouter()
const route = useRoute()

const tabs = [
  { name: 'providers', label: '供应商管理', path: '/admin/providers' },
  { name: 'rag-config', label: '知识库配置', path: '/admin/rag-config' },
  { name: 'monitor', label: '系统监控', path: '/admin/monitor' }
]

const activeTab = computed(() => {
  if (route.path.includes('rag-config')) return 'rag-config'
  if (route.path.includes('monitor')) return 'monitor'
  return 'providers'
})

const switchTab = (name) => {
  const tab = tabs.find(t => t.name === name)
  if (tab) router.push(tab.path)
}
</script>

<template>
  <div class="admin-page">
    <div class="admin-header">
      <div>
        <h2>管理后台</h2>
        <p>系统配置与运行状态总览</p>
      </div>
      <el-tag type="warning" effect="dark" size="small">管理员</el-tag>
    </div>

    <div class="admin-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.name"
        class="admin-tab"
        :class="{ active: activeTab === tab.name }"
        @click="switchTab(tab.name)">
        {{ tab.label }}
      </div>
    </div>

    <div class="admin-body">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f5f7fa;
}
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px 14px;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
}
.admin-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #1f2937;
}
.admin-header p {
  margin: 0;
  font-size: 13px;
  color: #9ca3af;
}
.admin-tabs {
  display: flex;
  gap: 4px;
  padding: 0 28px;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
}
.admin-tab {
  padding: 12px 20px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.15s;
}
.admin-tab:hover {
  color: #667eea;
}
.admin-tab.active {
  color: #667eea;
  font-weight: 600;
  border-bottom-color: #667eea;
}
.admin-body {
  flex: 1;
  overflow: auto;
  padding: 24px 28px;
}
</style>
