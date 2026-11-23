<script setup>
import { ref } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['send-message'])
const inputValue = ref('')
const textareaRef = ref(null)

const autoResize = () => {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 160) + 'px'
  }
}

const handleSend = () => {
  const text = inputValue.value.trim()
  if (text && !props.disabled) {
    emit('send-message', text)
    inputValue.value = ''
    autoResize()
  }
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="chat-input-area">
    <div class="input-box">
      <textarea
        ref="textareaRef"
        v-model="inputValue"
        :placeholder="disabled ? 'AI 正在思考...' : '输入你的问题，Enter 发送，Shift+Enter 换行'"
        class="textarea"
        :disabled="disabled"
        rows="1"
        @input="autoResize"
        @keydown="handleKeydown"
      />
      <button
        class="send-btn"
        :class="{ active: inputValue.trim() && !disabled }"
        :disabled="!inputValue.trim() || disabled"
        @click="handleSend"
        title="发送"
      >
        <el-icon :size="16">
          <Promotion v-if="disabled" />
          <Promotion v-else />
        </el-icon>
      </button>
    </div>
    <div class="input-tip">DocuAgent 可能会出错，请核实重要信息</div>
  </div>
</template>

<style scoped>
.chat-input-area {
  padding: 16px 24px 20px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}

.input-box {
  max-width: 820px;
  margin: 0 auto;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: white;
  border: 1.5px solid #e5e7eb;
  border-radius: 12px;
  padding: 8px 8px 8px 14px;
  transition: all 0.2s;
}

.input-box:focus-within {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  padding: 6px 0;
  max-height: 160px;
  overflow-y: auto;
  background: transparent;
}

.textarea:disabled {
  cursor: not-allowed;
  color: #9ca3af;
}

.textarea::placeholder {
  color: #9ca3af;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #e5e7eb;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  cursor: pointer;
}

.send-btn.active:hover {
  transform: scale(1.05);
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.8;
}

.input-tip {
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}
</style>
