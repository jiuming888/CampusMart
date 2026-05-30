<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  School, DataBoard, Box, User, Document, Collection,
  Fold, Expand, HomeFilled, SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapsed = ref(false)

const menuItems = [
  { path: '/admin/dashboard', name: 'Dashboard', label: '仪表盘', icon: DataBoard },
  { path: '/admin/products', name: 'AdminProducts', label: '商品管理', icon: Box },
  { path: '/admin/users', name: 'AdminUsers', label: '用户管理', icon: User },
  { path: '/admin/orders', name: 'AdminOrders', label: '订单管理', icon: Document },
  { path: '/admin/categories', name: 'AdminCategories', label: '分类管理', icon: Collection }
]

const activeMenu = computed(() => route.path)
const userInfo = computed(() => userStore.userInfo)

function goTo(path: string) { router.push(path) }
function goHome() { router.push('/') }
function logout() { userStore.logout(); router.push('/login') }
</script>

<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <div class="logo" @click="goHome">
          <el-icon :size="24"><School /></el-icon>
          <span v-show="!isCollapsed" class="logo-text">CampusMart</span>
        </div>
        <div class="badge" v-show="!isCollapsed">管理后台</div>
      </div>

      <nav class="menu">
        <div
          v-for="item in menuItems"
          :key="item.path"
          class="menu-item"
          :class="{ active: activeMenu === item.path }"
          @click="goTo(item.path)"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span v-show="!isCollapsed" class="menu-label">{{ item.label }}</span>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="menu-item" @click="goHome">
          <el-icon :size="20"><HomeFilled /></el-icon>
          <span v-show="!isCollapsed" class="menu-label">返回前台</span>
        </div>
        <div class="menu-item collapse-btn" @click="isCollapsed = !isCollapsed">
          <el-icon :size="20"><component :is="isCollapsed ? Expand : Fold" /></el-icon>
          <span v-show="!isCollapsed" class="menu-label">收起菜单</span>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area" :class="{ expanded: isCollapsed }">
      <header class="topbar">
        <div class="topbar-left">
          <h2>{{ menuItems.find(m => m.path === activeMenu)?.label || '管理后台' }}</h2>
        </div>
        <div class="topbar-right">
          <el-dropdown trigger="click">
            <div class="admin-info">
              <el-avatar :size="32" :src="userInfo?.avatar || undefined" style="background: #C0C4CC;">
                <el-icon :size="18" color="#fff"><User /></el-icon>
              </el-avatar>
              <span class="admin-name">{{ userInfo?.nickname || userInfo?.username || '管理员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-background);
}

/* ── Sidebar ── */
.sidebar {
  width: var(--admin-sidebar-width);
  background: linear-gradient(180deg, #1E1345 0%, #2D1B5E 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: width var(--transition-slow);
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: var(--z-sidebar);
}

.sidebar.collapsed {
  width: var(--admin-sidebar-collapsed);
}

.sidebar-header {
  padding: var(--space-5) var(--space-4);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  color: var(--color-secondary);
  transition: opacity var(--transition-fast);
  margin-bottom: var(--space-3);
}

.logo:hover {
  opacity: 0.8;
}

.logo-text {
  font-family: var(--font-heading);
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: #fff;
}

.badge {
  display: inline-block;
  background: rgba(167, 139, 250, 0.2);
  color: var(--color-secondary);
  font-size: 11px;
  font-weight: var(--font-medium);
  padding: 2px 10px;
  border-radius: var(--radius-full);
}

/* Menu */
.menu {
  flex: 1;
  padding: var(--space-3) var(--space-2);
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-1);
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
  gap: var(--space-3);
  color: rgba(255, 255, 255, 0.6);
  min-height: var(--touch-min);
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.9);
}

.menu-item.active {
  background: var(--color-primary);
  color: #fff;
}

.menu-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  white-space: nowrap;
}

.sidebar-footer {
  padding: var(--space-3) var(--space-2);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.collapse-btn {
  opacity: 0.5;
}

/* ── Main Area ── */
.main-area {
  flex: 1;
  margin-left: var(--admin-sidebar-width);
  transition: margin-left var(--transition-slow);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-area.expanded {
  margin-left: var(--admin-sidebar-collapsed);
}

/* Topbar */
.topbar {
  height: 64px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.topbar-left h2 {
  font-family: var(--font-heading);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
}

.topbar-right {
  display: flex;
  align-items: center;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-lg);
  transition: background var(--transition-fast);
}

.admin-info:hover {
  background: var(--color-muted);
}

.admin-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}

/* Content */
.content {
  flex: 1;
  padding: var(--space-6);
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .sidebar {
    width: var(--admin-sidebar-collapsed);
  }
  .sidebar .logo-text,
  .sidebar .badge,
  .sidebar .menu-label {
    display: none;
  }
  .main-area {
    margin-left: var(--admin-sidebar-collapsed);
  }
}
</style>
