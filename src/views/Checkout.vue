<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, cartApi, addressApi, productApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import { formatPrice, getFirstImage } from '@/utils'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const addresses = ref<any[]>([])
const selectedAddress = ref<any>(null)
const orderItems = ref<any[]>([])
const remark = ref('')
const loading = ref(false)
const showAddressDialog = ref(false)

// 新增收货地址表单
const addressForm = ref({
  receiverName: '',
  phone: '',
  province: '',
  detailAddress: '',
  isDefault: false
})

const totalPrice = computed(() => {
  return orderItems.value.reduce((sum, item) => {
    return sum + Number(item.price || 0) * item.quantity
  }, 0)
})

onMounted(async () => {
  await loadAddresses()
  await loadOrderItems()
})

async function loadAddresses() {
  try {
    const res = await addressApi.getList()
    addresses.value = res || []
    // 设置默认地址
    selectedAddress.value = addresses.value.find(a => a.isDefault === 1) || addresses.value[0]
  } catch (e) {
    console.error('获取地址失败', e)
  }
}

async function loadOrderItems() {
  // 从购物车选择
  const cartIds = route.query.cartIds as string
  if (cartIds) {
    await cartStore.fetchCart()
    const selectedIds = cartIds.split(',').map(id => parseInt(id))
    orderItems.value = cartStore.items
      .filter(item => selectedIds.includes(item.id))
      .map(item => ({
        id: item.id,
        productId: item.productId,
        name: item.product?.name || '商品已下架',
        image: item.product ? getFirstImage(item.product.images) : '',
        price: item.product?.price || 0,
        quantity: item.quantity
      }))
  }
  // 直接购买
  const productId = route.query.productId as string
  const quantity = route.query.quantity as string
  if (productId && !cartIds) {
    orderItems.value = [{
      id: 0,
      productId: Number(productId),
      name: '',
      image: '',
      price: 0,
      quantity: Number(quantity) || 1
    }]
    // 加载商品信息
    try {
      const productData = await productApi.getDetail(Number(productId))
      if (productData) {
        orderItems.value[0].name = productData.name
        orderItems.value[0].image = getFirstImage(productData.images)
        orderItems.value[0].price = productData.price
      }
    } catch (e) {
      console.error('获取商品信息失败', e)
      orderItems.value[0].name = '商品加载失败'
    }
  }
}

