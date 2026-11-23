import { defineStore } from "pinia"

// 从 localStorage 安全读取 isAdmin（兼容老数据）
function readIsAdmin() {
  return localStorage.getItem("isAdmin") === "true"
}

export const useUserStore = defineStore("user", {
  state: () => ({
    token: localStorage.getItem("token") || null,
    username: localStorage.getItem("username") || "",
    // 关键：isAdmin 存进 Pinia 成为响应式状态，不能只依赖 localStorage
    isAdmin: readIsAdmin()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem("token", token)
    },
    setUserInfo(username, isAdmin) {
      this.username = username || ""
      this.isAdmin = !!isAdmin
      localStorage.setItem("username", this.username)
      localStorage.setItem("isAdmin", this.isAdmin ? "true" : "false")
    },
    // 多标签页同步：外部修改 localStorage 时刷新响应式状态
    syncFromStorage() {
      this.token = localStorage.getItem("token") || null
      this.username = localStorage.getItem("username") || ""
      this.isAdmin = readIsAdmin()
    },
    clearToken() {
      this.token = null
      this.username = ""
      this.isAdmin = false
      localStorage.removeItem("token")
      localStorage.removeItem("username")
      localStorage.removeItem("isAdmin")
    }
  }
})
