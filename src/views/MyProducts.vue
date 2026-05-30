<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi } from '@/api'
import { formatPrice, getFirstImage } from '@/utils'

const router = useRouter()

const products = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  await loadProducts()
})

async function loadProducts() {
  loading.value = true
  try {
    const res = await productApi.getMyProducts({ page: 1, pageSize: 100 })
    products.value = res?.records || []
  } catch (e) {
    console.error('获取商品失败', e)
  } finally {
    loading.value = false
  }
}

function goToProduct(id: number) {
  router.push(`/product/${id}`)
}

function goPublish() {
  router.push('/publish')
}

async function deleteProduct(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await productApi.delete(id)
    ElMessage.success('删除成功')
    await loadProducts()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
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
</script>

<template>
  <div class="my-products-page">
    <div class="container">
      <div class="page-header">
        <h1>我的发布</h1>
        <el-button type="primary" @click="goPublish">发布新商品</el-button>
      </div>
      
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>
      
      <div v-else-if="products.length === 0" class="empty-state">
        <div class="icon"><el-icon :size="64"><Box /></el-icon></div>
        <h2>还没有发布商品</h2>
        <p>快来发布你的闲置物品吧</p>
        <el-button type="primary" @click="goPublish">发布商品</el-button>
      </div>
      
      <div v-else class="product-list">
        <div v-for="product in products" :key="product.id" class="product-card">
          <div class="product-image" @click="goToProduct(product.id)">
            <img :src="getFirstImage(product.images)" :alt="product.name" />
            <span class="status-tag" :class="'status-' + product.status">
              {{ getStatusText(product.status) }}
            </span>
          </div>
          <div class="product-info">
            <h3 @click="goToProduct(product.id)">{{ product.name }}</h3>
            <p class="price">{{ formatPrice(product.price) }}</p>
            <p class="category">{{ product.categoryName }}</p>
            <p class="stock-info" :class="(product.stock ?? 0) > 0 ? 'in-stock' : 'out-stock'">
              库存：{{ product.stock ?? 0 }} 件
            </p>
            <p class="reason" v-if="product.reason">
              <el-icon><Warning /></el-icon>
              {{ product.reason }}
            </p>
          </div>
          <div class="product-actions">
            <el-button size="small" @click="goToProduct(product.id)">查看</el-button>
            <el-button size="small" type="danger" @click="deleteProduct(product.id)">删除</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Loading, Warning, Box } from '@element-plus/icons-vue'
export default {
  components: { Loading, Warning, Box }
}
</script>

<style scoped>
.my-products-page {
  background: #f8f8f8;
  padding: 20px 0;
  min-height: calc(100vh - 200px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.product-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.product-image {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  cursor: pointer;
}

.product-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.status-0 { background: #FAAD14; }
.status-1 { background: #52C41A; }
.status-2 { background: #999; }
.status-3 { background: #F5222D; }

.product-info {
  padding: 16px;
}

.product-info h3 {
  font-size: 14px;
  margin-bottom: 8px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-info h3:hover {
  color: #FF6B35;
}

.price {
  color: #FF6B35;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.category {
  color: #999;
  font-size: 13px;
}

.stock-info {
  font-size: 13px;
  margin-top: 4px;
}

.stock-info.in-stock {
  color: #52c41a;
}

.stock-info.out-stock {
  color: #ff4d4f;
}

.reason {
  margin-top: 8px;
  padding: 8px;
  background: #FFF2F0;
  border-radius: 4px;
  color: #F5222D;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.product-actions {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
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

.empty-state h2 {
  font-size: 20px;
  margin-bottom: 12px;
}

.empty-state p {
  color: #999;
  margin-bottom: 24px;
}

@media (max-width: 1200px) {
  .product-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .product-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
