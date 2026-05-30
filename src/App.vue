<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { categoryApi, messageApi } from '@/api'
import ChatWidget from '@/components/ChatWidget.vue'
import {
  School, Search, ShoppingCart, ChatDotRound, User, ArrowDown,
  Plus, Setting, Message, Box, SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const searchKeyword = ref('')
const categories = ref<any[]>([])
const unreadMessageCount = ref(0)

let refreshTimer: number | null = null

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)
const cartCount = computed(() => cartStore.totalCount)

onMounted(async () => {
  try {
    const res = await categoryApi.getList()
    categories.value = res || []
  } catch (e) {
    console.error('获取分类失败', e)
  }

  if (isLoggedIn.value) {
    cartStore.fetchCart()
    loadUnreadCount()
    startRefreshTimer()
  }
})

onUnmounted(() => {
  stopRefreshTimer()
})

watch(() => route.path, () => {
  if (isLoggedIn.value) loadUnreadCount()
})

watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    loadUnreadCount()
    startRefreshTimer()
  } else {
    stopRefreshTimer()
    unreadMessageCount.value = 0
  }
})

function startRefreshTimer() {
  stopRefreshTimer()
  refreshTimer = window.setInterval(() => {
    if (isLoggedIn.value) loadUnreadCount()
  }, 10000)
}

function stopRefreshTimer() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

async function loadUnreadCount() {
  if (!isLoggedIn.value) return
  try {
    const res = await messageApi.getUnreadCount()
    unreadMessageCount.value = res || 0
  } catch (e) {
    unreadMessageCount.value = 0
  }
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ name: 'Search', query: { keyword: searchKeyword.value } })
  }
}

function goToLogin() { router.push('/login') }
function goToRegister() { router.push('/register') }
function goHome() { router.push('/') }
function goCart() { router.push('/cart') }
function goUserCenter() { router.push('/user') }
function goMyProducts() { router.push('/my-products') }
function goMessages() { router.push('/messages') }
function goAdmin() { router.push('/admin') }

function logout() {
  userStore.logout()
  router.push('/')
  location.reload()
}
</script>

<template>
  <div class="app">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="container header-content">
        <!-- Logo -->
        <div class="logo" @click="goHome">
          <el-icon class="logo-icon" :size="28"><School /></el-icon>
          <span class="logo-text">CampusMart</span>
        </div>

        <!-- 搜索框 -->
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品..."
            size="large"
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button :icon="Search" @click="handleSearch" />
            </template>
          </el-input>
        </div>

        <!-- 导航菜单 -->
        <nav class="nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/category/0" class="nav-link">商品</router-link>
          <router-link v-if="isLoggedIn" to="/publish" class="nav-link publish-link">
            <el-icon class="publish-icon"><Plus /></el-icon>
            发布闲置
          </router-link>
        </nav>

        <!-- 用户区域 -->
        <div class="user-area">
          <!-- 购物车 -->
          <div class="icon-btn" @click="goCart">
            <el-badge :value="cartCount" :hidden="cartCount === 0" :max="99">
              <el-icon :size="22"><ShoppingCart /></el-icon>
            </el-badge>
            <span class="icon-label">购物车</span>
          </div>

          <!-- 私信 -->
          <div class="icon-btn" @click="goMessages">
            <el-badge :value="unreadMessageCount" :hidden="unreadMessageCount === 0" :max="99">
              <el-icon :size="22"><ChatDotRound /></el-icon>
            </el-badge>
            <span class="icon-label">私信</span>
          </div>

          <!-- 登录/用户菜单 -->
          <template v-if="isLoggedIn">
            <el-dropdown trigger="click" @command="(cmd: string) => {
              if (cmd === 'user') goUserCenter()
              if (cmd === 'products') goMyProducts()
              if (cmd === 'messages') goMessages()
              if (cmd === 'admin') goAdmin()
              if (cmd === 'logout') logout()
            }">
              <div class="user-info">
                <el-avatar :size="36" :src="userInfo?.avatar || '/images/avatars/default-avatar.svg'" />
                <span class="username">{{ userInfo?.nickname || userInfo?.username }}</span>
                <el-icon :size="14"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="user">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="products">
                    <el-icon><Box /></el-icon>我的发布
                  </el-dropdown-item>
                  <el-dropdown-item command="messages">
                    <el-icon><Message /></el-icon>我的消息
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userInfo?.role === 'ADMIN'" command="admin">
                    <el-icon><Setting /></el-icon>管理后台
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button class="btn-login" @click="goToLogin">登录</el-button>
            <el-button type="primary" @click="goToRegister">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="main">
      <router-view />
    </main>

    <!-- 底部 -->
    <footer class="footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-section brand">
            <div class="footer-logo" @click="goHome">
              <el-icon :size="24"><School /></el-icon>
              <span>CampusMart</span>
            </div>
            <p>校园闲置交易平台，为毕业生和学生提供便捷的二手物品交易服务。</p>
          </div>
          <div class="footer-section">
            <h4>快速链接</h4>
            <ul>
              <li><router-link to="/">首页</router-link></li>
              <li><router-link to="/category/0">全部商品</router-link></li>
              <li><router-link to="/publish">发布闲置</router-link></li>
            </ul>
          </div>
          <div class="footer-section">
            <h4>帮助中心</h4>
            <ul>
              <li><a href="#">如何发布</a></li>
              <li><a href="#">交易安全</a></li>
              <li><a href="#">常见问题</a></li>
              <li><a href="#">联系客服</a></li>
            </ul>
          </div>
          <div class="footer-section">
            <h4>关于平台</h4>
            <p>校园二手，安全交易</p>
            <p class="slogan">让闲置流转，让价值延续</p>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2026 CampusMart. All rights reserved.</p>
        </div>
      </div>
    </footer>

    <!-- 智能客服 -->
    <ChatWidget />
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-background);
}

