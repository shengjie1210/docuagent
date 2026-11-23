<script setup>
  import { useRouter, useRoute } from "vue-router";
  import { onMounted, computed, ref, watch } from "vue";
  import { ElMessage, ElMessageBox } from "element-plus";
  import {
    ChatDotRound,
    Plus,
    User,
    Document,
    SwitchButton,
    Delete,
    Setting,
    Connection,
    Tools,
    DataLine,
    ArrowDown,
  } from "@element-plus/icons-vue";
  import { conversationApi, adminApi } from "@/api";
  import { useUserStore } from "@/stores/user";

  const router = useRouter();
  const route = useRoute();
  const userStore = useUserStore();
  const isLoggedIn = ref(!!localStorage.getItem("token"));

// 监听 storage 事件（多标签页同步登录/退出/权限变化）
window.addEventListener("storage", (e) => {
  userStore.syncFromStorage();
  if (e.key === "token") {
    isLoggedIn.value = !!localStorage.getItem("token");
  }
  if (e.key === "adminToken") {
    adminUnlocked.value = !!localStorage.getItem("adminToken");
  }
});
  const username = computed(() => userStore.username || localStorage.getItem("username") || "");
  // 管理后台解锁状态（共享密码验证通过后写入 adminToken），所有用户可见入口
  const adminUnlocked = ref(!!localStorage.getItem("adminToken"));

  const activeMenu = ref("chat");
  // 管理后台二级菜单是否展开
  const adminSubmenuOpen = ref(false);
  const conversations = ref([]);
  const currentConvId = ref(null);

  const loadConversations = async () => {
    try {
      const res = await conversationApi.list();
      conversations.value = res || [];
    } catch (err) {
      console.error("加载对话列表失败", err);
    }
  };

  const handleLogout = () => {
    ElMessageBox.confirm("确定要退出登录吗？", "提示", {
      confirmButtonText: "退出",
      cancelButtonText: "取消",
      type: "warning",
    })
      .then(() => {
        userStore.clearToken();
        localStorage.removeItem("adminToken");
        adminUnlocked.value = false;
        isLoggedIn.value = false;
        router.push("/login");
      })
      .catch(() => {});
  };

  // 共享管理员密码解锁：未解锁时弹密码框，验证通过后写入解锁 token
  const unlockAdmin = async () => {
    try {
      const { value } = await ElMessageBox.prompt(
        "请输入管理后台密码",
        "管理后台",
        {
          confirmButtonText: "进入",
          cancelButtonText: "取消",
          inputType: "password",
          inputPlaceholder: "请输入共享管理员密码",
          inputValidator: (v) => (v && v.length > 0 ? true : "密码不能为空"),
        }
      );
      const token = await adminApi.unlock(value);
      if (token) {
        localStorage.setItem("adminToken", token);
        adminUnlocked.value = true;
        return true;
      }
      return false;
    } catch (err) {
      // 密码错误（403）或用户取消，均不进入
      return false;
    }
  };

  const switchMenu = async (menu) => {
    if (menu === "chat") {
      activeMenu.value = menu;
      router.push("/");
    } else if (menu === "documents") {
      activeMenu.value = menu;
      router.push("/documents");
    } else if (menu === "admin") {
      // 所有用户都能点，先验解锁 token
      if (!adminUnlocked.value) {
        const ok = await unlockAdmin();
        if (!ok) return;
      }
      activeMenu.value = "admin";
      adminSubmenuOpen.value = true;
      router.push("/admin/providers");
    }
  };

  const gotoAdmin = (name) => {
    activeMenu.value = "admin";
    router.push("/admin/" + name);
  };

  const newChat = async () => {
    try {
      const res = await conversationApi.create({});
      if (res && res.id) {
        currentConvId.value = res.id;
        loadConversations();
        router.push("/?convId=" + res.id);
      }
    } catch (err) {
      console.error("新建对话失败", err);
    }
  };

  const switchConversation = (conv) => {
    currentConvId.value = conv.id;
    router.push("/?convId=" + conv.id);
  };

  const deleteConversation = async (e, conv) => {
    e.stopPropagation();
    try {
      await ElMessageBox.confirm("确定删除这个对话吗？", "提示", {
        confirmButtonText: "删除",
        cancelButtonText: "取消",
        type: "warning",
      });
      await conversationApi.delete(conv.id);
      ElMessage.success("删除成功");
      if (currentConvId.value === conv.id) {
        currentConvId.value = null;
        router.push("/");
      }
      await loadConversations();
    } catch (err) {
      if (err !== "cancel") {
        console.error(err);
        ElMessage.error("删除失败");
      }
    }
  };

  const handleConvCreated = (id) => {
    currentConvId.value = id;
    loadConversations();
  };

  const syncActiveMenu = (p) => {
    if (p.startsWith("/admin")) {
      activeMenu.value = "admin";
      adminSubmenuOpen.value = true;
    } else if (p === "/documents") {
      activeMenu.value = "documents";
    } else {
      activeMenu.value = "chat";
    }
  };

  onMounted(() => {
    userStore.syncFromStorage();
    adminUnlocked.value = !!localStorage.getItem("adminToken");
    if (!isLoggedIn.value && route.path !== "/login") {
      router.push("/login");
      return;
    }
    if (isLoggedIn.value) {
      syncActiveMenu(route.path);
      loadConversations();
    }
  });

  watch(
    () => route.path,
    (p) => {
      // 每次路由变化同步响应式状态，登录/登出/权限变化后 layout 才能正确切换
      isLoggedIn.value = !!localStorage.getItem("token");
      adminUnlocked.value = !!localStorage.getItem("adminToken");
      userStore.syncFromStorage();
      syncActiveMenu(p);
      // 登录后拉一次对话列表
      if (isLoggedIn.value) {
        loadConversations();
      }
    },
  );
