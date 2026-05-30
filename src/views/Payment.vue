<template>
  <div class="payment-page">
    <div class="payment-container">
      <div class="payment-header">
        <div class="header-left">
          <span class="alipay-logo">&#x1F1E8;&#x1F1F3;</span>
          <span class="header-title">支付宝安全支付</span>
        </div>
        <div class="header-right">
          <span class="secure-tag">
            <span class="lock-icon">&#x1F512;</span>
            交易安全
          </span>
        </div>
      </div>

      <div v-if="loading" class="status-body">
        <div class="loading-spinner-large"></div>
        <p class="status-title">正在加载订单...</p>
      </div>

      <div v-else-if="error" class="status-body">
        <div class="error-icon-large">&#x2716;</div>
        <p class="status-title">加载失败</p>
        <p class="status-sub">{{ error }}</p>
        <el-button @click="loadOrder" type="primary">重新加载</el-button>
      </div>

      <div v-else-if="order" class="order-content">
      <div v-if="paymentStatus === 'success'" class="success-panel">
        <div class="success-icon">&#x2714;</div>
        <p class="success-title">支付成功</p>
        <p class="success-sub">感谢您的购买，页面将自动跳转...</p>
      </div>

      <div v-else class="order-info">
        <div class="info-row">
          <span class="label">订单编号</span>
          <span class="value mono">{{ order.orderNo }}</span>
        </div>
        <div class="info-row">
          <span class="label">商品详情</span>
          <span class="value">校园闲置商品</span>
        </div>
        <div class="info-row highlight">
          <span class="label">应付金额</span>
          <span class="value amount">¥{{ order.totalAmount }}</span>
        </div>
      </div>

      <div v-if="paymentStatus !== 'success'" class="action-section">
        <el-button
          type="primary"
          size="large"
          class="pay-btn"
          @click="handlePayment"
          :loading="paying"
        >
          {{ paying ? '正在跳转支付宝...' : '去支付宝付款 ¥' + order.totalAmount }}
        </el-button>
        <el-button size="large" @click="goBack">返回</el-button>
      </div>

      <div v-else class="action-section">
        <el-button type="primary" size="large" @click="goToOrders">
          查看我的订单
        </el-button>
      </div>

      <div v-if="paymentStatus !== 'success'" class="payment-hint">
        <p>点击上方按钮，跳转到支付宝沙箱完成付款</p>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const paying = ref(false)
const error = ref('')
const order = ref<any>(null)
const pollTimer = ref<any>(null)
const paymentStatus = ref<'waiting' | 'success' | 'failed'>('waiting')

async function loadOrder() {
  loading.value = true
  error.value = ''

  const orderId = route.query.orderId as string
  if (!orderId) {
    error.value = '订单ID不存在'
    loading.value = false
    return
  }

  try {
    const res: any = await request.get(`/order/${orderId}`)
    order.value = res
    paymentStatus.value = order.value?.status === 'PAID' ? 'success' : 'waiting'
  } catch (e: any) {
    error.value = e?.message || '加载订单失败，请重试'
  } finally {
    loading.value = false
  }
}

async function handlePayment() {
  if (!order.value) return

  paying.value = true
  paymentStatus.value = 'waiting'

  try {
    const params = new URLSearchParams({
      orderId: String(order.value.id),
      frontendUrl: window.location.origin,
      publicApiUrl: window.location.origin
    })
    const response = await fetch('/api/alipay/pay', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: params.toString()
    })

    const data = await response.json()

    if (data.success && data.payUrl) {
      window.open(data.payUrl, '_blank', 'noopener,noreferrer')
      startPaymentPolling()
      return
    }

    ElMessage.error('支付发起失败: ' + (data.message || '未知错误'))
  } catch (e: any) {
    ElMessage.error(e?.message || '支付发起失败')
  } finally {
    paying.value = false
  }
}

async function startPaymentPolling() {
  if (pollTimer.value) clearInterval(pollTimer.value)
  
  pollTimer.value = setInterval(async () => {
    if (!order.value) return
    
    try {
      const res: any = await request.get(`/order/${order.value.id}`)
      const currentOrder = res
      
      if (currentOrder?.status === 'PAID') {
        clearInterval(pollTimer.value)
        paymentStatus.value = 'success'
        ElMessage.success('支付成功！')
        
        setTimeout(() => {
          router.push('/orders')
        }, 2000)
      }
    } catch (e) {
      console.error('轮询订单状态失败:', e)
    }
  }, 3000)
}

function cancelPayment() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
  paymentStatus.value = 'waiting'
}

function goBack() {
  cancelPayment()
  router.back()
}

function goToOrders() {
  cancelPayment()
  router.push('/orders')
}

onMounted(() => {
  loadOrder()
})

onUnmounted(() => {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
  }
})
</script>

<style scoped>
.payment-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a6fc4 0%, #0d47a1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.payment-container {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 520px;
  overflow: hidden;
}

.payment-header {
  background: #fff;
  padding: 20px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.alipay-logo {
  font-size: 28px;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #1677ff;
}

.header-right {
  display: flex;
  align-items: center;
}

.secure-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #52c41a;
  font-weight: 500;
}

.status-body {
  padding: 60px 40px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 380px;
  justify-content: center;
}

.loading-spinner-large {
  width: 56px;
  height: 56px;
  border: 4px solid #e0e0e0;
  border-top-color: #1677ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 24px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.order-content {
  padding: 24px;
}

.order-info {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row.highlight {
  background: #fff8f0;
  margin: 12px -20px -20px;
  padding: 16px 20px;
  border-radius: 0 0 12px 12px;
  border-bottom: none;
}

.info-row .label {
  color: #999;
  font-size: 14px;
}

.info-row .value {
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.info-row .value.mono {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.info-row .value.amount {
  font-size: 24px;
  font-weight: 700;
  color: #FF6B35;
}

.action-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-row {
  display: flex;
  gap: 12px;
}

.action-row .el-button {
  flex: 1;
}

.pay-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  border-radius: 25px;
  background: linear-gradient(135deg, #1677ff, #0958d9);
  border: none;
  font-weight: 600;
}

.pay-btn:not(:disabled):hover {
  background: linear-gradient(135deg, #4096ff, #1677ff);
}

.payment-hint {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #999;
}

.sandbox-tip {
  color: #1677ff;
  margin-top: 8px;
  font-weight: 500;
}

.success-panel {
  text-align: center;
  padding: 40px 20px;
}

.success-icon {
  width: 80px;
  height: 80px;
  background: #52c41a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  color: #fff;
  margin: 0 auto 24px;
  animation: popIn 0.4s ease;
}

.success-title {
  font-size: 26px;
  font-weight: 700;
  color: #52c41a;
  margin: 0 0 12px;
}

.success-sub {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.error-icon-large {
  width: 80px;
  height: 80px;
  background: #ff4d4f;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  color: #fff;
  margin-bottom: 24px;
  animation: popIn 0.4s ease;
}

@keyframes popIn {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.status-title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin: 0 0 12px;
}

.status-sub {
  font-size: 14px;
  color: #999;
  margin: 0 0 24px;
}
</style>

