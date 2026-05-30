<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi, cartApi, commentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { formatPrice, getFirstImage, formatRelativeTime } from '@/utils'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const product = ref<any>(null)
const comments = ref<any[]>([])
const loading = ref(true)
const activeTab = ref('detail')
const quantity = ref(1)

// 评论相关
const newComment = ref('')
const commentRating = ref(5)
const submitting = ref(false)

// 回复相关
const replyingTo = ref<number | null>(null) // 当前正在回复的评论ID
const replyContent = ref('')
const replySubmitting = ref(false)

const productId = computed(() => Number(route.params.id))
const isLoggedIn = computed(() => userStore.isLoggedIn)

onMounted(async () => {
  try {
    const [productData, commentRes] = await Promise.all([
      productApi.getDetail(productId.value),
      commentApi.getList(productId.value, { page: 1, pageSize: 10 })
    ])
    product.value = productData
    comments.value = commentRes?.records || []
  } catch (e) {
    ElMessage.error('获取商品详情失败')
    router.push('/')
  } finally {
    loading.value = false
  }
})

async function addToCart() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    await cartApi.add(productId.value, quantity.value)
    await cartStore.fetchCart()
    ElMessage.success('已加入购物车')
  } catch (e: any) {
    ElMessage.error(e.message || '加入购物车失败')
  }
}

function buyNow() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // 直接购买，跳转到结算页面
  router.push({
    path: '/checkout',
    query: { productId: productId.value.toString(), quantity: quantity.value.toString() }
  })
}

function contactSeller() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (product.value.userId === userStore.userInfo?.id) {
    ElMessage.warning('不能给自己发消息')
    return
  }
  
  router.push({
    path: '/messages',
    query: { userId: product.value.userId.toString(), productId: productId.value.toString() }
  })
}

function viewSellerProducts() {
  // 跳转到卖家商品页面，传入卖家用户ID
  router.push(`/seller/${product.value.userId}`)
}

// 提交评论
async function submitComment() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submitting.value = true
  try {
    await commentApi.add({
      productId: productId.value,
      content: newComment.value.trim(),
      rating: commentRating.value
    })
    ElMessage.success('评论发布成功')
    newComment.value = ''
    commentRating.value = 5

    // 刷新评论列表
    await loadComments()
  } catch (e: any) {
    ElMessage.error(e.message || '评论发布失败')
  } finally {
    submitting.value = false
  }
}

// 加载评论
async function loadComments() {
  const commentRes = await commentApi.getList(productId.value, { page: 1, pageSize: 50 })
  comments.value = commentRes?.records || []
}

// 点击评论人头像跳转私信
function goToMessage(userId: number) {
  router.push({
    path: '/messages',
    query: { userId: userId.toString() }
  })
}

// 点击回复按钮
function startReply(commentId: number) {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  replyingTo.value = commentId
  replyContent.value = ''
}

// 取消回复
function cancelReply() {
  replyingTo.value = null
  replyContent.value = ''
}

// 提交回复
async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  replySubmitting.value = true
  try {
    await commentApi.add({
      productId: productId.value,
      content: replyContent.value.trim(),
      parentId: replyingTo.value ?? undefined
    })
    ElMessage.success('回复成功')
    replyingTo.value = null
    replyContent.value = ''

    // 刷新评论列表
    await loadComments()
  } catch (e: any) {
    ElMessage.error(e.message || '回复失败')
  } finally {
    replySubmitting.value = false
  }
}
</script>

