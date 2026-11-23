<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

// echarts 按需引入（不整包 import，减小体积）
import * as echarts from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([PieChart, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const info = ref({
  heapUsedMb: 0, heapMaxMb: 0, heapUsedPercent: 0,
  nonHeapUsedMb: 0, nonHeapMaxMb: 0, cpuUsagePercent: -1, threadCount: 0,
  startTimeText: '-', uptimeText: '-',
  dbActiveConnections: 0, dbIdleConnections: 0, dbTotalConnections: 0, dbMaxConnections: 0,
  documentCount: 0, myDocumentCount: 0, conversationCount: 0, messageCount: 0,
  serverTime: '-', appVersion: '-', javaVersion: '-', osName: '-'
})

const pieRef = ref(null)
const lineRef = ref(null)
let pieChart = null
let lineChart = null
const trend = ref([]) // { time, used }
let timer = null

const cpuText = computed(() => info.value.cpuUsagePercent >= 0 ? info.value.cpuUsagePercent + '%' : 'N/A')

const initCharts = () => {
  if (pieRef.value) pieChart = echarts.init(pieRef.value)
  if (lineRef.value) lineChart = echarts.init(lineRef.value)
  renderPie()
  renderLine()
}

const renderPie = () => {
  if (!pieChart) return
  const used = info.value.heapUsedMb || 0
  const free = Math.max((info.value.heapMaxMb || 0) - used, 0)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} MB ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#6b7280' } },
    series: [{
      type: 'pie', radius: ['55%', '75%'], center: ['50%', '45%'], avoidLabelOverlap: false,
      label: { show: true, formatter: '{d}%', color: '#374151' },
      data: [
        { value: used, name: '已用堆内存', itemStyle: { color: '#667eea' } },
        { value: free, name: '空闲堆内存', itemStyle: { color: '#e5e7eb' } }
      ]
    }]
  }, true)
}

const renderLine = () => {
  if (!lineChart) return
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 45, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: trend.value.map(p => p.time), axisLabel: { color: '#9ca3af', fontSize: 11 } },
    yAxis: { type: 'value', name: 'MB', axisLabel: { color: '#9ca3af' }, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [{
      type: 'line', smooth: true, showSymbol: false,
      data: trend.value.map(p => p.used),
      lineStyle: { color: '#667eea', width: 2 },
      areaStyle: { color: 'rgba(102,126,234,0.15)' }
    }]
  }, true)
}

const fetchInfo = async (record = false) => {
  try {
    const res = await adminApi.systemInfo()
    if (!res) return
    info.value = res
    if (record) {
      trend.value.push({ time: (res.serverTime || '').slice(11) || new Date().toLocaleTimeString(), used: res.heapUsedMb })
      if (trend.value.length > 50) trend.value.shift()
    }
    renderPie()
    renderLine()
  } catch (e) {}
}

// ---- 日志查看器 ----
const logType = ref('info')
const logLines = ref(100)
const logs = ref([])
const logLoading = ref(false)
const logBox = ref(null)
const fetchLogs = async () => {
  logLoading.value = true
  try {
    const res = logType.value === 'error'
      ? await adminApi.systemErrorLogs(logLines.value)
      : await adminApi.systemLogs(logLines.value)
    // 后端返回正常顺序（旧->新），这里倒序显示（最新在最上）
    logs.value = (res || []).slice().reverse()
    await nextTick()
    if (logBox.value) logBox.value.scrollTop = 0
  } catch (e) {
  } finally {
    logLoading.value = false
  }
}

const handleResize = () => { pieChart?.resize(); lineChart?.resize() }

onMounted(async () => {
  await nextTick()
  initCharts()
  await fetchInfo(true)
  await fetchLogs()
  timer = setInterval(() => fetchInfo(true), 5000)
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  lineChart?.dispose()
})

const logText = computed(() => logs.value.join('\n'))
</script>

