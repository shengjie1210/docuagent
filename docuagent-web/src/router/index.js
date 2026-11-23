import { createRouter, createWebHistory } from "vue-router"
import { useUserStore } from "@/stores/user"
import LoginView from "@/views/LoginView.vue"
import ChatView from "@/views/ChatView.vue"
import DocumentView from "@/views/DocumentView.vue"
import AdminView from "@/views/admin/AdminView.vue"
import ProviderManage from "@/views/admin/ProviderManage.vue"
import RagConfig from "@/views/admin/RagConfig.vue"
import SystemMonitor from "@/views/admin/SystemMonitor.vue"

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: LoginView
    },
    {
      path: "/",
      name: "chat",
      component: ChatView,
      meta: { requiresAuth: true }
    },
    {
      path: "/documents",
      name: "documents",
      component: DocumentView,
      meta: { requiresAuth: true }
    },
    {
      path: "/admin",
      component: AdminView,
      meta: { requiresAuth: true, requiresAdmin: true },
      redirect: "/admin/providers",
      children: [
        { path: "providers", name: "admin-providers", component: ProviderManage, meta: { requiresAuth: true, requiresAdmin: true } },
        { path: "rag-config", name: "admin-rag", component: RagConfig, meta: { requiresAuth: true, requiresAdmin: true } },
        { path: "monitor", name: "admin-monitor", component: SystemMonitor, meta: { requiresAuth: true, requiresAdmin: true } }
      ]
    }
  ]
})

// 路由守卫：登录校验 + 管理后台解锁校验
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token")
  const adminToken = localStorage.getItem("adminToken")

  if (to.meta.requiresAuth && !token) {
    next("/login")
    return
  }
  // 未解锁管理后台时，直接输 /admin URL 也拦住，回聊天页由侧边栏弹密码框
  if (to.meta.requiresAdmin && !adminToken) {
    next("/")
    return
  }
  next()
})

export default router
