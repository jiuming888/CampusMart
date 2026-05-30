<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi, productApi } from '@/api'
import { formatPrice, getFirstImage } from '@/utils'

const router = useRouter()

const categoryId = ref<number | string>(0)
const categories = ref<any[]>([])
const products = ref<any[]>([])
const loading = ref(true)
const pagination = ref({
  current: 1,
  pageSize: 12,
  total: 0
})

// 筛选条件
const filters = ref({
  keyword: '',
  minPrice: '',
  maxPrice: '',
  conditionLevel: '',
  sort: 'create_time'
})

const conditionOptions = [
  { label: '全新', value: '全新' },
  { label: '几乎全新', value: '几乎全新' },
  { label: '轻微使用', value: '轻微使用' },
  { label: '明显使用', value: '明显使用' }
]

const sortOptions = [
  { label: '最新发布', value: 'create_time' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' }
]

onMounted(async () => {
  // 获取URL参数
  const routeParams = new URLSearchParams(window.location.search)
  categoryId.value = routeParams.get('categoryId') || 0
  
  await loadCategories()
  await loadProducts()
})

async function loadCategories() {
  try {
    const res = await categoryApi.getList()
    categories.value = res || []
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.current,
      pageSize: pagination.value.pageSize
    }
    
    if (categoryId.value && categoryId.value !== '0') {
      params.categoryId = categoryId.value
    }
    if (filters.value.keyword) {
      params.keyword = filters.value.keyword
    }
    if (filters.value.minPrice) {
      params.minPrice = filters.value.minPrice
    }
    if (filters.value.maxPrice) {
      params.maxPrice = filters.value.maxPrice
    }
    if (filters.value.conditionLevel) {
      params.conditionLevel = filters.value.conditionLevel
    }
    
    const res = await productApi.getList(params)
    let data = res?.records || []
    
    // 排序
    if (filters.value.sort === 'price_asc') {
      data.sort((a: any, b: any) => Number(a.price) - Number(b.price))
    } else if (filters.value.sort === 'price_desc') {
      data.sort((a: any, b: any) => Number(b.price) - Number(a.price))
    }
    
    products.value = data
    pagination.value.total = res?.total || data.length
  } catch (e) {
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

function selectCategory(id: number | string) {
  categoryId.value = id
  pagination.value.current = 1
  loadProducts()
}

function handleSearch() {
  pagination.value.current = 1
  loadProducts()
}

function resetFilters() {
  filters.value = {
    keyword: '',
    minPrice: '',
    maxPrice: '',
    conditionLevel: '',
    sort: 'create_time'
  }
  pagination.value.current = 1
  loadProducts()
}

function handlePageChange(page: number) {
  pagination.value.current = page
  loadProducts()
}

function goToProduct(id: number) {
  router.push(`/product/${id}`)
}

function getCurrentCategoryName() {
  if (categoryId.value === '0' || !categoryId.value) {
    return '全部商品'
  }
  const cat = categories.value.find(c => c.id === Number(categoryId.value))
  return cat?.name || '商品分类'
}
</script>

<template>
  <div class="category-page">
    <div class="container">
      <div class="page-header">
        <h1>{{ getCurrentCategoryName() }}</h1>
        <span class="total">共 {{ pagination.total }} 件商品</span>
      </div>
      
      <div class="content">
        <!-- 左侧分类栏 -->
        <aside class="sidebar">
          <div class="category-list">
            <div 
              class="category-item"
              :class="{ active: categoryId === '0' || categoryId === 0 }"
              @click="selectCategory(0)"
            >
              全部商品
            </div>
            <div 
              v-for="cat in categories" 
              :key="cat.id"
              class="category-item"
              :class="{ active: categoryId === cat.id }"
              @click="selectCategory(cat.id)"
            >
              {{ cat.name }}
            </div>
          </div>
        </aside>
        
        <!-- 右侧商品列表 -->
        <main class="main-content">
          <!-- 筛选栏 -->
          <div class="filter-bar">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索商品名称"
              class="search-input"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
            
            <div class="filter-group">
              <el-select v-model="filters.conditionLevel" placeholder="新旧程度" clearable>
                <el-option 
                  v-for="opt in conditionOptions" 
                  :key="opt.value" 
                  :label="opt.label" 
                  :value="opt.value" 
                />
              </el-select>
              
              <el-select v-model="filters.sort">
                <el-option 
                  v-for="opt in sortOptions" 
                  :key="opt.value" 
                  :label="opt.label" 
                  :value="opt.value" 
                />
              </el-select>
            </div>
            
            <el-button @click="resetFilters">重置</el-button>
          </div>
          
          <!-- 商品网格 -->
          <div v-if="loading" class="loading-container">
            <el-icon class="is-loading" :size="40"><Loading /></el-icon>
          </div>
          
          <div v-else-if="products.length === 0" class="empty-state">
            <div class="icon"><el-icon :size="64"><Box /></el-icon></div>
            <p>暂无商品</p>
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
                  <span class="stock-tag" :class="(product.stock ?? 0) > 0 ? 'in-stock' : 'out-stock'">
                    库存{{ product.stock ?? 0 }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 分页 -->
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
        </main>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Search, Loading, Box } from '@element-plus/icons-vue'
export default {
  components: { Search, Loading, Box }
}
</script>

<style scoped>
.category-page {
  background: var(--color-background);
  padding: 20px 0;
  min-height: calc(100vh - 200px);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
}

.page-header .total {
  color: var(--color-text-muted);
  font-size: 14px;
}

.content {
  display: flex;
  gap: 20px;
}

/* 侧边栏 */
.sidebar {
  width: 200px;
  flex-shrink: 0;
}

.category-list {
  background: var(--color-surface);
  border-radius: 12px;
  padding: 16px;
  position: sticky;
  top: 90px;
}

.category-item {
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--color-text-secondary);
}

.category-item:hover {
  background: var(--color-muted);
  color: var(--color-text-primary);
}

.category-item.active {
  background: var(--color-primary);
  color: #fff;
}

/* 主内容 */
.main-content {
  flex: 1;
}

.filter-bar {
  background: var(--color-surface);
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 20px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.search-input {
  width: 300px;
}

.filter-group {
  display: flex;
  gap: 12px;
  flex: 1;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-base);
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(34, 197, 94, 0.38);
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: var(--color-muted);
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
  background: rgba(34, 197, 94, 0.92);
  color: #fff;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
}

.info {
  padding: 16px;
}

.name {
  font-size: 14px;
  color: var(--color-text-primary);
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
  color: var(--color-accent);
}

.original-price {
  font-size: 12px;
  color: var(--color-text-muted);
  text-decoration: line-through;
  margin-left: 8px;
}

.meta {
  font-size: 12px;
  color: var(--color-text-muted);
  display: flex;
  justify-content: space-between;
}

.stock-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
}

