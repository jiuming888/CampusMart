<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import AnimatedCharacters from '@/components/AnimatedCharacters.vue'

const router = useRouter()

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
})

const loading = ref(false)
const isTyping = ref(false)
const typingField = ref<'username' | 'password' | null>(null)

async function handleRegister() {
  if (!registerForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!registerForm.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }

  loading.value = true
  try {
    await userApi.register({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname || registerForm.username,
      phone: registerForm.phone,
      email: registerForm.email
    })

    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <!-- 左侧品牌区域 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-logo">
            <el-icon :size="60"><School /></el-icon>
          </div>
          <h1>CampusMart</h1>
          <p class="brand-slogan">加入我们</p>
          <p class="brand-desc">
            开启你的校园闲置交易之旅<br/>
            让闲置物品发挥最大价值
          </p>
        </div>

        <div class="characters-area">
          <AnimatedCharacters :is-typing="isTyping" context="register" :typing-field="typingField" />
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

      <!-- 右侧注册表单 -->
      <div class="form-section">
        <div class="form-container">
          <h2>创建账号</h2>
          <p class="form-subtitle">注册 CampusMart 开始交易</p>

          <el-form :model="registerForm" class="register-form" @submit.prevent="handleRegister" autocomplete="off">
            <el-form-item>
              <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名（必填）"
                size="large"
                :prefix-icon="User"
                autocomplete="off"
                @focus="isTyping = true; typingField = 'username'"
                @blur="isTyping = false; typingField = null"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.nickname"
                placeholder="请输入昵称（选填）"
                size="large"
                :prefix-icon="UserFilled"
                autocomplete="off"
                @focus="isTyping = true"
                @blur="isTyping = false"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.phone"
                placeholder="请输入手机号（选填）"
                size="large"
                :prefix-icon="Phone"
                autocomplete="off"
                @focus="isTyping = true"
                @blur="isTyping = false"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱（选填）"
                size="large"
                :prefix-icon="Message"
                autocomplete="off"
                @focus="isTyping = true"
                @blur="isTyping = false"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码（至少6位）"
                size="large"
                :prefix-icon="Lock"
                autocomplete="new-password"
                @focus="isTyping = true; typingField = 'password'"
                @blur="isTyping = false; typingField = null"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请确认密码"
                size="large"
                :prefix-icon="Lock"
                autocomplete="new-password"
                @focus="isTyping = true; typingField = 'password'"
                @blur="isTyping = false; typingField = null"
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="register-btn"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>已有账号？</span>
            <el-link type="primary" @click="goLogin">立即登录</el-link>
          </div>

          <p class="agreement">
            注册即表示同意<a href="#">《用户协议》</a>和<a href="#">《隐私政策》</a>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { User, UserFilled, Lock, Phone, Message, School, Box } from '@element-plus/icons-vue'
export default {
  components: { User, UserFilled, Lock, Phone, Message, School, Box }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-primary) 0%, #5B21B6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-5);
}

.register-container {
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
  padding: var(--space-10) var(--space-10);
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
  margin-bottom: var(--space-6);
}

.register-form {
  margin-bottom: var(--space-4);
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: var(--text-base);
  border-radius: var(--radius-lg);
}

.form-footer {
  text-align: center;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  margin-bottom: var(--space-3);
}

.agreement {
  text-align: center;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.agreement a {
  color: var(--color-primary);
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
