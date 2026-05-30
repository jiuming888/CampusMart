<template>
  <div class="success-page">
    <div class="success-card">
      <!-- 查询中 -->
      <template v-if="status === 'checking'">
        <div class="spinner"></div>
        <h1>正在确认支付结果...</h1>
        <p>请稍候，正在查询支付宝支付状态</p>
      </template>

      <!-- 支付成功 -->
      <template v-else-if="status === 'paid'">
        <div class="icon success">✓</div>
        <h1>支付成功！</h1>
        <p>感谢您的购买，正在跳转订单页面...</p>
      </template>

      <!-- 支付结果待确认 -->
      <template v-else>
        <div class="icon pending">!</div>
        <h1>支付结果确认中</h1>
        <p>支付宝处理可能需要几秒钟，请稍后到订单页面查看</p>
        <el-button type="primary" @click="router.push('/order')">查看我的订单</el-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const status = ref<'checking' | 'paid' | 'pending'>('checking')

onMounted(async () => {
  const orderNo = route.query.orderNo as string
  if (!orderNo) {
    router.replace('/order')
    return
  }

  // 最多重试3次查询支付状态（支付宝沙箱可能有延迟）
  for (let i = 0; i < 3; i++) {
    try {
      const res: any = await request.get('/alipay/query', { params: { orderNo } })
      if (res && res.paid) {
        status.value = 'paid'
        await cartStore.clearCart()
        setTimeout(() => {
          router.push('/order')
        }, 2500)
        return
      }
    } catch (e) {
      console.error('查询支付状态失败:', e)
    }
    // 等2秒后重试
    if (i < 2) {
      await new Promise(resolve => setTimeout(resolve, 2000))
    }
  }

  // 3次重试后仍未确认
  status.value = 'pending'
  setTimeout(() => {
    router.push('/order')
  }, 5000)
})
</script>

<style scoped>
.success-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-width: 460px;
  width: 90%;
}

.icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  font-size: 48px;
  color: #fff;
}

.icon.success {
  background: #52c41a;
}

.icon.pending {
  background: #faad14;
}

.spinner {
  width: 56px;
  height: 56px;
  border: 4px solid #e0e0e0;
  border-top-color: #1677ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 24px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

h1 {
  font-size: 24px;
  color: #333;
  margin-bottom: 12px;
}

p {
  color: #888;
  font-size: 15px;
  margin-bottom: 20px;
}
</style>