<template>
  <div class="product-detail-page">
    <div class="container">
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>
      
      <template v-else-if="product">
        <!-- 商品信息 -->
        <div class="product-main">
          <!-- 左侧图片 -->
          <div class="product-image">
            <div class="main-image">
              <img 
                :src="getFirstImage(product.images) || '/images/products/default-product.svg'" 
                :alt="product.name"
              />
              <span v-if="product.conditionLevel" class="condition-tag">
                {{ product.conditionLevel }}
              </span>
            </div>
          </div>
          
          <!-- 右侧信息 -->
          <div class="product-info">
            <h1 class="product-name">{{ product.name }}</h1>
            
            <div class="price-section">
              <span class="current-price">{{ formatPrice(product.price) }}</span>
              <span v-if="product.originalPrice" class="original-price">
                原价: {{ formatPrice(product.originalPrice) }}
              </span>
              <span v-if="product.originalPrice" class="discount">
                省 {{ formatPrice(Number(product.originalPrice) - Number(product.price)) }}
              </span>
            </div>
            
            <div class="info-grid">
              <div class="info-item">
                <span class="label">新旧程度</span>
                <span class="value">{{ product.conditionLevel || '未知' }}</span>
              </div>
              <div class="info-item">
                <span class="label">商品分类</span>
                <span class="value">{{ product.categoryName || '其他' }}</span>
              </div>
              <div class="info-item">
                <span class="label">交易地点</span>
                <span class="value">{{ product.location || '未填写' }}</span>
              </div>
              <div class="info-item">
                <span class="label">浏览次数</span>
                <span class="value">{{ product.viewCount || 0 }}次</span>
              </div>
            </div>
            
            <div class="quantity-section">
              <span class="label">购买数量</span>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="product.stock ?? 1"
                size="default"
              />
              <span class="stock">库存 {{ product.stock ?? 1 }} 件</span>
            </div>
            
            <div class="action-buttons">
              <el-button type="primary" size="large" @click="buyNow">
                立即购买
              </el-button>
              <el-button size="large" @click="addToCart">
                加入购物车
              </el-button>
              <el-button size="large" @click="contactSeller">
                联系卖家
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- 卖家信息 -->
        <div class="seller-section">
          <div class="seller-card">
            <div class="seller-info">
              <el-avatar :size="60" :src="product.sellerAvatar || '/images/avatars/default-avatar.svg'" />
              <div class="seller-detail">
                <h3>{{ product.sellerNickname || '匿名用户' }}</h3>
                <p>卖家</p>
              </div>
            </div>
            <el-button size="small" @click="viewSellerProducts">
              查看更多商品
            </el-button>
          </div>
        </div>
        
        <!-- 商品详情和评论 -->
        <div class="detail-tabs">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="商品详情" name="detail">
              <div class="product-description">
                <h3>商品描述</h3>
                <p>{{ product.description || '暂无描述' }}</p>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="`商品评论 (${comments.length})`" name="comment">
              <!-- 评论输入框 -->
              <div class="comment-form" v-if="isLoggedIn">
                <div class="form-header">
                  <el-avatar :size="40" :src="userStore.userInfo?.avatar" />
                  <div class="form-content">
                    <div class="rating-row">
                      <span>评分：</span>
                      <el-rate v-model="commentRating" />
                    </div>
                    <el-input
                      v-model="newComment"
                      type="textarea"
                      :rows="3"
                      placeholder="写下你对商品的评价..."
                      maxlength="500"
                      show-word-limit
                    />
                    <div class="form-actions">
                      <el-button type="primary" :loading="submitting" @click="submitComment">
                        发布评论
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
              <div class="comment-form login-tip" v-else>
                <p>登录后可发表评论 <el-button type="primary" text @click="router.push('/login')">去登录</el-button></p>
              </div>
              
              <!-- 评论列表 -->
              <div class="comment-list">
                <div v-if="comments.length === 0" class="empty-comments">
                  暂无评论，快来发表第一条评论吧
                </div>
                <div v-for="comment in comments" :key="comment.id" class="comment-item">
                  <div class="comment-header">
                    <el-avatar 
                      :size="40" 
                      :src="comment.userAvatar" 
                      class="clickable-avatar"
                      @click="goToMessage(comment.userId)"
                    />
                    <div class="comment-user">
                      <span class="nickname" @click="goToMessage(comment.userId)">{{ comment.userNickname }}</span>
                      <el-rate v-model="comment.rating" disabled size="small" />
                    </div>
                    <span class="comment-time">{{ formatRelativeTime(comment.createTime) }}</span>
                  </div>
                  <p class="comment-content">{{ comment.content }}</p>
                  <div class="comment-actions">
                    <span class="reply-btn" @click="startReply(comment.id)">回复</span>
                    <span class="pm-btn" @click="goToMessage(comment.userId)">私信</span>
                  </div>

                  <!-- 回复表单 -->
                  <div v-if="replyingTo === comment.id" class="reply-form">
                    <el-input
                      v-model="replyContent"
                      type="textarea"
                      :rows="2"
                      :placeholder="`回复 ${comment.userNickname}...`"
                      maxlength="200"
                      show-word-limit
                    />
                    <div class="reply-actions">
                      <el-button size="small" @click="cancelReply">取消</el-button>
                      <el-button type="primary" size="small" :loading="replySubmitting" @click="submitReply">
                        发送回复
                      </el-button>
                    </div>
                  </div>

                  <!-- 回复列表 -->
                  <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
                    <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                      <div class="reply-header">
                        <el-avatar 
                          :size="32" 
                          :src="reply.userAvatar" 
                          class="clickable-avatar"
                          @click="goToMessage(reply.userId)"
                        />
                        <div class="reply-user">
                          <span class="nickname" @click="goToMessage(reply.userId)">{{ reply.userNickname }}</span>
                          <span class="reply-time">{{ formatRelativeTime(reply.createTime) }}</span>
                        </div>
                        <span class="pm-btn-small" @click="goToMessage(reply.userId)">私信</span>
                      </div>
                      <p class="reply-content">{{ reply.content }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>
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
.product-detail-page {
  background: var(--color-background);
  padding: 20px 0;
}

.product-main {
  display: flex;
  gap: 40px;
  background: var(--color-surface);
  padding: 30px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.product-image {
  flex-shrink: 0;
  width: 400px;
}

.main-image {
  position: relative;
  width: 100%;
  padding-top: 100%;
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-muted);
}

.main-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.condition-tag {
  position: absolute;
  top: 16px;
  left: 16px;
  background: var(--color-accent);
  color: #fff;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 20px;
}

.price-section {
  background: var(--color-accent-light);
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.current-price {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-accent);
}

.original-price {
  font-size: 14px;
  color: var(--color-text-muted);
  text-decoration: line-through;
  margin-left: 16px;
}

.discount {
  background: var(--color-accent);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  padding: 12px;
  background: var(--color-background);
  border-radius: 8px;
}

.info-item .label {
  color: var(--color-text-muted);
  width: 80px;
}

.info-item .value {
  color: var(--color-text-primary);
  font-weight: 500;
}

.quantity-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.quantity-section .label {
  color: var(--color-text-secondary);
}


.stock {
  color: var(--color-text-muted);
  font-size: 14px;
}

.action-buttons {
  display: flex;
  gap: 16px;
}

.action-buttons .el-button {
  flex: 1;
  height: 48px;
  font-size: 16px;
}

.action-buttons .el-button--primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

/* 卖家信息 */
.seller-section {
  margin-bottom: 20px;
}

.seller-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-surface);
  padding: 20px;
  border-radius: 12px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.seller-detail h3 {
  font-size: 16px;
  margin-bottom: 4px;
}

