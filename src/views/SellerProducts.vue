<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api'
import { formatPrice, getFirstImage } from '@/utils'

const route = useRoute()
const router = useRouter()

const sellerId = ref<number>(0)
const sellerName = ref<string>('')
const products = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  sellerId.value = Number(route.params.sellerId)
  
  if (!sellerId.value) {
    ElMessage.error('无效的卖家信息')
    router.push('/')
    return
  }
  
  await loadSellerProducts()
})

async function loadSellerProducts() {
  loading.value = true
  try {
    // 调用后端API获取指定卖家的商品
    const res = await productApi.getByUser(sellerId.value)
    products.value = res || []
    // 从第一个商品获取卖家名称，如果没有则使用默认名称
    if (products.value.length > 0 && products.value[0].sellerNickname) {
      sellerName.value = products.value[0].sellerNickname
    } else {
      sellerName.value = '该卖家'
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取商品列表失败')
  } finally {
    loading.value = false
  }
}

function goToProduct(id: number) {
  router.push(`/product/${id}`)
}
</script>

<template>
  <div class="seller-products-page">
    <div class="container">
      <div class="page-header">
        <div class="seller-info">
          <h1>{{ sellerName }}的商品</h1>
          <span class="count">共 {{ products.length }} 件商品</span>
        </div>
        <el-button @click="router.back()">返回</el-button>
      </div>
      
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>
      
      <div v-else-if="products.length === 0" class="empty-state">
        <div class="icon"><el-icon :size="64"><Box /></el-icon></div>
        <h2>暂无商品</h2>
        <p>该卖家还没有发布任何商品</p>
      </div>
      
      <div v-else class="product-grid">
        <div 
          v-for="product in products" 
          :key="product.id"
          class="product-card"
          @click="goToProduct(product.id)"
        >
          <div class="image-wrapper">
            <img 
              :src="getFirstImage(product.images) || '/images/products/default-product.svg'" 
              :alt="product.name"
            />
            <span v-if="product.conditionLevel" class="condition-tag">
              {{ product.conditionLevel }}
            </span>
          </div>
          <div class="info">
            <h3 class="name">{{ product.name }}</h3>
            <div class="price-row">
              <span class="price">{{ formatPrice(product.price) }}</span>
              <span v-if="product.originalPrice" class="original-price">
                {{ formatPrice(product.originalPrice) }}
              </span>
            </div>
            <div class="meta">
              <span class="location">{{ product.location || '未知地点' }}</span>
              <span class="views">{{ product.viewCount || 0 }}次浏览</span>
              <span class="stock-tag" :class="(product.stock ?? 0) > 0 ? 'in-stock' : 'out-stock'">
                库存{{ product.stock ?? 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Loading, Box } from '@element-plus/icons-vue'
export default {
  components: { Loading, Box }
}
</script>

<style scoped>
.seller-products-page {
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

.seller-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
}

.seller-info .count {
  color: #999;
  font-size: 14px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: #f5f5f5;
}

.image-wrapper img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.condition-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(255, 107, 53, 0.9);
  color: #fff;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
}

.info {
  padding: 16px;
}

.name {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.price {
  font-size: 20px;
  font-weight: 600;
  color: #FF6B35;
}

.original-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
  margin-left: 8px;
}

.meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  flex-wrap: wrap;
  gap: 4px;
}

.stock-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
}

.stock-tag.in-stock {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.stock-tag.out-stock {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.loading-container {
  text-align: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 12px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 12px;
}

.empty-state .icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h2 {
  font-size: 20px;
  margin-bottom: 8px;
}

.empty-state p {
  color: #999;
}

@media (max-width: 1200px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
