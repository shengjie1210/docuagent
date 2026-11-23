<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { marked } from 'marked'
import { User, Cpu } from '@element-plus/icons-vue'

const props = defineProps({
  messages: { type: Array, required: true },
  isStreaming: { type: Boolean, default: false }
})

const listRef = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, scrollToBottom)
watch(() => props.messages[props.messages.length - 1]?.content, scrollToBottom)

const formatSources = (sources) => {
  if (!sources || !sources.length) return []
  if (typeof sources === 'string') {
    try {
      return JSON.parse(sources)
    } catch (e) {
      return []
    }
  }
  return sources
}

/** Markdown 渲染：marked 输出后暴力压掉浏览器默认 margin */
const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    let html = marked.parse(text, { breaks: true })
    // 暴力清洗：去掉 p/ul/ol/h1-h6/blockquote 等元素的默认上下 margin
    html = html.replace(/<p>/g, '<p style="margin:0 0 8px 0;line-height:1.65;">')
    html = html.replace(/<\/p>/g, '</p>')
    html = html.replace(/<ul>/g, '<ul style="margin:4px 0 8px 0;padding-left:20px;">')
    html = html.replace(/<ol>/g, '<ol style="margin:4px 0 8px 0;padding-left:20px;">')
    html = html.replace(/<li>/g, '<li style="margin:2px 0;line-height:1.6;">')
    html = html.replace(/<h([1-6])>/g, '<h$1 style="margin:8px 0 4px 0;line-height:1.4;">')
    html = html.replace(/<blockquote>/g, '<blockquote style="margin:6px 0;padding:2px 10px;border-left:3px solid #c7d2fe;color:#6b7280;background:#f9fafb;">')
    return html
  } catch (e) {
    return text
  }
}

/** 判断是否为最后一条 assistant 消息且正在流式输出 */
const isLastStreaming = (idx) => {
  if (!props.isStreaming) return false
  const last = props.messages[props.messages.length - 1]
  return last && last.role === 'assistant' && idx === props.messages.length - 1
}
</script>

<template>
  <div class="message-list" ref="listRef">
    <div class="message-wrap">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="msg-row"
        :class="msg.role"
      >
        <div class="avatar" :class="msg.role">
          <el-icon :size="18">
            <User v-if="msg.role === 'user'" />
            <Cpu v-else />
          </el-icon>
        </div>
        <div class="msg-body">
          <div class="msg-bubble" :class="{ streaming: isLastStreaming(idx) }">
            <!-- 思考中动画：assistant 还没返回任何内容时 -->
            <div v-if="msg.role === 'assistant' && !msg.content && isLastStreaming(idx)" class="thinking">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="thinking-text">正在思考</span>
            </div>
            <!-- 正常内容 -->
            <div v-else class="msg-content" v-html="renderMarkdown(msg.content)"></div>
            <!-- 流式光标：正在输出时显示闪烁的竖线 -->
            <span v-if="isLastStreaming(idx) && msg.content" class="stream-cursor"></span>
          </div>
          <div v-if="formatSources(msg.sources).length > 0" class="sources">
            <div class="sources-title">
              <el-icon :size="12"><Document /></el-icon>
              引用来源
            </div>
            <div
              v-for="(src, si) in formatSources(msg.sources)"
              :key="si"
              class="source-card"
            >
              <div class="source-head">
                <el-icon :size="13" class="src-icon"><Document /></el-icon>
                <span class="src-name">{{ src.filename || '文档' }}</span>
              </div>
              <div v-if="src.content" class="src-snippet">{{ src.content }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* ===== 全局 Markdown 紧凑样式（不 scoped，确保 v-html 内容能命中） ===== */
.msg-content p,
.msg-content ul,
.msg-content ol,
.msg-content li,
.msg-content h1,
.msg-content h2,
.msg-content h3,
.msg-content h4,
.msg-content h5,
.msg-content h6,
.msg-content blockquote,
.msg-content pre,
.msg-content table,
.msg-content hr {
  margin-top: 0;
  margin-bottom: 0;
}

.msg-content p {
  margin-bottom: 8px;
  line-height: 1.65;
}

.msg-content ul,
.msg-content ol {
  margin: 4px 0 8px 0;
  padding-left: 20px;
}

.msg-content li {
  margin: 2px 0;
  line-height: 1.6;
}

.msg-content li p {
  margin: 0;
}

.msg-content h1, .msg-content h2, .msg-content h3, .msg-content h4 {
  margin: 8px 0 4px 0;
  line-height: 1.4;
}
.msg-content h1 { font-size: 17px; }
.msg-content h2 { font-size: 15px; }
.msg-content h3, .msg-content h4 { font-size: 14px; }

.msg-content blockquote {
  margin: 6px 0;
  padding: 2px 10px;
  border-left: 3px solid #c7d2fe;
  color: #6b7280;
  background: #f9fafb;
}

.msg-content code {
  background: #f3f4f6;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12.5px;
  font-family: Consolas, monospace;
}

.msg-content pre {
  background: #1f2937;
  color: #f9fafb;
  padding: 10px 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 6px 0;
}

.msg-content pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}

