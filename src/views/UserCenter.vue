<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi, productApi, warningApi, orderApi, uploadApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatPrice, formatDate, getOrderStatusText, getOrderStatusType, getFirstImage } from '@/utils'
import { Camera } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('info')
const userInfo = ref<any>(null)
const myProducts = ref<any[]>([])
const loading = ref(false)

// 头像上传相关
const avatarInputRef = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)

// 我的警告
const warnings = ref<any[]>([])
const warningLoading = ref(false)

// 我的订单
const orders = ref<any[]>([])
const orderLoading = ref(false)
const orderActiveTab = ref('all')

const orderStatusOptions = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: 'PENDING' },
  { label: '待发货', value: 'PAID' },
  { label: '待收货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

// 编辑信息
const editForm = ref({
  nickname: '',
  phone: '',
  email: ''
})
const showEditDialog = ref(false)

// 修改密码
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const showPasswordDialog = ref(false)

onMounted(async () => {
  await loadUserInfo()
  await loadMyProducts()
  await loadWarnings()
  await loadOrders()
})

async function loadUserInfo() {
  try {
    const res = await userApi.getUserInfo()
    userInfo.value = res
    editForm.value = {
      nickname: res.nickname || '',
      phone: res.phone || '',
      email: res.email || ''
    }
  } catch (e) {
    console.error('获取用户信息失败', e)
  }
}

async function loadMyProducts() {
  try {
    const res = await productApi.getMyProducts({ page: 1, pageSize: 100 })
    myProducts.value = res?.records || []
  } catch (e) {
    console.error('获取我的发布失败', e)
  }
}

async function loadWarnings() {
  warningLoading.value = true
  try {
    const res = await warningApi.getMyWarnings()
    console.log('个人中心-警告数据:', res)
    warnings.value = res || []
  } catch (e) {
    console.error('获取警告失败', e)
  } finally {
    warningLoading.value = false
  }
}

async function markAsRead(id: number) {
  try {
    await warningApi.markAsRead(id)
    await loadWarnings()
  } catch (e) {
    console.error('标记已读失败', e)
  }
}

async function markAllAsRead() {
  try {
    await warningApi.markAllAsRead()
    await loadWarnings()
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    console.error('标记全部已读失败', e)
  }
}

// 加载我的订单
async function loadOrders() {
  orderLoading.value = true
  try {
    const params: any = {}
    if (orderActiveTab.value !== 'all') {
      params.status = orderActiveTab.value
    }
    const res = await orderApi.getList({ page: 1, pageSize: 100, ...params })
    orders.value = res?.records || []
  } catch (e) {
    console.error('获取订单失败', e)
  } finally {
    orderLoading.value = false
  }
}

function onOrderTabChange() {
  loadOrders()
}

function goToOrderDetail(id: number) {
  router.push(`/order/${id}`)
}

async function deleteOrder(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await orderApi.delete(id)
    ElMessage.success('删除成功')
    loadOrders()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

async function deleteAllOrders() {
  try {
    await ElMessageBox.confirm('确定要删除全部订单吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await orderApi.deleteAll()
    ElMessage.success('删除成功')
    loadOrders()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

async function updateUserInfo() {
  try {
    await userApi.updateUserInfo(editForm.value)
    ElMessage.success('修改成功')
    showEditDialog.value = false
    await loadUserInfo()
  } catch (e: any) {
    ElMessage.error(e.message || '修改失败')
  }
}

async function updatePassword() {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }
  
  try {
    await userApi.updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    showPasswordDialog.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e: any) {
    ElMessage.error(e.message || '修改失败')
  }
}

function goToProduct(id: number) {
  router.push(`/product/${id}`)
}

function getStatusText(status: number) {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已上架',
    2: '已下架',
    3: '审核拒绝'
  }
  return map[status] || '未知'
}

function getStatusType(status: number) {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'info',
    3: 'danger'
  }
  return map[status] || 'info'
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

// 头像上传相关函数
function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

async function handleAvatarChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning('请上传 JPG、PNG、GIF 或 WebP 格式的图片')
    return
  }

  // 验证文件大小（最大 2MB）
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  avatarUploading.value = true
  try {
    const res = await uploadApi.uploadImage(file, 'avatar') as string
    // 更新用户头像
    await userApi.updateUserInfo({ avatar: res })
    // 更新本地显示
    userInfo.value = { ...userInfo.value, avatar: res }
    // 更新 store 中的用户信息
    userStore.setUserInfo({ ...userStore.userInfo!, avatar: res })
    ElMessage.success('头像上传成功')
  } catch (e: any) {
    ElMessage.error(e.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
    // 清空 input 值，允许重复选择同一文件
    if (avatarInputRef.value) {
      avatarInputRef.value.value = ''
    }
  }
}
</script>

<template>
  <div class="user-center-page">
    <div class="container">
      <div class="page-header">
        <h1>个人中心</h1>
      </div>
      
      <div class="content">
        <!-- 侧边导航 -->
        <aside class="sidebar">
          <div class="user-card">
            <div class="avatar-wrapper" @click="triggerAvatarUpload">
              <el-avatar :size="80" :src="userInfo?.avatar || '/images/avatars/default-avatar.svg'" />
              <div class="avatar-mask">
                <el-icon><Camera /></el-icon>
              </div>
            </div>
            <input
              ref="avatarInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleAvatarChange"
            />
            <h3>{{ userInfo?.nickname || userInfo?.username }}</h3>
            <p>{{ userInfo?.email || '未设置邮箱' }}</p>
          </div>
          
          <div class="nav-list">
            <div 
              class="nav-item" 
              :class="{ active: activeTab === 'info' }"
              @click="activeTab = 'info'"
            >
              个人信息
            </div>
            <div 
              class="nav-item" 
              :class="{ active: activeTab === 'products' }"
              @click="activeTab = 'products'"
            >
              我的发布
            </div>
            <div 
              class="nav-item" 
              :class="{ active: activeTab === 'orders' }"
              @click="activeTab = 'orders'"
            >
              我的订单
            </div>
            <div 
              class="nav-item" 
              :class="{ active: activeTab === 'warnings' }"
              @click="activeTab = 'warnings'"
            >
              管理员警告
              <el-badge v-if="warnings.filter(w => !w.isRead).length" :value="warnings.filter(w => !w.isRead).length" :max="99" />
            </div>
            <div 
              class="nav-item" 
              :class="{ active: activeTab === 'security' }"
              @click="activeTab = 'security'"
            >
              安全设置
            </div>
          </div>
        </aside>
        
        <!-- 主内容 -->
        <main class="main-content">
          <!-- 个人信息 -->
          <div v-if="activeTab === 'info'" class="panel">
            <h2>个人信息</h2>
            <div class="info-list">
              <div class="info-item">
                <span class="label">用户名</span>
                <span class="value">{{ userInfo?.username }}</span>
              </div>
              <div class="info-item">
                <span class="label">昵称</span>
                <span class="value">{{ userInfo?.nickname || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">手机号</span>
                <span class="value">{{ userInfo?.phone || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">邮箱</span>
                <span class="value">{{ userInfo?.email || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">注册时间</span>
                <span class="value">{{ userInfo?.createTime || '-' }}</span>
              </div>
            </div>
            <div class="action-row">
              <el-button type="primary" @click="showEditDialog = true">编辑资料</el-button>
            </div>
          </div>
          
          <!-- 我的发布 -->
          <div v-if="activeTab === 'products'" class="panel">
            <div class="panel-header">
              <h2>我的发布</h2>
              <el-button type="primary" size="small" @click="router.push('/publish')">
                发布新商品
              </el-button>
            </div>
            
            <div v-if="myProducts.length === 0" class="empty-state">
              <p>暂无发布的商品</p>
              <el-button type="primary" @click="router.push('/publish')">发布商品</el-button>
            </div>
            
            <div v-else class="product-list">
              <div v-for="product in myProducts" :key="product.id" class="product-item" @click="goToProduct(product.id)">
                <div class="product-status">
                  <el-tag :type="getStatusType(product.status)" size="small">
                    {{ getStatusText(product.status) }}
                  </el-tag>
                </div>
                <p class="product-name">{{ product.name }}</p>
                <p class="product-price">{{ formatPrice(product.price) }}</p>
                <p class="product-reason" v-if="product.reason">
                  拒绝原因：{{ product.reason }}
                </p>
              </div>
            </div>
          </div>
          
          <!-- 我的订单 -->
          <div v-if="activeTab === 'orders'" class="panel">
            <div class="panel-header">
              <h2>我的订单</h2>
              <el-button v-if="orders.length > 0" type="danger" size="small" @click="deleteAllOrders">
                一键删除全部订单
              </el-button>
            </div>
            
            <div class="order-tabs">
              <el-radio-group v-model="orderActiveTab" @change="onOrderTabChange">
                <el-radio-button v-for="opt in orderStatusOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </el-radio-button>
              </el-radio-group>
            </div>
            
            <div v-if="orderLoading" class="loading-container">
              <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            </div>
            
            <div v-else-if="orders.length === 0" class="empty-state">
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
                    <el-button size="small" type="danger" plain @click.stop="deleteOrder(order.id)">删除</el-button>
                    <el-button size="small" @click.stop="goToOrderDetail(order.id)">查看详情</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 管理员警告 -->
          <div v-if="activeTab === 'warnings'" class="panel">
            <div class="panel-header">
              <h2>管理员警告</h2>
              <el-button v-if="warnings.some(w => !w.isRead)" size="small" @click="markAllAsRead">
                全部标为已读
              </el-button>
            </div>
            
            <div v-if="warningLoading" class="loading-container">
              <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            </div>
            
            <div v-else-if="warnings.length === 0" class="empty-state">
              <p>暂无管理员警告</p>
            </div>
            
            <div v-else class="warning-list">
              <div 
                v-for="warning in warnings" 
                :key="warning.id" 
                class="warning-item"
                :class="{ unread: !warning.isRead }"
                @click="markAsRead(warning.id)"
              >
                <div class="warning-header">
                  <span class="warning-admin">管理员</span>
                  <span class="warning-time">{{ formatDate(warning.createTime) }}</span>
                </div>
                <div class="warning-content">{{ warning.reason }}</div>
                <div v-if="!warning.isRead" class="unread-badge">未读</div>
              </div>
            </div>
          </div>
          
          <!-- 安全设置 -->
          <div v-if="activeTab === 'security'" class="panel">
            <h2>安全设置</h2>
            <div class="security-item">
              <div class="security-info">
                <h4>登录密码</h4>
                <p>定期更换密码可以保护账户安全</p>
              </div>
              <el-button @click="showPasswordDialog = true">修改密码</el-button>
            </div>
          </div>
        </main>
      </div>
    </div>
    
    <!-- 编辑资料对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑资料" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="updateUserInfo">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="500px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="updatePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-center-page {
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

.content {
  display: flex;
  gap: 20px;
}

/* 侧边栏 */
.sidebar {
  width: 240px;
  flex-shrink: 0;
}

.user-card {
  background: #fff;
  border-radius: 12px;
  padding: 30px 20px;
  text-align: center;
  margin-bottom: 20px;
}

.user-card h3 {
  margin: 16px 0 8px;
  font-size: 18px;
}

.user-card p {
  color: #999;
  font-size: 14px;
}

.nav-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.nav-item {
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.3s;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background: #fafafa;
}

.nav-item.active {
  background: #FFF7F5;
  border-left-color: #FF6B35;
  color: #FF6B35;
}

.nav-item :deep(.el-badge) {
  margin-left: 8px;
}

/* 主内容 */
.main-content {
  flex: 1;
}

.panel {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.panel h2 {
  font-size: 18px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-header h2 {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.info-list {
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  width: 100px;
  color: #999;
}

.info-item .value {
  flex: 1;
  color: #333;
}

.action-row {
  display: flex;
  justify-content: flex-start;
}

/* 我的发布 */
.product-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.product-item {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.product-item:hover {
  background: #f5f5f5;
}

.product-status {
  margin-bottom: 8px;
}

.product-name {
  font-size: 14px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  color: #FF6B35;
  font-weight: 600;
}

.product-reason {
  margin-top: 8px;
  font-size: 12px;
  color: #F5222D;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state p {
  margin-bottom: 20px;
}

/* 安全设置 */
.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.security-info h4 {
  margin-bottom: 4px;
}

.security-info p {
  color: #999;
  font-size: 14px;
}

@media (max-width: 768px) {
  .content {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
  }
  
  .product-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 警告列表 */
.loading-container {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.warning-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.warning-item {
  position: relative;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border-left: 4px solid #E6A23C;
  cursor: pointer;
  transition: all 0.3s;
}

.warning-item:hover {
  background: #f5f5f5;
}

.warning-item.unread {
  background: #FFF7F5;
  border-left-color: #F56C6C;
}

.warning-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.warning-admin {
  font-weight: 600;
  color: #333;
}

.warning-time {
  font-size: 12px;
  color: #999;
}

.warning-content {
  color: #666;
  line-height: 1.6;
}

/* 我的订单样式 */
.order-tabs {
  margin: 16px 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
}

.order-no {
  font-size: 13px;
  color: #666;
}

.order-items {
  padding: 0 16px;
}

.order-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
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
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
}

.order-item .item-info {
  flex: 1;
  margin-left: 12px;
}

.order-item .item-info h3 {
  font-size: 14px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-item .item-info p {
  color: #999;
  font-size: 12px;
}

.item-price {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.order-info .total {
  font-size: 13px;
}

.order-info .total strong {
  color: #FF6B35;
  font-size: 16px;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.unread-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 2px 8px;
  background: #F56C6C;
  color: #fff;
  font-size: 12px;
  border-radius: 4px;
}
</style>
