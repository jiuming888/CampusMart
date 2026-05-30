<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'
import { formatPrice, formatDate, getOrderStatusText, getOrderStatusType, getFirstImage } from '@/utils'

const userList = ref<any[]>([])
const userLoading = ref(true)
const selectedUser = ref<any>(null)
const orders = ref<any[]>([])
const orderLoading = ref(false)
const pagination = ref({ current: 1, pageSize: 10, total: 0 })

onMounted(async () => {
  await loadUsers()
})

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await adminApi.order.getUsers()
    userList.value = res || []
  } catch (e) {
    console.error('获取用户列表失败', e)
  } finally {
    userLoading.value = false
  }
}

async function selectUser(user: any) {
  selectedUser.value = user
  pagination.value.current = 1
  await loadUserOrders()
}

async function loadUserOrders() {
  if (!selectedUser.value) return
  orderLoading.value = true
  try {
    const res = await adminApi.order.getUserOrders(selectedUser.value.id, {
      page: pagination.value.current,
      pageSize: pagination.value.pageSize
    })
    orders.value = res?.records || []
    pagination.value.total = res?.total || 0
  } catch (e) {
    console.error('获取用户订单失败', e)
  } finally {
    orderLoading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.value.current = page
  loadUserOrders()
}
</script>

<template>
  <div class="admin-orders">
    <div class="page-header">
      <h1>订单管理</h1>
      <p class="subtitle">查看所有用户的已支付订单</p>
    </div>

    <div class="orders-layout">
      <!-- 左侧用户列表 -->
      <div class="user-panel">
        <div class="panel-title">买家列表</div>
        <div v-if="userLoading" class="loading-container">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        </div>
        <div v-else-if="userList.length === 0" class="empty-state">
          暂无订单用户
        </div>
        <div v-else class="user-list">
          <div
            v-for="user in userList"
            :key="user.id"
            class="user-item"
            :class="{ active: selectedUser?.id === user.id }"
            @click="selectUser(user)"
          >
            <el-avatar :size="40" :src="user.avatar">
              {{ user.nickname?.[0] || user.username?.[0] || '?' }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ user.nickname || user.username }}</div>
              <div class="user-stats">
                <span>{{ user.orderCount }}笔订单</span>
                <span class="amount">¥{{ formatPrice(user.totalAmount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧订单列表 -->
      <div class="order-panel">
        <template v-if="selectedUser">
          <div class="order-header">
            <span>{{ selectedUser.nickname || selectedUser.username }} 的订单</span>
            <el-button text @click="selectedUser = null">取消选择</el-button>
          </div>

          <div v-if="orderLoading" class="loading-container">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          </div>
          <div v-else-if="orders.length === 0" class="empty-state">
            暂无订单
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
                <div v-for="item in order.items" :key="item.id" class="order-item">
                  <img :src="getFirstImage(item.productImage)" alt="" />
                  <div class="item-info">
                    <p class="name">{{ item.productName }}</p>
                    <p class="quantity">x{{ item.quantity }}</p>
                  </div>
                  <div class="item-price">{{ formatPrice(item.price) }}</div>
                </div>
              </div>

              <div class="order-footer">
                <div class="order-info">
                  <p>收货人：{{ order.receiverName }} {{ order.receiverPhone }}</p>
                  <p>地址：{{ order.receiverAddress }}</p>
                  <p>下单时间：{{ formatDate(order.createTime) }}</p>
                </div>
                <div class="order-total">
                  订单总额：<span>{{ formatPrice(order.totalAmount) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="pagination.total > 0" class="pagination">
            <el-pagination
              v-model:current-page="pagination.current"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              layout="prev, pager, next"
              @current-change="handlePageChange"
            />
          </div>
        </template>
        <div v-else class="empty-state">
          <p>请从左侧选择一个用户</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Loading } from '@element-plus/icons-vue'
export default {
  components: { Loading }
}
</script>

<style scoped>
.admin-orders {
  height: calc(100vh - 64px - var(--space-12));
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: var(--space-5);
}

.page-header h1 {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
}

.subtitle {
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
}

.orders-layout {
  display: flex;
  gap: var(--space-4);
  flex: 1;
  overflow: hidden;
}

.user-panel {
  width: 280px;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border-light);
}

.panel-title {
  padding: var(--space-4) var(--space-5);
  font-weight: var(--font-semibold);
  font-size: var(--text-base);
  border-bottom: 1px solid var(--color-border-light);
  background: var(--color-muted);
  font-family: var(--font-heading);
}

.user-list {
  flex: 1;
  overflow-y: auto;
}

.user-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 14px var(--space-5);
  cursor: pointer;
  transition: background var(--transition-fast);
  border-bottom: 1px solid var(--color-border-light);
}

.user-item:hover {
  background: var(--color-muted);
}

.user-item.active {
  background: var(--color-primary-light);
  border-left: 3px solid var(--color-primary);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-stats {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
}

.user-stats .amount {
  color: var(--color-primary);
  font-weight: var(--font-medium);
}

.order-panel {
  flex: 1;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border-light);
}

.order-panel > .order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
  font-weight: var(--font-semibold);
}

.order-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4) var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.order-card {
  background: var(--color-muted);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.order-card .order-header {
  padding: var(--space-3) var(--space-4);
  background: var(--color-border-light);
  border-bottom: none;
}

.order-no {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.order-items {
  padding: var(--space-3) var(--space-4);
}

.order-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 6px 0;
}

.order-item img {
  width: 45px;
  height: 45px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.order-item .item-info {
  flex: 1;
}

.order-item .name {
  font-size: var(--text-xs);
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-item .quantity {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.order-item .item-price {
  font-weight: var(--font-medium);
  font-size: var(--text-xs);
}

.order-footer {
  display: flex;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  background: var(--color-border-light);
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.order-total span {
  font-size: var(--text-base);
  color: var(--color-primary);
  font-weight: var(--font-semibold);
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--color-text-muted);
  flex-direction: column;
  gap: var(--space-2);
}

.pagination {
  display: flex;
  justify-content: center;
  padding: var(--space-4);
  border-top: 1px solid var(--color-border-light);
}
</style>