</script>

<template>
  <div class="app-container">
    <router-view v-if="!isLoggedIn" />

    <div v-else class="main-layout">
      <aside class="sidebar">
        <div class="sidebar-header">
          <div class="logo">
            <el-icon class="logo-icon" :size="22" color="#667eea"
              ><Document
            /></el-icon>
            <span class="logo-text">DocuAgent</span>
          </div>
        </div>

        <div class="sidebar-actions">
          <button class="new-chat-btn" @click="newChat">
            <el-icon :size="16"><Plus /></el-icon>
            <span>新建对话</span>
          </button>
        </div>

        <div class="conv-section">
          <div class="section-label">最近对话</div>
          <div class="conv-list">
            <div
              v-for="conv in conversations"
              :key="conv.id"
              class="conv-item"
              :class="{ active: currentConvId === conv.id }"
              @click="switchConversation(conv)">
              <el-icon class="conv-icon" :size="14"><ChatDotRound /></el-icon>
              <span class="conv-title">{{ conv.title || "新对话" }}</span>
              <el-icon
                class="conv-del"
                :size="24"
                @click="deleteConversation($event, conv)"
                title="删除">
                <Delete />
              </el-icon>
            </div>
            <div v-if="conversations.length === 0" class="conv-empty">
              暂无对话，点击上方新建
            </div>
          </div>
        </div>

        <div class="sidebar-footer">
          <div class="user-info">
            <div class="user-avatar">
              <el-icon :size="16"><User /></el-icon>
            </div>
            <span class="user-name">{{ username }}</span>
          </div>
          <div class="menu-list">
            <div
              class="menu-item"
              :class="{ active: activeMenu === 'chat' }"
              @click="switchMenu('chat')">
              <el-icon class="menu-icon" :size="16"><ChatDotRound /></el-icon>
              <span class="menu-text">聊天助手</span>
            </div>
            <div
              class="menu-item"
              :class="{ active: activeMenu === 'documents' }"
              @click="switchMenu('documents')">
              <el-icon class="menu-icon" :size="16"><Document /></el-icon>
              <span class="menu-text">文档管理</span>
            </div>
            <div
              class="menu-item"
              :class="{ active: activeMenu === 'admin' }"
              @click="switchMenu('admin')">
              <el-icon class="menu-icon" :size="16"><Setting /></el-icon>
              <span class="menu-text">管理后台</span>
              <el-icon class="menu-arrow" :size="12" :class="{ open: adminSubmenuOpen }">
                <ArrowDown />
              </el-icon>
            </div>
            <div v-if="adminSubmenuOpen" class="admin-submenu">
              <div class="submenu-item" :class="{ active: route.path === '/admin/providers' }" @click="gotoAdmin('providers')">
                <el-icon :size="14"><Connection /></el-icon><span>供应商管理</span>
              </div>
              <div class="submenu-item" :class="{ active: route.path === '/admin/rag-config' }" @click="gotoAdmin('rag-config')">
                <el-icon :size="14"><Tools /></el-icon><span>知识库配置</span>
              </div>
              <div class="submenu-item" :class="{ active: route.path === '/admin/monitor' }" @click="gotoAdmin('monitor')">
                <el-icon :size="14"><DataLine /></el-icon><span>系统监控</span>
              </div>
            </div>
            <div class="menu-item logout" @click="handleLogout">
              <el-icon class="menu-icon" :size="16"><SwitchButton /></el-icon>
              <span class="menu-text">退出登录</span>
            </div>
          </div>
        </div>
      </aside>

      <main class="main-content">
        <router-view @conv-created="handleConvCreated" />
      </main>
    </div>
  </div>