function selectAddress(address: any) {
  selectedAddress.value = address
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

async function addAddress() {
  if (!addressForm.value.receiverName || !addressForm.value.phone || !addressForm.value.detailAddress) {
    ElMessage.warning('请填写完整的收货信息')
    return
  }
  
  try {
    const newAddr = await addressApi.add(addressForm.value)
    addresses.value.push(newAddr)
    selectedAddress.value = newAddr
    showAddressDialog.value = false
    addressForm.value = {
      receiverName: '',
      phone: '',
      province: '',
      detailAddress: '',
      isDefault: false
    }
    ElMessage.success('添加成功')
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

async function submitOrder() {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址')
    return
  }

  loading.value = true
  try {
    const cartIds = route.query.cartIds as string
    const productId = route.query.productId as string

    let orderData: any = {
      addressId: selectedAddress.value.id,
      remark: remark.value
    }

    if (cartIds) {
      orderData.cartItemIds = cartIds.split(',').map(id => parseInt(id))
    } else if (productId) {
      orderData.productId = Number(productId)
      orderData.quantity = Number(route.query.quantity) || 1
    }

    // 创建订单成功后，直接调起支付宝支付
    const res = await orderApi.create(orderData)
    const orderId = res?.id || res?.orderId
    const orderNo = res?.orderNo || res?.order_no || ''
    const amount = res?.totalAmount || res?.total_amount || totalPrice.value

    if (!orderId) {
      ElMessage.error('创建订单失败：未获取到订单ID')
      return
    }

    // 获取支付宝支付链接并跳转
    const params = new URLSearchParams({
      orderId: String(orderId),
      frontendUrl: window.location.origin,
      publicApiUrl: window.location.origin
    })
    try {
      const payRes = await fetch('/api/alipay/pay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
      })

      const payData = await payRes.json()

      if (payData.success && payData.payUrl) {
        // 直接在当前页面跳转到支付宝沙箱支付页面
        window.location.href = payData.payUrl
        return
      } else {
        ElMessage.error(payData.message || '获取支付链接失败，请重试')
      }
    } catch (networkError) {
      console.error('网络请求失败:', networkError)
      ElMessage.error('支付网关连接失败，订单已创建，请到订单页面完成支付')
    }

    // 支付发起失败，跳转到订单页面
    setTimeout(() => {
      router.push('/order')
    }, 1500)
  } catch (e: any) {
    ElMessage.error(e?.message || '创建订单失败')
  } finally {
    loading.value = false
  }
}

</script>

<template>

  <div class="checkout-page">
    <div class="container">
      <div class="page-header">
        <h1>确认订单</h1>
      </div>
      
      <div class="checkout-content">
        <!-- 收货地址 -->
        <div class="section">
          <h2>收货地址</h2>
          <div class="address-list">
            <div 
              v-for="addr in addresses" 
              :key="addr.id"
              class="address-card"
              :class="{ active: selectedAddress?.id === addr.id }"
              @click="selectAddress(addr)"
            >
              <div class="address-info">
                <p class="receiver">{{ addr.receiverName }} {{ addr.phone }}</p>
                <p class="address">
                  {{ addr.province || addr.detailAddress }}
                </p>
              </div>
              <span v-if="addr.isDefault" class="default-tag">默认</span>
              <span v-if="selectedAddress?.id === addr.id" class="check-icon">✓</span>
              <el-icon class="delete-icon" @click.stop="deleteAddress(addr.id)"><Delete /></el-icon>
            </div>
            
            <div class="address-add" @click="showAddressDialog = true">
              <span class="add-icon">+</span>
              <span>添加新地址</span>
            </div>
          </div>
        </div>
        
        <!-- 商品清单 -->
        <div class="section">
          <h2>商品清单</h2>
          <div class="order-items">
            <div v-for="item in orderItems" :key="item.id" class="order-item">
              <img :src="item.image || '/images/products/default-product.svg'" alt="" />
              <div class="item-info">
                <h3>{{ item.name }}</h3>
                <p>数量：{{ item.quantity }}</p>
              </div>
              <div class="item-price">{{ formatPrice(item.price) }}</div>
            </div>
          </div>
        </div>
        
        <!-- 订单备注 -->
        <div class="section">
          <h2>订单备注</h2>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            placeholder="选填，可备注特殊需求"
            class="remark-input"
          />
        </div>
        
        <!-- 结算信息 -->
        <div class="section settlement">
          <div class="total-info">
            <div class="info-row">
              <span>商品总价</span>
              <span>{{ formatPrice(totalPrice) }}</span>
            </div>
            <div class="info-row">
              <span>运费</span>
              <span>免运费</span>
            </div>
            <div class="info-row total">
              <span>应付总额</span>
              <span class="total-price">{{ formatPrice(totalPrice) }}</span>
            </div>
          </div>
        </div>
        
        <!-- 提交按钮 -->
        <div class="submit-section">
          <div class="submit-info">
            <span>收货人：{{ selectedAddress?.receiverName || '请选择' }}</span>
            <span>联系电话：{{ selectedAddress?.phone || '' }}</span>
          </div>
          <el-button type="primary" size="large" :loading="loading" @click="submitOrder">
            去付款
          </el-button>
        </div>
      </div>
    </div>

    <!-- 新增地址对话框 -->
    <el-dialog v-model="showAddressDialog" title="添加收货地址" width="500px">
      <el-form :model="addressForm" label-width="80px">
        <el-form-item label="收货人">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="addressForm.detailAddress" type="textarea" :rows="3" placeholder="请输入收货地址" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" @click="addAddress">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Delete } from '@element-plus/icons-vue'
export default {
  components: { Delete }
}
</script>

<style scoped>
.checkout-page {
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
}

.checkout-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.section h2 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

/* 地址列表 */
.address-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.address-card {
  position: relative;
  width: calc(50% - 8px);
  padding: 16px;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.address-card:hover {
  border-color: #ddd;
}

.address-card.active {
  border-color: #FF6B35;
  background: #FFF7F5;
}

.receiver {
  font-weight: 600;
  margin-bottom: 8px;
}

.address {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.default-tag {
  position: absolute;
  top: 12px;
  right: 36px;
  background: #FF6B35;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.delete-icon {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #999;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.2s;
}

.delete-icon:hover {
  color: #f56c6c;
}

.check-icon {
  position: absolute;
  bottom: 12px;
  right: 36px;
  color: #FF6B35;
  font-size: 20px;
  font-weight: bold;
}

.address-add {
  width: calc(50% - 8px);
  padding: 16px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: #999;
  transition: all 0.3s;
}

.address-add:hover {
  border-color: #FF6B35;
  color: #FF6B35;
}

.add-icon {
  font-size: 24px;
}

/* 商品清单 */
.order-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.order-item .item-info {
  flex: 1;
  margin-left: 16px;
}

.order-item .item-info h3 {
  font-size: 14px;
  margin-bottom: 8px;
}

.order-item .item-info p {
  color: #999;
  font-size: 14px;
}

.item-price {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 结算信息 */
.settlement {
  text-align: right;
}

.total-info {
  display: inline-block;
  min-width: 300px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  color: #666;
}

.info-row.total {
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
  padding-top: 16px;
  font-weight: 600;
}

.total-price {
  font-size: 24px;
  color: #FF6B35;
}

/* 提交区域 */
.submit-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.submit-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #666;
  font-size: 14px;
}

.submit-section .el-button {
  width: 160px;
  height: 48px;
  font-size: 16px;
}

@media (max-width: 768px) {
  .address-card,
  .address-add {
    width: 100%;
  }
}
</style>
