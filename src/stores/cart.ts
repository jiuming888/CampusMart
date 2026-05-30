import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { cartApi } from '@/api'

interface CartItem {
  id: number
  productId: number
  quantity: number
  product?: any
  selected?: boolean
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const loading = ref(false)

  const totalCount = computed(() => 
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  const totalPrice = computed(() => 
    items.value.reduce((sum, item) => {
      if (item.product?.price) {
        return sum + parseFloat(item.product.price) * item.quantity
      }
      return sum
    }, 0)
  )

  const selectedItems = computed(() => 
    items.value.filter(item => item.selected !== false)
  )

  const selectedCount = computed(() => 
    selectedItems.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  const selectedPrice = computed(() => 
    selectedItems.value.reduce((sum, item) => {
      if (item.product?.price) {
        return sum + parseFloat(item.product.price) * item.quantity
      }
      return sum
    }, 0)
  )

  async function fetchCart() {
    loading.value = true
    try {
      const res = await cartApi.getList()
      items.value = res || []
    } finally {
      loading.value = false
    }
  }

  async function addToCart(productId: number, quantity: number = 1) {
    const res = await cartApi.add(productId, quantity)
    await fetchCart()
    return res
  }

  async function updateQuantity(id: number, quantity: number) {
    await cartApi.updateQuantity(id, quantity)
    const item = items.value.find(i => i.id === id)
    if (item) {
      item.quantity = quantity
    }
  }

  async function removeItem(id: number) {
    await cartApi.remove(id)
    items.value = items.value.filter(i => i.id !== id)
  }

  async function clearCart() {
    await cartApi.clear()
    items.value = []
  }

  async function clearSelectedItems() {
    // 获取选中的商品ID
    const selectedIds = items.value
      .filter(item => item.selected !== false)
      .map(item => item.id)

    // 调用后端API删除选中的商品
    for (const id of selectedIds) {
      await cartApi.remove(id)
    }

    // 从本地移除选中的商品
    items.value = items.value.filter(item => item.selected === false)
  }

  function toggleSelect(id: number) {
    const item = items.value.find(i => i.id === id)
    if (item) {
      item.selected = item.selected === false ? true : false
    }
  }

  function selectAll(selected: boolean) {
    items.value.forEach(item => {
      item.selected = selected
    })
  }

  return {
    items,
    loading,
    totalCount,
    totalPrice,
    selectedItems,
    selectedCount,
    selectedPrice,
    fetchCart,
    addToCart,
    updateQuantity,
    removeItem,
    clearCart,
    clearSelectedItems,
    toggleSelect,
    selectAll
  }
})
