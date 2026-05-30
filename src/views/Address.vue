<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addressApi } from '@/api'

const addresses = ref<any[]>([])
const loading = ref(true)
const showDialog = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const form = ref({
  receiverName: '',
  phone: '',
  province: '',
  detailAddress: '',
  isDefault: false
})

onMounted(async () => {
  await loadAddresses()
})

async function loadAddresses() {
  loading.value = true
  try {
    const res = await addressApi.getList()
    addresses.value = res || []
  } catch (e) {
    console.error('获取地址失败', e)
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  form.value = {
    receiverName: '',
    phone: '',
    province: '',
    detailAddress: '',
    isDefault: false
  }
  showDialog.value = true
}

function openEditDialog(addr: any) {
  isEdit.value = true
  currentId.value = addr.id
  form.value = {
    receiverName: addr.receiverName,
    phone: addr.phone,
    province: addr.province || addr.detailAddress || '',
    detailAddress: '',
    isDefault: addr.isDefault === 1
  }
  showDialog.value = true
}

async function submitForm() {
  if (!form.value.receiverName || !form.value.phone || !form.value.detailAddress) {
    ElMessage.warning('请填写完整的收货信息')
    return
  }

  try {
    if (isEdit.value && currentId.value) {
      await addressApi.update(currentId.value, form.value)
      ElMessage.success('修改成功')
    } else {
      await addressApi.add(form.value)
      ElMessage.success('添加成功')
    }
    showDialog.value = false
    await loadAddresses()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function deleteAddress(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await addressApi.delete(id)
    ElMessage.success('删除成功')
    await loadAddresses()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

async function setDefault(id: number) {
  try {
    await addressApi.setDefault(id)
    ElMessage.success('设置成功')
    await loadAddresses()
  } catch (e: any) {
    ElMessage.error(e.message || '设置失败')
  }
}
</script>

<template>
  <div class="address-page">
    <div class="container">
      <div class="page-header">
        <h1>收货地址</h1>
        <el-button type="primary" @click="openAddDialog">添加新地址</el-button>
      </div>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>

      <div v-else-if="addresses.length === 0" class="empty-state">
        <div class="icon">📍</div>
        <h2>暂无收货地址</h2>
        <p>添加收货地址方便下单</p>
        <el-button type="primary" @click="openAddDialog">添加地址</el-button>
      </div>

      <div v-else class="address-list">
        <div v-for="addr in addresses" :key="addr.id" class="address-card">
          <div class="address-content">
            <div class="address-main">
              <p class="receiver">{{ addr.receiverName }} {{ addr.phone }}</p>
              <p class="full-address">{{ addr.province || addr.detailAddress }}</p>
            </div>
            <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
          </div>
          <div class="address-actions">
            <el-button text type="primary" @click="openEditDialog(addr)">编辑</el-button>
            <el-button text type="danger" @click="deleteAddress(addr.id)">删除</el-button>
            <el-button text v-if="addr.isDefault !== 1" @click="setDefault(addr.id)">设为默认</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑地址' : '添加地址'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="form.detailAddress" type="textarea" :rows="3" placeholder="请输入收货地址" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
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
.address-page {
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

.address-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.address-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.address-content {
  display: flex;
  gap: 16px;
}

.receiver {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.full-address {
  color: #666;
  line-height: 1.6;
}

.default-tag {
  background: #FF6B35;
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.address-actions {
  display: flex;
  gap: 8px;
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
</style>
