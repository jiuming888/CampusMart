<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api'

const categories = ref<any[]>([])
const loading = ref(true)
const showDialog = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const form = ref({
  name: '',
  icon: '',
  sort: 0
})

const icons = ['📚', '🏠', '💻', '👗', '💄', '⚽', '📎', '📦']

onMounted(async () => {
  await loadCategories()
})

async function loadCategories() {
  loading.value = true
  try {
    const res = await categoryApi.getList()
    categories.value = res || []
  } catch (e) {
    console.error('获取分类失败', e)
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  form.value = { name: '', icon: '', sort: 0 }
  showDialog.value = true
}

function openEditDialog(cat: any) {
  isEdit.value = true
  currentId.value = cat.id
  form.value = {
    name: cat.name,
    icon: cat.icon,
    sort: cat.sort
  }
  showDialog.value = true
}

async function submitForm() {
  if (!form.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  
  try {
    if (isEdit.value && currentId.value) {
      await categoryApi.update({ id: currentId.value, ...form.value })
      ElMessage.success('修改成功')
    } else {
      await categoryApi.add(form.value)
      ElMessage.success('添加成功')
    }
    showDialog.value = false
    await loadCategories()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function deleteCategory(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await categoryApi.delete(id)
    ElMessage.success('删除成功')
    await loadCategories()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}
</script>

<template>
  <div class="admin-categories">
    <div class="page-header">
      <h1>分类管理</h1>
      <p class="subtitle">管理商品分类</p>
    </div>
    
    <div class="action-bar">
      <el-button type="primary" @click="openAddDialog">添加分类</el-button>
    </div>
    
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
    </div>
    
    <div v-else class="category-list">
      <el-table :data="categories" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="分类" min-width="200">
          <template #default="{ row }">
            <div class="category-cell">
              <span class="icon">{{ row.icon || '📦' }}</span>
              <span class="name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteCategory(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑分类' : '添加分类'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="图标">
          <div class="icon-select">
            <span
              v-for="icon in icons"
              :key="icon"
              class="icon-option"
              :class="{ active: form.icon === icon }"
              @click="form.icon = icon"
            >
              {{ icon }}
            </span>
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Loading } from '@element-plus/icons-vue'
export default {
  components: { Loading }
}
</script>

<style scoped>
.admin-categories {
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

.action-bar {
  margin-bottom: var(--space-5);
}

.category-list {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  border: 1px solid var(--color-border-light);
}

.category-cell {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.category-cell .icon {
  font-size: 24px;
}

.category-cell .name {
  font-weight: var(--font-medium);
}

.icon-select {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.icon-option {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 20px;
  transition: all var(--transition-fast);
}

.icon-option:hover {
  border-color: var(--color-primary);
}

.icon-option.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}
</style>