/* ── Header ── */
.header {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.85);
}

.header-content {
  display: flex;
  align-items: center;
  height: var(--header-height);
  gap: var(--space-6);
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  flex-shrink: 0;
  color: var(--color-primary);
  transition: opacity var(--transition-fast);
}

.logo:hover {
  opacity: 0.85;
}

.logo-text {
  font-family: var(--font-heading);
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-primary);
  letter-spacing: -0.02em;
}

/* Search */
.search-box {
  flex: 1;
  max-width: 480px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-full) !important;
  background: var(--color-muted);
  border: 2px solid transparent !important;
  box-shadow: none !important;
  transition: all var(--transition-base);
}

.search-input :deep(.el-input__wrapper:hover) {
  background: var(--color-border-light);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  background: var(--color-surface);
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.14) !important;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 var(--radius-full) var(--radius-full) 0 !important;
  background: var(--color-primary);
  border: none;
  color: #fff;
}

.search-input :deep(.el-input-group__append .el-button) {
  color: #fff !important;
}

/* Nav */
.nav {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.nav-link {
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.nav-link:hover,
.nav-link.router-link-active {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.publish-link {
  background: var(--color-primary) !important;
  color: #fff !important;
  gap: var(--space-1);
  display: flex;
  align-items: center;
}

.publish-link:hover {
  background: var(--color-primary-hover) !important;
  color: #fff !important;
}

/* User Area */
.user-area {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.icon-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  min-height: var(--touch-min);
  min-width: var(--touch-min);
  padding: var(--space-1);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
  color: var(--color-text-secondary);
  position: relative;
}

.icon-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.icon-label {
  font-size: 11px;
  margin-top: 1px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.user-info:hover {
  background: var(--color-muted);
}

.username {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-login {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.btn-login:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

/* ── Main ── */
.main {
  flex: 1;
}

/* ── Footer ── */
.footer {
  background: linear-gradient(135deg, #123026 0%, #0F241E 100%);
  color: rgba(255, 255, 255, 0.7);
  padding: var(--space-12) 0 var(--space-6);
  margin-top: var(--space-16);
}

.footer-content {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: var(--space-10);
  margin-bottom: var(--space-8);
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  margin-bottom: var(--space-4);
  color: #fff;
  font-family: var(--font-heading);
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
}

.footer-section h4 {
  color: #fff;
  font-family: var(--font-heading);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  margin-bottom: var(--space-4);
}

.footer-section p {
  line-height: var(--leading-relaxed);
  font-size: var(--text-sm);
}

.footer-section .slogan {
  margin-top: var(--space-2);
  opacity: 0.5;
}

.footer-section ul li {
  margin-bottom: var(--space-2);
}

.footer-section ul li a {
  font-size: var(--text-sm);
  transition: color var(--transition-fast);
}

.footer-section ul li a:hover {
  color: var(--color-primary-light);
}

.footer-bottom {
  text-align: center;
  padding-top: var(--space-6);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: var(--text-xs);
  opacity: 0.5;
}

/* ── Dropdown icons ── */
.el-dropdown-menu__item .el-icon {
  margin-right: var(--space-2);
}

/* ── Responsive ── */
@media (max-width: 1200px) {
  .nav {
    display: none;
  }
}

@media (max-width: 768px) {
  .header-content {
    gap: var(--space-3);
    height: 60px;
  }

  .logo-text {
    display: none;
  }

  .search-box {
    max-width: none;
  }

  .icon-label {
    display: none;
  }

  .username {
    display: none;
  }

  .footer-content {
    grid-template-columns: 1fr 1fr;
    gap: var(--space-8);
  }
}
</style>