</template>

<style scoped>
  .app-container {
    height: 100vh;
    width: 100%;
    overflow: hidden;
  }

  .main-layout {
    display: flex;
    height: 100vh;
    width: 100%;
  }

  .sidebar {
    width: 260px;
    background: #1e1e2d;
    color: #a0a0b8;
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    border-right: 1px solid #2d2d44;
  }

  .sidebar-header {
    padding: 20px 16px;
    border-bottom: 1px solid #2d2d44;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .logo-icon {
    flex-shrink: 0;
  }

  .logo-text {
    font-size: 18px;
    font-weight: 600;
    color: white;
  }

  .sidebar-actions {
    padding: 16px;
  }

  .new-chat-btn {
    width: 100%;
    height: 40px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    transition: all 0.2s;
  }

  .new-chat-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  }

  .conv-section {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .section-label {
    padding: 8px 20px;
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 1px;
    color: #6b6b80;
    font-weight: 600;
  }

  .conv-list {
    flex: 1;
    overflow-y: auto;
    padding: 0 8px;
  }

  .conv-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13px;
    color: #a0a0b8;
    transition: all 0.15s;
    margin-bottom: 2px;
  }

  .conv-item:hover {
    background: #2d2d44;
    color: #fff;
  }

  .conv-item:hover .conv-del {
    opacity: 1;
  }

  .conv-item.active {
    background: #2d2d44;
    color: #fff;
  }

  .conv-icon {
    flex-shrink: 0;
    opacity: 0.8;
  }

  .conv-title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .conv-del {
    opacity: 0.7;
    padding: 4px;
    color: #fca5a5;
    border-radius: 4px;
    transition: all 0.15s;
    flex-shrink: 0;
  }

  .conv-del:hover {
    opacity: 1;
    color: #f87171;
    background: rgba(248, 113, 113, 0.15);
  }

  .conv-empty {
    text-align: center;
    color: #5a5a70;
    font-size: 12px;
    padding: 24px 12px;
  }

  .sidebar-footer {
    border-top: 1px solid #2d2d44;
    padding: 12px;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    margin-bottom: 8px;
  }

  .user-avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: #2d2d44;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #a0a0b8;
  }

  .user-name {
    font-size: 13px;
    color: #d0d0e0;
    font-weight: 500;
  }

  .menu-list {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .menu-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13px;
    color: #a0a0b8;
    transition: all 0.15s;
  }

  .menu-item:hover {
    background: #2d2d44;
    color: #fff;
  }

  .menu-item.active {
    background: #2d2d44;
    color: #fff;
  }

  .menu-item.logout:hover {
    color: #f87171;
  }

  .menu-icon {
    width: 20px;
    text-align: center;
    opacity: 0.9;
  }

  .menu-text {
    flex: 1;
  }

  .menu-arrow {
    opacity: 0.6;
    transition: transform 0.2s;
  }
  .menu-arrow.open {
    transform: rotate(180deg);
  }

  .admin-submenu {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 4px 0 4px 22px;
  }
  .submenu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 12px;
    color: #8a8aa0;
    transition: all 0.15s;
  }
  .submenu-item:hover {
    background: #2d2d44;
    color: #fff;
  }
  .submenu-item.active {
    background: #2d2d44;
    color: #7c9cff;
  }

  .main-content {
    flex: 1;
    background: #f9fafb;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
</style>
