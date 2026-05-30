<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi } from '@/api'
import { formatPrice, getFirstImage } from '@/utils'

const route = useRoute()
const router = useRouter()

const products = ref<any[]>([])
const loading = ref(true)
const keyword = ref('')
const pagination = ref({
  current: 1,
  pageSize: 12,
  total: 0
})

onMounted(() => {
  keyword.value = (route.query.keyword as string) || ''
  loadProducts()
})

async function loadProducts() {
  loading.value = true
  try {
    const res = await productApi.getList({
      page: pagination.value.current,
      pageSize: pagination.value.pageSize,
      keyword: keyword.value
    })
    products.value = res?.records || []
    pagination.value.total = res?.total || 0
  } catch (e) {
    console.error('搜索失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.current = 1
  router.push({ path: '/search', query: { keyword: keyword.value } })
  loadProducts()
}

function goToProduct(id: number) {
  router.push(`/product/${id}`)
}

function handlePageChange(page: number) {
  pagination.value.current = page
  loadProducts()
}
</script>

<template>
  <div class="search-page">
    <div class="container">
      <div class="page-header">
        <h1>搜索结果</h1>
        <p v-if="keyword" class="keyword">关键词: {{ keyword }}</p>
      </div>
      
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索商品..."
          size="large"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>
      
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>
      
      <div v-else-if="products.length === 0" class="empty-state">
        <div class="icon">🔍</div>
        <p>未找到相关商品</p>
      </div>
      
      <div v-else class="product-grid">
        <div v-for="product in products" :key="product.id" class="product-card" @click="goToProduct(product.id)">
          <div class="image-wrapper">
            <img :src="getFirstImage(product.images)" :alt="product.name" />
            <span v-if="product.conditionLevel" class="condition-tag">{{ product.conditionLevel }}</span>
          </div>
          <div class="info">
            <h3>{{ product.name }}</h3>
            <p class="price">{{ formatPrice(product.price) }}</p>
            <div class="card-meta">
              <span>{{ product.location || '未知地点' }}</span>
              <span class="stock-tag" :class="(product.stock ?? 0) > 0 ? 'in-stock' : 'out-stock'">
                库存{{ product.stock ?? 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="pagination.total > 0" class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          background
          prev-text="上一页"
          next-text="下一页"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Search, Loading } from '@element-plus/icons-vue'
export default {
  components: { Search, Loading }
}
</script>

<style scoped>
.search-page {
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
  margin-bottom: 8px;
}

.keyword {
  color: #FF6B35;
  font-size: 14px;
}

.search-bar {
  margin-bottom: 20px;
}

.search-bar .el-input {
  max-width: 600px;
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

.info h3 {
  font-size: 14px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  color: #FF6B35;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.meta {
  color: #999;
  font-size: 12px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
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

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 20px 0 4px;
}

.pagination :deep(.el-pagination) {
  gap: 10px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #e8ecef;
  border-radius: 24px;
  box-shadow: 0 8px 24px rgba(15, 35, 28, 0.06);
}

.pagination :deep(.btn-prev),
.pagination :deep(.btn-next),
.pagination :deep(.el-pager li) {
  min-width: 44px;
  height: 44px;
  padding: 0 16px;
  border-radius: 999px !important;
  font-size: 14px;
  font-weight: 600;
  color: #5f6f68;
  background: #f4f7f5 !important;
  transition: all 0.2s ease;
}

.pagination :deep(.btn-prev),
.pagination :deep(.btn-next) {
  min-width: 88px;
}

.pagination :deep(.btn-prev:hover),
.pagination :deep(.btn-next:hover),
.pagination :deep(.el-pager li:hover) {
  color: #22c55e;
  background: #dcfce7 !important;
}

.pagination :deep(.el-pager li.is-active) {
  color: #fff !important;
  background: #22c55e !important;
  box-shadow: 0 8px 18px rgba(34, 197, 94, 0.24);
}

.pagination :deep(button:disabled) {
  color: #a3aaa6 !important;
  background: #f8f8f8 !important;
  cursor: not-allowed;
  opacity: 0.65;
}

.pagination :deep(button:focus-visible),
.pagination :deep(.el-pager li:focus-visible) {
  outline: 3px solid rgba(34, 197, 94, 0.22);
  outline-offset: 2px;
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

.empty-state p {
  color: #999;
  font-size: 16px;
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
