import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getTokenKey, getUserKey } from '@/utils/tabId'

interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  role: string
}

export const useUserStore = defineStore('user', () => {
  // 使用 sessionStorage - 关闭浏览器后登录状态自动清除
  const token = ref(sessionStorage.getItem(getTokenKey()) || '')
  const userInfo = ref<UserInfo | null>(JSON.parse(sessionStorage.getItem(getUserKey()) || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  function setToken(newToken: string) {
    token.value = newToken
    sessionStorage.setItem(getTokenKey(), newToken)
  }

  function setUserInfo(info: UserInfo | null) {
    userInfo.value = info
    if (info) {
      sessionStorage.setItem(getUserKey(), JSON.stringify(info))
    } else {
      sessionStorage.removeItem(getUserKey())
    }
  }

  function login(newToken: string, info: UserInfo) {
    setToken(newToken)
    setUserInfo(info)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem(getTokenKey())
    sessionStorage.removeItem(getUserKey())
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    setToken,
    setUserInfo,
    login,
    logout
  }
})