.seller-detail p {
  font-size: 14px;
  color: var(--color-text-muted);
}

/* 详情标签页 */
.detail-tabs {
  background: var(--color-surface);
  padding: 20px;
  border-radius: 12px;
}

.product-description {
  padding: 20px 0;
}

.product-description h3 {
  font-size: 18px;
  margin-bottom: 16px;
}

.product-description p {
  line-height: 1.8;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
}

/* 评论表单 */
.comment-form {
  background: var(--color-background);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.comment-form.login-tip {
  text-align: center;
  color: var(--color-text-muted);
}

.comment-form .form-header {
  display: flex;
  gap: 12px;
}

.comment-form .form-content {
  flex: 1;
}

.comment-form .rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.comment-form .form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.comment-form .form-actions .el-button--primary {
  background: var(--color-accent);
  border-color: var(--color-accent);
}

.comment-list {
  padding: 20px 0;
}

.empty-comments {
  text-align: center;
  padding: 40px;
  color: var(--color-text-muted);
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.comment-user {
  flex: 1;
}

.comment-user .nickname {
  display: block;
  font-weight: 500;
  margin-bottom: 4px;
}

.comment-time {
  color: var(--color-text-muted);
  font-size: 13px;
}

.comment-content {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.comment-actions {
  margin-top: 8px;
  display: flex;
  gap: 16px;
}

.reply-btn {
  color: var(--color-accent);
  font-size: 13px;
  cursor: pointer;
}

.reply-btn:hover {
  text-decoration: underline;
}

.pm-btn {
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  padding: 2px 8px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  margin-left: 8px;
}

.pm-btn:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.pm-btn-small {
  color: var(--color-text-muted);
  font-size: 12px;
  cursor: pointer;
  margin-left: auto;
}

.pm-btn-small:hover {
  color: var(--color-accent);
}

.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.clickable-avatar:hover {
  transform: scale(1.1);
}

.comment-user .nickname {
  cursor: pointer;
}

.comment-user .nickname:hover {
  color: var(--color-accent);
}

.reply-user .nickname {
  cursor: pointer;
}

.reply-user .nickname:hover {
  color: var(--color-accent);
}

/* 回复表单 */
.reply-form {
  margin-top: 12px;
  padding: 12px;
  background: var(--color-background);
  border-radius: 8px;
}

.reply-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.reply-actions .el-button--primary {
  background: var(--color-accent);
  border-color: var(--color-accent);
}

/* 回复列表 */
.reply-list {
  margin-top: 12px;
  padding-left: 20px;
  border-left: 2px solid var(--color-border-light);
}

.reply-item {
  padding: 10px 0;
  border-bottom: 1px dashed var(--color-border-light);
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.reply-user {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.reply-user .nickname {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.reply-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.reply-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-left: 40px;
}

@media (max-width: 768px) {
  .product-main {
    flex-direction: column;
  }
  
  .product-image {
    width: 100%;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