.stock-tag.in-stock {
  background: var(--color-success-light);
  color: var(--color-success);
  border: 1px solid #BBF7D0;
}

.stock-tag.out-stock {
  background: var(--color-error-light);
  color: var(--color-error);
  border: 1px solid #FECACA;
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
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-sm);
}

.pagination :deep(.btn-prev),
.pagination :deep(.btn-next),
.pagination :deep(.el-pager li) {
  min-width: 44px;
  height: 44px;
  padding: 0 16px;
  border-radius: var(--radius-full) !important;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-muted) !important;
  transition: all var(--transition-fast);
}

.pagination :deep(.btn-prev),
.pagination :deep(.btn-next) {
  min-width: 88px;
}

.pagination :deep(.btn-prev:hover),
.pagination :deep(.btn-next:hover),
.pagination :deep(.el-pager li:hover) {
  color: var(--color-primary);
  background: var(--color-primary-light) !important;
}

.pagination :deep(.el-pager li.is-active) {
  color: #fff !important;
  background: var(--color-primary) !important;
  box-shadow: 0 8px 18px rgba(34, 197, 94, 0.24);
}

.pagination :deep(button:disabled) {
  color: var(--color-text-muted) !important;
  background: var(--color-background) !important;
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
  background: var(--color-surface);
  border-radius: 12px;
}

.empty-state .icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state p {
  color: var(--color-text-muted);
  font-size: 16px;
}

@media (max-width: 1200px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .filter-bar {
    flex-wrap: wrap;
  }
  
  .search-input {
    width: 100%;
  }
}
</style>
