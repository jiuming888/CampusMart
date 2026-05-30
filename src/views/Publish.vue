<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi, uploadApi } from '@/api'
import { formatPrice } from '@/utils'

const router = useRouter()

const categories = ref<any[]>([])
const loading = ref(false)
const uploading = ref(false)

const form = ref({
  name: '',
  description: '',
  price: '',
  originalPrice: '',
  categoryId: '',
  conditionLevel: '',
  location: '',
  stock: 1,
  images: [] as string[]
})

const conditionOptions = [
  { label: '全新', value: '全新' },
  { label: '几乎全新', value: '几乎全新' },
  { label: '轻微使用', value: '轻微使用' },
  { label: '明显使用', value: '明显使用' }
]

onMounted(async () => {
  try {
    const res = await categoryApi.getList()
    categories.value = res || []
  } catch (e) {
    console.error('获取分类失败', e)
  }
})

async function handleImageUpload(event: Event) {
  const target = event.target as HTMLInputElement
  const files = target.files
  
  if (files && files.length > 0) {
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      
      // 检查文件类型
      const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
      if (!allowedTypes.includes(file.type)) {
        ElMessage.warning('只支持 JPG、PNG、GIF、WebP 格式的图片')
        continue
      }
      
      // 检查文件大小（最大 5MB）
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning('单张图片大小不能超过 5MB')
        continue
      }
      
      // 上传到服务器
      uploading.value = true
      try {
        const res = await uploadApi.uploadImage(file, 'product')
        if (res) {
          const imageUrl = res
          form.value.images.push(imageUrl)
          ElMessage.success('图片上传成功')
        }
      } catch (e: any) {
        ElMessage.error(e.message || '图片上传失败')
      } finally {
        uploading.value = false
      }
    }
  }
  
  // 清空 input 以便再次选择同一张图片
  target.value = ''
}

function removeImage(index: number) {
  form.value.images.splice(index, 1)
}

async function submitProduct() {
  if (!form.value.name) {
    ElMessage.warning('请输入商品名称')
    return
  }
  if (!form.value.price) {
    ElMessage.warning('请输入商品价格')
    return
  }
  if (!form.value.categoryId) {
    ElMessage.warning('请选择商品分类')
    return
  }
  if (!form.value.conditionLevel) {
    ElMessage.warning('请选择新旧程度')
    return
  }
  if (form.value.images.length === 0) {
    ElMessage.warning('请上传至少一张商品图片')
    return
  }
  
  loading.value = true
  try {
    const res = await fetch('/api/product/publish', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Token': sessionStorage.getItem('token') || ''
      },
      body: JSON.stringify({
        name: form.value.name,
        description: form.value.description,
        price: Number(form.value.price),
        originalPrice: form.value.originalPrice ? Number(form.value.originalPrice) : null,
        categoryId: Number(form.value.categoryId),
        conditionLevel: form.value.conditionLevel,
        location: form.value.location,
        stock: form.value.stock,
        images: JSON.stringify(form.value.images.map((url, index) => ({
          url,
          isCover: index === 0
        })))
      })
    })
    
    const data = await res.json()
    if (data.code === 200) {
      ElMessage.success('发布成功！商品正在等待管理员审核，审核通过后将展示在首页')
      router.push('/my-products')
    } else {
      ElMessage.error(data.message || '发布失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="publish-page">
    <div class="container">
      <div class="page-header">
        <h1>发布闲置商品</h1>
        <p class="tip">发布后需要管理员审核才能展示</p>
      </div>
      
      <div class="publish-form">
        <!-- 商品图片 -->
        <div class="form-section">
          <h2>商品图片</h2>
          <div class="image-upload">
            <div class="upload-list">
              <div v-for="(img, index) in form.images" :key="index" class="upload-item">
                <img :src="img" alt="" />
                <span class="cover-tag" v-if="index === 0">封面</span>
                <el-icon class="delete-btn" @click="removeImage(index)"><Close /></el-icon>
              </div>
            <div class="upload-add" v-if="form.images.length < 5 && !uploading">
                <input 
                  type="file" 
                  accept="image/*" 
                  multiple 
                  @change="handleImageUpload"
                  class="upload-input"
                />
                <el-icon><Plus /></el-icon>
                <span>{{ form.images.length }}/5</span>
            </div>
            <div class="upload-add uploading" v-if="uploading">
              <el-icon class="is-loading" :size="24"><Loading /></el-icon>
              <span>上传中...</span>
            </div>
            </div>
            <p class="upload-tip">最多上传5张图片，支持 JPG、PNG 格式</p>
          </div>
        </div>
        
        <!-- 商品信息 -->
        <div class="form-section">
          <h2>商品信息</h2>
          <el-form label-width="100px">
            <el-form-item label="商品名称">
              <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="100" show-word-limit />
            </el-form-item>
            
            <el-form-item label="商品分类">
              <el-select v-model="form.categoryId" placeholder="请选择分类">
                <el-option 
                  v-for="cat in categories" 
                  :key="cat.id" 
                  :label="cat.name" 
                  :value="cat.id" 
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="新旧程度">
              <el-radio-group v-model="form.conditionLevel">
                <el-radio v-for="opt in conditionOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="交易地点">
              <el-input v-model="form.location" placeholder="如：图书馆前广场、学生宿舍5号楼" />
            </el-form-item>

            <el-form-item label="商品库存">
              <el-input-number
                v-model="form.stock"
                :min="1"
                :max="999"
                :step="1"
              />
              <span class="unit">件</span>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 价格信息 -->
        <div class="form-section">
          <h2>价格设置</h2>
          <el-form label-width="100px">
            <el-form-item label="出售价格">
              <el-input-number 
                v-model="form.price" 
                :min="0.01" 
                :precision="2" 
                :step="1"
                placeholder="请输入出售价格"
              />
              <span class="unit">元</span>
            </el-form-item>
            
            <el-form-item label="原价（选填）">
              <el-input-number 
                v-model="form.originalPrice" 
                :min="0" 
                :precision="2" 
                :step="1"
                placeholder="填写原价让买家更心动"
              />
              <span class="unit">元</span>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 商品描述 -->
        <div class="form-section">
          <h2>商品描述</h2>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="描述一下商品的详细信息、品牌型号、入手渠道、转手原因等"
            maxlength="500"
            show-word-limit
          />
        </div>
        
        <!-- 提交按钮 -->
        <div class="submit-section">
          <el-button size="large" @click="router.back()">取消</el-button>
          <el-button type="primary" size="large" :loading="loading" @click="submitProduct">
            提交发布
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Plus, Close, Loading } from '@element-plus/icons-vue'
export default {
  components: { Plus, Close, Loading }
}
</script>

<style scoped>
.publish-page {
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

.tip {
  color: #FA8C16;
  font-size: 14px;
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.form-section h2 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

/* 图片上传 */
.image-upload {
  padding: 0;
}

.upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.upload-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
}

.upload-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-tag {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 107, 53, 0.9);
  color: #fff;
  text-align: center;
  font-size: 12px;
  padding: 4px;
}

.delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-add {
  width: 120px;
  height: 120px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: #999;
  position: relative;
}

.upload-add:hover {
  border-color: #FF6B35;
  color: #FF6B35;
}

.upload-add.uploading {
  border-color: #FF6B35;
  color: #FF6B35;
  cursor: wait;
}

.upload-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.upload-tip {
  margin-top: 16px;
  color: #999;
  font-size: 13px;
}

/* 表单 */
.unit {
  margin-left: 8px;
  color: #999;
}

.submit-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.submit-section .el-button {
  width: 120px;
  height: 44px;
}
</style>
