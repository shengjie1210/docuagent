<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, DataLine, DataBoard } from '@element-plus/icons-vue'
import MessageList from '@/components/chat/MessageList.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import { conversationApi, fetchChatStream } from '@/api'

const route = useRoute()
const emit = defineEmits(['conv-created'])

const messages = ref([])
const currentConvId = ref(null)
const isStreaming = ref(false)

const initConversation = async (convId) => {
  currentConvId.value = convId ? Number(convId) : null
  messages.value = []
  if (!convId) return
  try {
    const res = await conversationApi.messages(convId)
    messages.value = res || []
  } catch (err) {
    console.error('加载消息失败', err)
  }
}

const handleSend = async (content) => {
  if (!content || !content.trim() || isStreaming.value) return
  const text = content.trim()

  if (!currentConvId.value) {
    try {
      const res = await conversationApi.create({})
      if (res && res.id) {
        currentConvId.value = res.id
        emit('conv-created', res.id)
      } else {
        ElMessage.error('创建对话失败')
        return
      }
    } catch (err) {
      ElMessage.error('创建对话失败')
      return
    }
  }

  messages.value.push({ role: 'user', content: text, sources: [] })

  const assistantMsg = reactive({ role: 'assistant', content: '', sources: [] })
  messages.value.push(assistantMsg)

  isStreaming.value = true

  try {
    await fetchChatStream(
      currentConvId.value,
      text,
      (data) => {
        if (data.content) {
          assistantMsg.content += data.content
        }
        if (data.sources) {
          assistantMsg.sources = data.sources
        }
      },
      () => {
        isStreaming.value = false
        if (!assistantMsg.content) {
          assistantMsg.content = '（回答结束）'
        }
      },
      (err) => {
        isStreaming.value = false
        console.error('流式请求失败', err)
        const errMsg = err?.message || '请求失败'
        if (!assistantMsg.content) {
          assistantMsg.content = '抱歉，' + errMsg + '，请稍后重试。'
        } else {
          ElMessage.error(errMsg)
        }
      }
    )
  } catch (e) {
    isStreaming.value = false
    console.error('Unexpected error in chat stream', e)
    ElMessage.error('意外错误，请刷新重试')
  }
}

const quickQuestions = [
  { icon: 'Document', title: '总结文档', desc: '快速了解文档内容概要', q: '请总结一下已上传文档的主要内容' },
  { icon: 'DataLine', title: '提取要点', desc: '获取文档核心观点', q: '这份文档的主要观点是什么？' },
  { icon: 'DataBoard', title: '查找数据', desc: '定位文档中的重要数据', q: '文档中有哪些关键数据和指标？' }
]

onMounted(() => {
  const convId = route.query.convId
  if (convId) {
    initConversation(convId)
  }
})

watch(() => route.query.convId, (newId) => {
  const id = newId ? Number(newId) : null
  if (id !== currentConvId.value) {
    initConversation(newId)
  }
})
</script>

<template>
  <div class="chat-page">
    <div v-if="messages.length === 0" class="welcome">
      <div class="welcome-icon">
        <el-icon :size="52" color="#667eea"><Document /></el-icon>
      </div>
      <h1>你好！</h1>
      <p class="welcome-desc">我是你的文档智能助手，可以帮你解答文档相关的问题。</p>
      <div class="quick-tips">
        <div
          class="tip-card"
          v-for="(item, i) in quickQuestions"
          :key="i"
          @click="handleSend(item.q)"
        >
          <el-icon class="tip-icon" :size="22" color="#667eea">
            <component :is="item.icon" />
          </el-icon>
          <div>
            <div class="tip-title">{{ item.title }}</div>
            <div class="tip-desc">{{ item.desc }}</div>
          </div>
        </div>
      </div>
    </div>

    <MessageList v-else :messages="messages" :is-streaming="isStreaming" />

    <ChatInput @send-message="handleSend" :disabled="isStreaming" />
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f9fafb;
}

.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.welcome-icon {
  margin-bottom: 16px;
  opacity: 0.9;
}

.welcome h1 {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.5px;
}

.welcome-desc {
  margin: 0 0 40px;
  font-size: 15px;
  color: #6b7280;
}

.quick-tips {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  max-width: 640px;
  width: 100%;
}

.tip-card {
  background: white;
  border: 1.5px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  transition: all 0.2s;
}

.tip-card:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.18);
  transform: translateY(-3px);
}

.tip-card:hover .tip-icon {
  transform: scale(1.15);
}

.tip-icon {
  transition: transform 0.2s ease;
}

.tip-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.tip-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.tip-desc {
  font-size: 12px;
  color: #6b7280;
}

@media (max-width: 600px) {
  .quick-tips {
    grid-template-columns: 1fr;
  }
}
</style>
