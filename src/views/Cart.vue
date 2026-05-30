<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { formatPrice, getFirstImage } from '@/utils'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const selectedAll = ref(true)

// 初始化购物车
async function initCart() {
  await cartStore.fetchCart()
  updateSelectedAll()
}

onMounted(async () => {
  await initCart()
})

function updateSelectedAll() {
  selectedAll.value = cartStore.items.length > 0 && 
    cartStore.items.every(item => item.selected !== false && !isProductUnavailable(item.product))
}

function isProductUnavailable(product: any): boolean {
  // status=4 表示商品不存在或已删除，其他非上架状态都视为不可用
  if (!product) return true
  if (product.status === 4) return true  // 已删除
  if (product.status !== 1) return true  // 非上架状态
  return false
}

async function toggleSelect(id: number) {
  cartStore.toggleSelect(id)
  updateSelectedAll()
}

function toggleSelectAll() {
  cartStore.selectAll(!selectedAll.value)
  selectedAll.value = !selectedAll.value
}

async function updateQuantity(id: number, quantity: number) {
  await cartStore.updateQuantity(id, quantity)
}

async function removeItem(id: number) {
  await cartStore.removeItem(id)
  ElMessage.success('已移除商品')
}

function checkout() {
  const selectedIds = cartStore.selectedItems.map(item => item.id)
  if (selectedIds.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  router.push({ path: '/checkout', query: { cartIds: selectedIds.join(',') } })
}
</script>

<template>
  <div class="cart-page">
    <div class="container">
      <div class="page-header">
        <h1>购物车</h1>
        <span class="total">共 {{ cartStore.totalCount }} 件商品</span>
      </div>
      
      <div v-if="cartStore.items.length === 0" class="empty-cart">
        <div class="empty-icon"><el-icon :size="80"><ShoppingCart /></el-icon></div>
        <h2>购物车是空的</h2>
        <p>快去挑选心仪的商品吧</p>
        <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
      </div>
      
      <div v-else class="cart-content">
        <!-- 商品列表 -->
        <div class="cart-list">
          <div class="cart-header">
            <el-checkbox v-model="selectedAll" @change="toggleSelectAll">全选</el-checkbox>
            <span class="header-info">商品信息</span>
            <span class="header-price">单价</span>
            <span class="header-quantity">数量</span>
            <span class="header-subtotal">小计</span>
            <span class="header-action">操作</span>
          </div>
          
          <div class="cart-items">
            <div 
              v-for="item in cartStore.items" 
              :key="item.id"
              class="cart-item"
              :class="{ 'item-unavailable': isProductUnavailable(item.product) }"
            >
              <div class="item-select">
                <el-checkbox 
                  :model-value="item.selected !== false && !isProductUnavailable(item.product)" 
                  :disabled="isProductUnavailable(item.product)"
                  @change="toggleSelect(item.id)"
                />
              </div>
              
              <div class="item-info" @click="!isProductUnavailable(item.product) && router.push(`/product/${item.productId}`)">
                <img 
                  :src="getFirstImage(item.product?.images) || '/images/products/default-product.svg'" 
                  alt=""
                  :class="{ 'img-unavailable': isProductUnavailable(item.product) }"
                />
                <div class="item-detail">
                  <h3 :class="{ 'text-unavailable': isProductUnavailable(item.product) }">
                    {{ item.product?.name || '商品已下架' }}
                    <el-tag v-if="isProductUnavailable(item.product)" type="danger" size="small">已下架</el-tag>
                  </h3>
                  <p>{{ item.product?.location || (item.product ? '未知地点' : '商品信息加载失败') }}</p>
                </div>
              </div>
              
              <div class="item-price" :class="{ 'price-unavailable': isProductUnavailable(item.product) }">
                {{ item.product && !isProductUnavailable(item.product) ? formatPrice(item.product.price) : '¥0.00' }}
              </div>
              
              <div class="item-quantity">
                <el-input-number 
                  :model-value="item.quantity" 
                  :min="1" 
                  :max="10"
                  size="small"
                  :disabled="isProductUnavailable(item.product)"
                  @change="(val: number) => updateQuantity(item.id, val)"
                />
              </div>
              
              <div class="item-subtotal">
                {{ item.product && !isProductUnavailable(item.product) ? formatPrice(Number(item.product.price) * item.quantity) : '¥0.00' }}
              </div>
              
              <div class="item-action">
                <el-button type="danger" text @click="removeItem(item.id)">
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 结算栏 -->
        <div class="checkout-bar">
          <div class="checkout-info">
            <span class="selected-count">
              已选择 {{ cartStore.selectedCount }} 件商品
            </span>
            <span class="total-price">
              合计：<strong>{{ formatPrice(cartStore.selectedPrice) }}</strong>
            </span>
          </div>
          <el-button type="primary" size="large" @click="checkout">
            去结算
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page {
  background: #f8f8f8;
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
  color: #999;
  font-size: 14px;
}

.empty-cart {
  background: #fff;
  border-radius: 12px;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.empty-cart h2 {
  font-size: 20px;
  margin-bottom: 12px;
}

.empty-cart p {
  color: #999;
  margin-bottom: 24px;
}

.cart-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cart-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.cart-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #f8f8f8;
  font-size: 14px;
  color: #666;
}

.header-info {
  flex: 1;
  margin-left: 16px;
}

.header-price,
.header-quantity,
.header-subtotal {
  width: 120px;
  text-align: center;
}

.header-action {
  width: 80px;
  text-align: center;
}

.cart-items {
  padding: 0 20px;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item.item-unavailable {
  background-color: #f9f9f9;
  opacity: 0.8;
}

.item-select {
  width: 40px;
}

.item-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.item-info img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.item-info img.img-unavailable {
  filter: grayscale(100%);
  opacity: 0.6;
}

.item-detail h3 {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-detail h3.text-unavailable {
  color: #999;
}

.item-detail p {
  font-size: 12px;
  color: #999;
}

.item-price {
  width: 120px;
  text-align: center;
  color: #333;
  font-weight: 500;
}

.item-price.price-unavailable {
  color: #999;
  text-decoration: line-through;
}

.item-quantity {
  width: 120px;
  text-align: center;
}

.item-subtotal {
  width: 120px;
  text-align: center;
  color: #FF6B35;
  font-weight: 600;
}

.item-action {
  width: 80px;
  text-align: center;
}

/* 结算栏 */
.checkout-bar {
  position: sticky;
  bottom: 20px;
  background: #fff;
  padding: 16px 24px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
}

.checkout-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.selected-count {
  color: #666;
  font-size: 14px;
}

.total-price {
  font-size: 16px;
}

.total-price strong {
  font-size: 24px;
  color: #FF6B35;
}

.checkout-bar .el-button {
  width: 160px;
  height: 48px;
  font-size: 16px;
}

@media (max-width: 768px) {
  .cart-header {
    display: none;
  }
  
  .cart-item {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .item-select {
    width: auto;
  }
  
  .item-info {
    width: calc(100% - 60px);
  }
  
  .item-info img {
    width: 60px;
    height: 60px;
  }
  
  .item-price,
  .item-quantity,
  .item-subtotal,
  .item-action {
    width: auto;
    flex: 1;
    text-align: left;
  }
  
  .item-price::before {
    content: '单价：';
    color: #999;
  }
  
  .item-subtotal::before {
    content: '小计：';
    color: #999;
  }
}
</style>
