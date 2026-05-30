<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi, warningApi } from '@/api'
import AnimatedCharacters from '@/components/AnimatedCharacters.vue'

const router = useRouter()
const userStore = useUserStore()

const loginForm = reactive({
  username: '',
  password: ''
})

const loading = ref(false)
const activeTab = ref('user')
const isTyping = ref(false)
const typingField = ref<'username' | 'password' | null>(null)
const showPassword = ref(false)

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    let res
    if (activeTab.value === 'admin') {
      res = await userApi.adminLogin(loginForm)
    } else {
      res = await userApi.login(loginForm)
    }

    const { token, user, isAdmin } = res
    userStore.login(token, user)

    ElMessage.success('登录成功')

    if (isAdmin) {
      router.push('/admin')
    } else {
      await checkAndShowWarnings()
      const redirect = router.currentRoute.value.query.redirect as string
      router.push(redirect || '/')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function checkAndShowWarnings() {
  try {
    const res = await warningApi.getMyWarnings()
    const unreadWarnings = (res || []).filter((w: any) => !w.isRead)

    if (unreadWarnings.length > 0) {
      const warningsHtml = unreadWarnings.map((w: any, index: number) =>
        `<div style="margin-bottom: 16px; padding: 12px; background: #FFF7F5; border-left: 4px solid #F56C6C; border-radius: 4px;">
          <div style="font-weight: bold; margin-bottom: 8px; color: #333;">警告 ${index + 1}</div>
          <div style="color: #666; line-height: 1.6;">${w.reason}</div>
          <div style="font-size: 12px; color: #999; margin-top: 8px;">${w.createTime ? new Date(w.createTime).toLocaleString('zh-CN') : ''}</div>
        </div>`
      ).join('')

      await ElMessageBox.alert(
        `<div style="max-height: 400px; overflow-y: auto;">${warningsHtml}</div>`,
        `您有 ${unreadWarnings.length} 条管理员警告`,
        {
          confirmButtonText: '我已知悉',
          dangerouslyUseHTMLString: true,
          customClass: 'warning-dialog'
        }
      )

      await warningApi.markAllAsRead()
    }
  } catch (e: any) {
    console.log('检查警告信息完成', e)
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧品牌区域 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-logo">
            <el-icon :size="60"><School /></el-icon>
          </div>
          <h1>CampusMart</h1>
          <p class="brand-slogan">校园闲置交易平台</p>
        </div>

        <!-- 交互动画角色 -->
        <div class="characters-area">
          <AnimatedCharacters :is-typing="isTyping" :show-password="showPassword" :password-length="loginForm.password.length" :typing-field="typingField" />
        </div>

        <div class="brand-features">
          <div class="feature">
            <el-icon :size="24"><Box /></el-icon>
            <span>海量商品</span>
          </div>
          <div class="feature">
            <el-icon :size="24"><Lock /></el-icon>
            <span>安全交易</span>
          </div>
          <div class="feature">
            <el-icon :size="24"><UserFilled /></el-icon>
            <span>校园认证</span>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="form-section">
        <div class="form-container">
          <h2>欢迎回来</h2>
          <p class="form-subtitle">登录 CampusMart 开始交易</p>

          <el-tabs v-model="activeTab" class="login-tabs">
            <el-tab-pane label="用户登录" name="user" />
            <el-tab-pane label="管理员登录" name="admin" />
          </el-tabs>

          <el-form :model="loginForm" class="login-form" @submit.prevent="handleLogin" autocomplete="off">
            <el-form-item>
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
                autocomplete="off"
                @focus="isTyping = true; typingField = 'username'"
                @blur="isTyping = false; typingField = null"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                autocomplete="new-password"
                @focus="isTyping = true; typingField = 'password'"
                @blur="isTyping = false; typingField = null"
                @keyup.enter="handleLogin"
              >
                <template #suffix>
                  <el-icon class="eye-toggle" @click="showPassword = !showPassword">
                    <component :is="showPassword ? 'View' : 'Hide'" />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="login-btn"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>还没有账号？</span>
            <el-link type="primary" @click="goRegister">立即注册</el-link>
          </div>

          <div v-if="activeTab === 'admin'" class="admin-tip">
            <el-icon><Warning /></el-icon>
            <span>管理员账号请从这里登录</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { User, Lock, Warning, School, Box, UserFilled, View, Hide } from '@element-plus/icons-vue'
export default {
  components: { User, Lock, Warning, School, Box, UserFilled, View, Hide }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-primary) 0%, #5B21B6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-5);
}

.login-container {
  display: flex;
  background: var(--color-surface);
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
  max-width: 960px;
  width: 100%;
}

.brand-section {
  flex: 1.1;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a78bfa 100%);
  padding: var(--space-10) var(--space-8) var(--space-6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  position: relative;
  overflow: hidden;
  min-height: 560px;
}

.brand-content {
  text-align: center;
  position: relative;
  z-index: 2;
}

.brand-logo {
  margin-bottom: var(--space-3);
  color: #fff;
}

.brand-content h1 {
  font-family: var(--font-heading);
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  margin-bottom: var(--space-1);
}

.brand-slogan {
  font-size: var(--text-base);
  margin-bottom: var(--space-4);
  opacity: 0.9;
}

.brand-desc {
  font-size: var(--text-xs);
  line-height: var(--leading-relaxed);
  opacity: 0.75;
}

.characters-area {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  width: 100%;
}

.brand-features {
  display: flex;
  justify-content: center;
  gap: var(--space-8);
  position: relative;
  z-index: 2;
}

.feature {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  opacity: 0.85;
  font-size: var(--text-xs);
}

/* Decorative grid */
.brand-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 24px 24px;
  z-index: 0;
}

.brand-section::after {
  content: '';
  position: absolute;
  top: 20%;
  right: 15%;
  width: 200px;
  height: 200px;
  background: rgba(255,255,255,0.08);
  border-radius: 50%;
  filter: blur(60px);
  z-index: 0;
}

.form-section {
  flex: 0.9;
  padding: var(--space-12) var(--space-10);
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-container {
  width: 100%;
  max-width: 320px;
}

.form-container h2 {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
}

.form-subtitle {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  margin-bottom: var(--space-8);
}

.login-tabs {
  margin-bottom: var(--space-6);
}

.login-form {
  margin-bottom: var(--space-5);
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: var(--text-base);
  border-radius: var(--radius-lg);
}

.form-footer {
  text-align: center;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
}

.admin-tip {
  margin-top: var(--space-5);
  padding: var(--space-3);
  background: var(--color-warning-light);
  border-radius: var(--radius-lg);
  color: var(--color-warning);
  font-size: var(--text-xs);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  justify-content: center;
}

.eye-toggle {
  cursor: pointer;
  color: var(--color-text-muted);
  transition: color 0.2s;
}

.eye-toggle:hover {
  color: var(--color-text-primary);
}

@media (max-width: 768px) {
  .brand-section {
    display: none;
  }

  .form-section {
    padding: var(--space-10) var(--space-8);
  }
}
</style>

<style>
.warning-dialog {
  max-width: 500px !important;
}

.warning-dialog .el-message-box__content {
  padding: 10px 20px 20px !important;
}

.warning-dialog .el-message-box__title {
  color: #F56C6C !important;
  font-size: 18px !important;
}
</style>
