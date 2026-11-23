<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const mode = ref('login')
const form = ref({ username: '', password: '' })
const loading = ref(false)

const switchMode = () => {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}

const handleSubmit = async () => {
  if (!form.value.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!form.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    const api = mode.value === 'login' ? authApi.login : authApi.register
    const res = await api(form.value)
    if (res && res.token) {
      // 用 Pinia 统一写入，isAdmin 成为响应式状态（管理后台入口据此显示）
      userStore.setToken(res.token)
      userStore.setUserInfo(res.username, res.isAdmin)
      ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
      router.push('/')
    }
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="bg-decoration">
      <div class="circle c1"></div>
      <div class="circle c2"></div>
      <div class="circle c3"></div>
    </div>
    <div class="login-card">
      <div class="brand">
        <div class="logo-icon">
          <el-icon :size="42" color="#667eea"><Document /></el-icon>
        </div>
        <h1>DocuAgent</h1>
        <p class="subtitle">智能文档管理助手</p>
      </div>

      <div class="tabs">
        <div
          class="tab"
          :class="{ active: mode === 'login' }"
          @click="mode = 'login'"
        >登录</div>
        <div
          class="tab"
          :class="{ active: mode === 'register' }"
          @click="mode = 'register'"
        >注册</div>
      </div>

      <div class="form-group">
        <div class="input-with-icon">
          <el-icon class="input-icon" :size="18" color="#9ca3af"><User /></el-icon>
          <input
            v-model="form.username"
            type="text"
            placeholder="用户名"
            class="form-input"
            @keyup.enter="handleSubmit"
          />
        </div>
      </div>

      <div class="form-group">
        <div class="input-with-icon">
          <el-icon class="input-icon" :size="18" color="#9ca3af"><Lock /></el-icon>
          <input
            v-model="form.password"
            type="password"
            placeholder="密码"
            class="form-input"
            @keyup.enter="handleSubmit"
          />
        </div>
      </div>

      <button class="submit-btn" :disabled="loading" @click="handleSubmit">
        {{ loading ? '请稍候...' : (mode === 'login' ? '登 录' : '注 册') }}
      </button>

      <div class="footer-hint">
        <span v-if="mode === 'login'">还没有账号？
          <a class="link" @click="switchMode">立即注册</a>
        </span>
        <span v-else>已有账号？
          <a class="link" @click="switchMode">去登录</a>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.c1 { width: 300px; height: 300px; top: -100px; left: -100px; }
.c2 { width: 200px; height: 200px; bottom: -50px; right: -50px; }
.c3 { width: 150px; height: 150px; top: 40%; right: 20%; }

.login-card {
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
}

.brand {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand h1 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.5px;
}

.subtitle {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.tabs {
  display: flex;
  margin-bottom: 28px;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 4px;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  border-radius: 8px;
  transition: all 0.2s;
}

.tab.active {
  background: white;
  color: #111827;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.form-group {
  margin-bottom: 16px;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  z-index: 1;
}

.form-input {
  width: 100%;
  height: 46px;
  padding: 0 16px 0 42px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.2s;
  background: #fafafa;
}

.form-input:focus {
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input::placeholder {
  color: #9ca3af;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 2px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.footer-hint {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #6b7280;
}

.link {
  color: #667eea;
  cursor: pointer;
  font-weight: 500;
}

.link:hover {
  text-decoration: underline;
}
</style>
