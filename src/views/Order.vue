<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api'
import { formatPrice, formatDate, getOrderStatusText, getOrderStatusType, getFirstImage } from '@/utils'

const router = useRouter()

const orders = ref<any[]>([])
const loading = ref(true)
const activeTab = ref('all')

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: 'PENDING' },
  { label: '待发货', value: 'PAID' },
  { label: '待收货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' }
]

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  loading.value = true
  try {
    const params: any = {}
    if (activeTab.value !== 'all') {
      params.status = activeTab.value
    }
    const res = await orderApi.getList({ page: 1, pageSize: 100, ...params })
    orders.value = res?.records || []
  } catch (e) {
    console.error('获取订单失败', e)
  } finally {
    loading.value = false
  }
}

function goToOrderDetail(id: number) {
  router.push(`/order/${id}`)
}

async function payOrder(id: number) {
  try {
    const params = new URLSearchParams({
      orderId: String(id),
      frontendUrl: window.location.origin,
      publicApiUrl: window.location.origin
    })
    const res = await fetch('/api/alipay/pay', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    })
    const html = await res.text()
    const parser = new DOMParser()
    const doc = parser.parseFromString(html, 'text/html')
    const form = doc.querySelector('form')
    if (form) {
      document.body.appendChild(form)
      form.submit()
      return
    }
    ElMessage.error('支付发起失败: ' + html)
  } catch (e: any) {
    ElMessage.error('支付发起失败: ' + (e?.message || ''))
  }
}

async function cancelOrder(id: number) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await orderApi.cancel(id)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '取消失败')
    }
  }
}

async function confirmReceive(id: number) {
  try {
    await orderApi.confirm(id)
    ElMessage.success('确认收货成功')
    await loadOrders()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function onTabChange() {
  loadOrders()
}
</script>

<template>
  <div class="order-page">
    <div class="container">
      <div class="page-header">
        <h1>我的订单</h1>
      </div>
      
      <div class="order-tabs">
        <el-tabs v-model="activeTab" @tab-change="onTabChange">
          <el-tab-pane v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :name="opt.value" />
        </el-tabs>
      </div>
      
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>
      
      <div v-else-if="orders.length === 0" class="empty-state">
        <div class="icon"><el-icon :size="64"><Document /></el-icon></div>
        <p>暂无订单</p>
        <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
      </div>
      
      <div v-else class="order-list">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <el-tag :type="getOrderStatusType(order.status)" size="small">
              {{ getOrderStatusText(order.status) }}
            </el-tag>
          </div>
          
          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item" @click="goToOrderDetail(order.id)">
              <img :src="getFirstImage(item.productImage)" alt="" />
              <div class="item-info">
                <h3>{{ item.productName }}</h3>
                <p>数量：{{ item.quantity }}</p>
              </div>
              <div class="item-price">{{ formatPrice(item.price) }}</div>
            </div>
          </div>
          
          <div class="order-footer">
            <div class="order-info">
              <span>下单时间：{{ formatDate(order.createTime) }}</span>
              <span class="total">合计：<strong>{{ formatPrice(order.totalAmount) }}</strong></span>
            </div>
            <div class="order-actions">
              <el-button v-if="order.status === 'PENDING'" size="small" @click.stop="cancelOrder(order.id)">
                取消订单
              </el-button>
              <el-button v-if="order.status === 'PENDING'" type="primary" size="small" @click.stop="payOrder(order.id)">
                去支付
              </el-button>
              <el-button v-if="order.status === 'SHIPPED'" type="primary" size="small" @click.stop="confirmReceive(order.id)">
                确认收货
              </el-button>
              <el-button size="small" @click.stop="goToOrderDetail(order.id)">查看详情</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Loading, Document } from '@element-plus/icons-vue'
export default {
  components: { Loading, Document }
}
</script>

<style scoped>
.order-page {
  background: #f8f8f8;
  padding: 20px 0;
  min-height: calc(100vh - 200px);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
}

.order-tabs {
  margin-bottom: 20px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8f8f8;
}

.order-no {
  font-size: 14px;
  color: #666;
}

.order-items {
  padding: 0 20px;
}

.order-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.3s;
}

.order-item:hover {
  background: #fafafa;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.order-item .item-info {
  flex: 1;
  margin-left: 16px;
}

.order-item .item-info h3 {
  font-size: 14px;
  margin-bottom: 4px;
}

.order-item .item-info p {
  color: #999;
  font-size: 13px;
}

.item-price {
  font-size: 16px;
  color: #333;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fafafa;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.order-info .total {
  font-size: 14px;
}

.order-info .total strong {
  color: #FF6B35;
  font-size: 18px;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 80px 20px;
  text-align: center;
}

.empty-state .icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state p {
  color: #999;
  margin-bottom: 24px;
}
</style>
