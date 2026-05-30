<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { formatPrice, formatDate, getFirstImage } from '@/utils'

const products = ref<any[]>([])
const loading = ref(true)
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})
const statusFilter = ref<number | null>(null)

onMounted(async () => {
  await loadProducts()
})

async function loadProducts() {
  loading.value = true
  try {
    const res = await adminApi.product.getAll({
      page: pagination.value.current,
      pageSize: pagination.value.pageSize,
      status: statusFilter.value
    })
    products.value = res?.records || []
    pagination.value.total = res?.total || 0
  } catch (e) {
    console.error('获取商品列表失败', e)
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.value.current = page
  loadProducts()
}

async function approveProduct(id: number) {
  try {
    await adminApi.product.approve(id)
    ElMessage.success('审核通过')
    await loadProducts()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function rejectProduct(id: number) {
  ElMessageBox.prompt('请输入拒绝原因', '审核拒绝', {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async ({ value }) => {
    try {
      await adminApi.product.reject(id, value)
      ElMessage.success('已拒绝')
      await loadProducts()
    } catch (e: any) {
      ElMessage.error(e.message || '操作失败')
    }
  }).catch(() => {})
}

async function updateStatus(id: number, status: number) {
  try {
    await adminApi.product.updateStatus(id, status)
    ElMessage.success('状态更新成功')
    await loadProducts()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function deleteProduct(id: number) {
  ElMessageBox.confirm('确定要删除此商品吗？此操作不可恢复。', '删除商品', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await adminApi.product.delete(id)
      ElMessage.success('商品已删除')
      await loadProducts()
    } catch (e: any) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

function getStatusText(status: number) {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已上架',
    2: '已下架',
    3: '已拒绝'
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

function editStock(row: any) {
  ElMessageBox.prompt('请输入新的库存数量', '设置库存', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: String(row.stock ?? 0),
    inputPattern: /^[0-9]+$/,
    inputErrorMessage: '请输入有效的数字'
  }).then(async ({ value }) => {
    try {
      await adminApi.product.updateStock(row.id, parseInt(value))
      ElMessage.success('库存已更新')
      await loadProducts()
    } catch (e: any) {
      ElMessage.error(e.message || '更新库存失败')
    }
  }).catch(() => {})
}
</script>

<template>
  <div class="admin-products">
    <div class="page-header">
      <h1>商品管理</h1>
      <p class="subtitle">管理所有商品信息，包括审核、上下架操作</p>
    </div>
    
    <div class="filter-bar">
      <el-select v-model="statusFilter" placeholder="筛选状态" clearable @change="loadProducts">
        <el-option :value="0" label="待审核" />
        <el-option :value="1" label="已上架" />
        <el-option :value="2" label="已下架" />
        <el-option :value="3" label="已拒绝" />
      </el-select>
    </div>
    
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
    </div>
    
    <div v-else-if="products.length === 0" class="empty-state">
      <div class="icon">
        <el-icon :size="64"><Box /></el-icon>
      </div>
      <h2>暂无商品</h2>
      <p>当前筛选条件下没有商品</p>
    </div>
    
    <div v-else class="product-table">
      <el-table :data="products" style="width: 100%">
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="getFirstImage(row.images)" :alt="row.name" />
              <div class="product-info">
                <p class="name">{{ row.name }}</p>
                <p class="category">{{ row.categoryName }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="120">
          <template #default="{ row }">
            <span class="price">{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="80" />
        <el-table-column label="库存" width="100">
          <template #default="{ row }">
            <el-tag :type="(row.stock ?? 0) > 0 ? 'success' : 'danger'" size="small">
              {{ row.stock ?? 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <!-- 待审核商品：显示通过/拒绝按钮 -->
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="approveProduct(row.id)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="rejectProduct(row.id)">
                拒绝
              </el-button>
            </template>
            <!-- 已上架商品：显示下架按钮 -->
            <template v-else-if="row.status === 1">
              <el-button size="small" @click="updateStatus(row.id, 2)">
                下架
              </el-button>
            </template>
            <!-- 已下架商品：显示上架按钮 -->
            <template v-else-if="row.status === 2">
              <el-button type="primary" size="small" @click="updateStatus(row.id, 1)">
                上架
              </el-button>
            </template>
            <!-- 已拒绝商品：也可以重新上架 -->
            <template v-else-if="row.status === 3">
              <el-button type="primary" size="small" @click="updateStatus(row.id, 1)">
                上架
              </el-button>
            </template>
            <!-- 所有状态都可以设置库存 -->
            <el-button type="warning" size="small" @click="editStock(row)">
              设置库存
            </el-button>
            <!-- 所有状态都可以删除 -->
            <el-button type="danger" size="small" text @click="deleteProduct(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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
  </div>
</template>

<script lang="ts">
import { Loading, Box } from '@element-plus/icons-vue'
export default {
  components: { Loading, Box }
}
</script>

<style scoped>
.admin-products {
  padding: 0;
}

.page-header {
  margin-bottom: var(--space-6);
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

.filter-bar {
  margin-bottom: var(--space-5);
}

.product-table {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  border: 1px solid var(--color-border-light);
}

.product-cell {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.product-cell img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.product-info .name {
  font-weight: var(--font-medium);
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-info .category {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.price {
  color: var(--color-primary);
  font-weight: var(--font-semibold);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--space-6);
}

.empty-state {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-16) var(--space-5);
  text-align: center;
  border: 1px solid var(--color-border-light);
}

.empty-state .icon {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.3;
}

.empty-state h2 {
  font-family: var(--font-heading);
  font-size: var(--text-xl);
  margin-bottom: var(--space-2);
  color: var(--color-text-primary);
}

.empty-state p {
  color: var(--color-text-muted);
}
</style>