<template>
  <div class="monitor">
    <!-- 数字卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background:#eef2ff;color:#667eea">⏱</div>
        <div><div class="stat-label">运行时长</div><div class="stat-value">{{ info.uptimeText }}</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#ecfdf5;color:#10b981">📄</div>
        <div>
          <div class="stat-label">文档总数（全站）</div>
          <div class="stat-value">{{ info.documentCount }}</div>
          <div class="stat-sub">其中我的 {{ info.myDocumentCount }} 份</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fff7ed;color:#f59e0b">💬</div>
        <div><div class="stat-label">对话总数</div><div class="stat-value">{{ info.conversationCount }}</div></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fdf2f8;color:#ec4899">✉️</div>
        <div><div class="stat-label">消息总数</div><div class="stat-value">{{ info.messageCount }}</div></div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <div class="card pie-card">
        <h3>堆内存使用率</h3>
        <div ref="pieRef" class="chart"></div>
        <div class="chart-foot">已用 {{ info.heapUsedMb }} MB / 最大 {{ info.heapMaxMb }} MB</div>
      </div>
      <div class="card line-card">
        <h3>内存使用趋势 <span class="sub">（每 5 秒刷新 · 最近 50 点）</span></h3>
        <div ref="lineRef" class="chart"></div>
      </div>
    </div>

    <!-- 详情 + 健康检查 -->
    <div class="card detail-card">
      <h3>运行详情</h3>
      <div class="detail-grid">
        <div><span>CPU 使用率</span><b>{{ cpuText }}</b></div>
        <div><span>线程数</span><b>{{ info.threadCount }}</b></div>
        <div><span>非堆内存</span><b>{{ info.nonHeapUsedMb }} MB</b></div>
        <div><span>DB 活跃连接</span><b>{{ info.dbActiveConnections }} / {{ info.dbMaxConnections }}</b></div>
        <div><span>DB 空闲连接</span><b>{{ info.dbIdleConnections }}</b></div>
        <div><span>启动时间</span><b>{{ info.startTimeText }}</b></div>
        <div><span>当前时间</span><b>{{ info.serverTime }}</b></div>
        <div><span>后端版本</span><b>{{ info.appVersion }}</b></div>
        <div><span>Java 版本</span><b>{{ info.javaVersion }}</b></div>
        <div><span>操作系统</span><b>{{ info.osName }}</b></div>
      </div>
    </div>

    <!-- 日志查看器 -->
    <div class="card log-card">
      <div class="log-header">
        <h3>日志查看器</h3>
        <div class="log-controls">
          <el-select v-model="logType" size="small" style="width: 110px" @change="fetchLogs">
            <el-option label="info.log" value="info" />
            <el-option label="error.log" value="error" />
          </el-select>
          <el-select v-model="logLines" size="small" style="width: 100px" @change="fetchLogs">
            <el-option :value="50" label="50 行" />
            <el-option :value="100" label="100 行" />
            <el-option :value="200" label="200 行" />
            <el-option :value="500" label="500 行" />
          </el-select>
          <el-button size="small" :loading="logLoading" @click="fetchLogs">刷新</el-button>
        </div>
      </div>
      <div ref="logBox" class="log-box" v-loading="logLoading">
        <pre>{{ logText || '（暂无日志）' }}</pre>
      </div>
    </div>
  </div>
</template>

<style scoped>
.monitor { display: flex; flex-direction: column; gap: 20px; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { display: flex; align-items: center; gap: 14px; background: #fff; border-radius: 12px; padding: 18px 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.stat-label { font-size: 12px; color: #9ca3af; margin-bottom: 4px; }
.stat-value { font-size: 18px; font-weight: 600; color: #1f2937; }
.stat-sub { font-size: 11px; color: #6b7280; margin-top: 2px; }
.card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.card h3 { margin: 0 0 16px; font-size: 15px; color: #1f2937; }
.sub { font-size: 12px; font-weight: 400; color: #9ca3af; }
.chart-row { display: grid; grid-template-columns: 340px 1fr; gap: 20px; }
.chart { height: 240px; width: 100%; }
.chart-foot { text-align: center; font-size: 12px; color: #9ca3af; margin-top: 4px; }
.detail-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px 20px; }
.detail-grid > div { display: flex; flex-direction: column; gap: 4px; }
.detail-grid span { font-size: 12px; color: #9ca3af; }
.detail-grid b { font-size: 14px; color: #374151; font-weight: 600; }
.log-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.log-header h3 { margin: 0; }
.log-controls { display: flex; gap: 8px; }
.log-box { max-height: 320px; overflow: auto; background: #1e1e2d; border-radius: 8px; padding: 14px; }
.log-box pre { margin: 0; color: #c8d3f5; font-size: 12px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; font-family: Consolas, Monaco, monospace; }
@media (max-width: 1100px) { .stat-cards { grid-template-columns: repeat(2, 1fr); } .chart-row { grid-template-columns: 1fr; } .detail-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
