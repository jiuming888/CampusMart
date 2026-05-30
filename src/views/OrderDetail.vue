<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api'
import { formatPrice, formatDate, getOrderStatusText, getOrderStatusType, getFirstImage } from '@/utils'

const route = useRoute()
const router = useRouter()

const orderId = Number(route.params.id)
const order = ref<any>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await orderApi.getDetail(orderId)
    order.value = res
  } catch (e) {
    console.error('获取订单详情失败', e)
  } finally {
    loading.value = false
  }
})

async function payOrder() {
  router.push({
    path: '/payment',
    query: { orderId: String(orderId) }
  })
}

async function cancelOrder() {
  try {
    await orderApi.cancel(orderId)
    order.value.status = 'CANCELLED'
  } catch (e: any) {
    ElMessage.error(e.message || '取消失败')
  }
}

async function confirmReceive() {
  try {
    await orderApi.confirm(orderId)
    order.value.status = 'COMPLETED'
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}
</script>

<template>
  <div class="order-detail-page">
    <div class="container">
      <el-button text @click="router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>
      
      <template v-else-if="order">
        <!-- 订单状态 -->
        <div class="order-status-card">
          <div class="status-icon">
            <el-icon v-if="order.status === 'PENDING'" :size="40"><Clock /></el-icon>
            <el-icon v-else-if="order.status === 'PAID'" :size="40"><Money /></el-icon>
            <el-icon v-else-if="order.status === 'SHIPPED'" :size="40"><Box /></el-icon>
            <el-icon v-else-if="order.status === 'COMPLETED'" :size="40"><CircleCheck /></el-icon>
            <el-icon v-else :size="40"><CircleClose /></el-icon>
          </div>
          <div class="status-info">
            <h2>{{ getOrderStatusText(order.status) }}</h2>
            <p v-if="order.status === 'PENDING'">请尽快完成支付</p>
            <p v-else-if="order.status === 'PAID'">等待卖家发货</p>
            <p v-else-if="order.status === 'SHIPPED'">商品已发货，请注意查收</p>
            <p v-else-if="order.status === 'COMPLETED'">交易已完成</p>
            <p v-else>订单已取消</p>
          </div>
        </div>
        
        <!-- 收货信息 -->
        <div class="info-card">
          <h3>收货信息</h3>
          <div class="info-content">
            <p><strong>收货人：</strong>{{ order.receiverName }}</p>
            <p><strong>联系电话：</strong>{{ order.receiverPhone }}</p>
            <p><strong>收货地址：</strong>{{ order.receiverAddress }}</p>
          </div>
        </div>
        
        <!-- 商品清单 -->
        <div class="info-card">
          <h3>商品清单</h3>
          <div class="product-list">
            <div v-for="item in order.items" :key="item.id" class="product-item">
              <img :src="getFirstImage(item.productImage)" alt="" />
              <div class="product-info">
                <h4>{{ item.productName }}</h4>
                <p>数量：{{ item.quantity }}</p>
              </div>
              <div class="product-price">{{ formatPrice(item.price) }}</div>
            </div>
          </div>
        </div>
        
        <!-- 订单信息 -->
        <div class="info-card">
          <h3>订单信息</h3>
          <div class="info-content">
            <p><strong>订单编号：</strong>{{ order.orderNo }}</p>
            <p><strong>下单时间：</strong>{{ formatDate(order.createTime) }}</p>
            <p v-if="order.remark"><strong>备注：</strong>{{ order.remark }}</p>
          </div>
          <div class="total-row">
            <span>订单总额：</span>
            <span class="total-price">{{ formatPrice(order.totalAmount) }}</span>
          </div>
        </div>
        
        <!-- 操作按钮 -->
        <div class="action-bar" v-if="order.status !== 'COMPLETED' && order.status !== 'CANCELLED'">
          <el-button v-if="order.status === 'PENDING'" type="primary" size="large" @click="payOrder">
            去支付
          </el-button>
          <el-button v-if="order.status === 'PENDING'" size="large" @click="cancelOrder">
            取消订单
          </el-button>
          <el-button v-if="order.status === 'SHIPPED'" type="primary" size="large" @click="confirmReceive">
            确认收货
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import { ArrowLeft, Loading, Clock, Money, Box, CircleCheck, CircleClose } from '@element-plus/icons-vue'
export default {
  components: { ArrowLeft, Loading, Clock, Money, Box, CircleCheck, CircleClose }
}
</script>

<style scoped>
.order-detail-page {
  background: #f8f8f8;
  padding: 20px 0;
  min-height: calc(100vh - 200px);
}

.back-btn {
  margin-bottom: 20px;
}

.order-status-card {
  background: linear-gradient(135deg, #FF6B35 0%, #FF8C5A 100%);
  color: #fff;
  padding: 40px;
  border-radius: 12px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.status-icon {
  font-size: 60px;
}

.status-info h2 {
  font-size: 24px;
  margin-bottom: 8px;
}

.status-info p {
  opacity: 0.9;
}

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.info-card h3 {
  font-size: 18px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.info-content p {
  margin-bottom: 12px;
  color: #666;
}

.info-content strong {
  color: #333;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.product-item img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.product-info {
  flex: 1;
}

.product-info h4 {
  font-size: 14px;
  margin-bottom: 4px;
}

.product-info p {
  color: #999;
  font-size: 13px;
}

.product-price {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.total-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  font-size: 16px;
}

.total-price {
  font-size: 28px;
  font-weight: 700;
  color: #FF6B35;
  margin-left: 16px;
}

.action-bar {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.action-bar .el-button {
  width: 120px;
  height: 44px;
}
</style>