.msg-content table {
  border-collapse: collapse;
  margin: 6px 0;
  font-size: 12.5px;
}

.msg-content th, .msg-content td {
  border: 1px solid #e5e7eb;
  padding: 4px 8px;
  text-align: left;
}

.msg-content th {
  background: #f9fafb;
  font-weight: 600;
}

.msg-content hr {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 8px 0;
}

.msg-content strong {
  color: #111827;
  font-weight: 600;
}
</style>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 0;
}

/* 美化滚动条 */
.message-list::-webkit-scrollbar {
  width: 6px;
}
.message-list::-webkit-scrollbar-track {
  background: transparent;
}
.message-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}
.message-list::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

.message-wrap {
  max-width: 820px;
  margin: 0 auto;
  padding: 0 24px;
}

.msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  animation: fadeInUp 0.3s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.avatar.assistant {
  background: #f3f4f6;
  color: #667eea;
  border: 1px solid #e5e7eb;
}

.msg-body {
  max-width: calc(100% - 60px);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.msg-row.user .msg-body {
  align-items: flex-end;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13.5px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.msg-bubble.streaming {
  border-color: #c7d2fe;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.08);
}

.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.msg-row.assistant .msg-bubble {
  background: white;
  color: #1f2937;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.msg-content {
  min-height: 1.2em;
}

/* ===== Markdown 紧凑样式（覆盖 marked 默认大 margin） ===== */
.msg-content :deep(p) {
  margin: 0 0 10px 0;
  line-height: 1.75;
}
.msg-content :deep(p:last-child) {
  margin-bottom: 0;
}
.msg-content :deep(ul),
.msg-content :deep(ol) {
  margin: 6px 0 10px 0;
  padding-left: 22px;
}
.msg-content :deep(li) {
  margin: 3px 0;
  line-height: 1.7;
}
.msg-content :deep(li > p) {
  margin: 0;
}
.msg-content :deep(h1),
.msg-content :deep(h2),
.msg-content :deep(h3),
.msg-content :deep(h4) {
  margin: 12px 0 6px 0;
  line-height: 1.4;
}
.msg-content :deep(h1) { font-size: 18px; }
.msg-content :deep(h2) { font-size: 16px; }
.msg-content :deep(h3) { font-size: 15px; }
.msg-content :deep(h4) { font-size: 14px; }
.msg-content :deep(blockquote) {
  margin: 6px 0;
  padding: 2px 12px;
  border-left: 3px solid #c7d2fe;
  color: #6b7280;
  background: #f9fafb;
}
.msg-content :deep(code) {
  background: #f3f4f6;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Consolas', monospace;
}
.msg-content :deep(pre) {
  background: #1f2937;
  color: #f9fafb;
  padding: 12px 14px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}
.msg-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
.msg-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 13px;
  width: 100%;
}
.msg-content :deep(th),
.msg-content :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 6px 10px;
  text-align: left;
}
.msg-content :deep(th) {
  background: #f9fafb;
  font-weight: 600;
}
.msg-content :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 10px 0;
}
.msg-content :deep(strong) {
  color: #111827;
  font-weight: 600;
}

/* 流式输出光标 */
.stream-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: #667eea;
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 思考中动画 */
.thinking {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 20px;
}

.thinking .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c7d2fe;
  animation: bounce 1.2s infinite;
}

.thinking .dot:nth-child(2) { animation-delay: 0.2s; }
.thinking .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.thinking-text {
  margin-left: 6px;
  font-size: 13px;
  color: #9ca3af;
}

.sources {
  width: 100%;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
}

.sources-title {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.source-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 10px;
  margin-bottom: 6px;
}

.source-card:last-child {
  margin-bottom: 0;
}

.source-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
}

.src-icon {
  color: #667eea;
}

.src-snippet {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>