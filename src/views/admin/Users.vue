<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi, warningApi } from '@/api'
import { formatDate } from '@/utils'

const users = ref<any[]>([])
const loading = ref(true)
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

onMounted(async () => {
  await loadUsers()
})

async function loadUsers() {
  loading.value = true
  try {
    const res = await userApi.getUserList(pagination.value.current, pagination.value.pageSize)
    users.value = res?.records || []
    pagination.value.total = res?.total || 0
  } catch (e) {
    console.error('获取用户列表失败', e)
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.value.current = page
  loadUsers()
}

async function handleSendWarning(row: any) {
  try {
    await ElMessageBox.prompt('请输入警告原因', `对 ${row.nickname || row.username} 发送警告`, {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '警告原因不能为空'
    })
    .then(async ({ value }) => {
      await warningApi.sendWarning(row.id, value)
      ElMessage.success('警告已发送')
    })
  } catch {
    // 用户取消
  }
}

async function handleDisable(row: any) {
  try {
    await ElMessageBox.confirm(`确定要禁用用户 ${row.nickname || row.username} 吗？禁用后该用户将无法登录。`, '提示', {
      confirmButtonText: '确定禁用',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userApi.disable(row.id)
    ElMessage.success('禁用成功')
    loadUsers()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '禁用失败')
    }
  }
}

async function handleEnable(row: any) {
  try {
    await ElMessageBox.confirm(`确定要启用用户 ${row.nickname || row.username} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await userApi.enable(row.id)
    ElMessage.success('启用成功')
    loadUsers()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '启用失败')
    }
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除用户 ${row.nickname || row.username} 吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userApi.deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}
</script>

<template>
  <div class="admin-users">
    <div class="page-header">
      <h1>用户管理</h1>
      <p class="subtitle">查看和管理平台用户</p>
    </div>
    
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
    </div>
    
    <div v-else class="user-table">
      <el-table :data="users" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar || '/images/avatars/default-avatar.svg'" />
              <div class="user-info">
                <p class="nickname">{{ row.nickname || row.username }}</p>
                <p class="username">@{{ row.username }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="最后登录" width="180">
          <template #default="{ row }">
            {{ row.lastLoginTime ? formatDate(row.lastLoginTime) : '暂无记录' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'success'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.role !== 'ADMIN'" 
              type="warning" 
              size="small" 
              link
              @click="handleSendWarning(row)"
            >
              警告
            </el-button>
            <el-button 
              v-if="row.role !== 'ADMIN' && row.status === 1" 
              type="danger" 
              size="small" 
              link
              @click="handleDisable(row)"
            >
              禁用
            </el-button>
            <el-button 
              v-if="row.role !== 'ADMIN' && row.status === 0" 
              type="success" 
              size="small" 
              link
              @click="handleEnable(row)"
            >
              启用
            </el-button>
            <el-button 
              v-if="row.role !== 'ADMIN'" 
              type="danger" 
              size="small" 
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
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
import { Loading } from '@element-plus/icons-vue'
export default {
  components: { Loading }
}
</script>

<style scoped>
.admin-users {
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

.user-table {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  border: 1px solid var(--color-border-light);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.user-info .nickname {
  font-weight: var(--font-medium);
  font-size: var(--text-xs);
}

.user-info .username {
  font-size: 11px;
  color: #999;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
