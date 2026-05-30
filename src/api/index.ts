import request from './request'

// 用户相关API
export const userApi = {
  login: (data: { username: string; password: string }) => 
    request.post('/user/login', data),
  
  adminLogin: (data: { username: string; password: string }) => 
    request.post('/user/admin/login', data),
  
  register: (data: { username: string; password: string; nickname?: string; phone?: string; email?: string }) =>
    request.post('/user/register', data),
  
  getUserInfo: () => request.get('/user/info'),
  
  updateUserInfo: (data: any) => request.put('/user/info', data),
  
  updatePassword: (data: { oldPassword: string; newPassword: string }) => 
    request.put('/user/password', data),

  getUserList: (page: number = 1, pageSize: number = 10) => 
    request.get('/user/list', { params: { page, pageSize } }),

  disable: (id: number) => request.put(`/user/disable/${id}`),

  enable: (id: number) => request.put(`/user/enable/${id}`),

  deleteUser: (id: number) => request.delete(`/user/delete/${id}`)
}

// 分类相关API
export const categoryApi = {
  getList: () => request.get('/category/list'),

  add: (data: { name: string; icon?: string; sort?: number }) =>
    request.post('/category/add', data),

  update: (data: { id: number; name: string; icon?: string; sort?: number }) =>
    request.put('/category/update', data),

  delete: (id: number) => request.delete(`/category/delete/${id}`)
}

// 商品相关API
export const productApi = {
  getList: (params: any) => request.get('/product/list', { params }),
  
  getDetail: (id: number) => request.get(`/product/detail/${id}`),
  
  publish: (data: any) => request.post('/product/publish', data),
  
  update: (data: any) => request.put('/product/update', data),
  
  delete: (id: number) => request.delete(`/product/delete/${id}`),
  
  getMyProducts: (params: any) => request.get('/product/my', { params }),
  
  getHotProducts: (limit?: number) => request.get('/product/hot', { params: { limit } }),
  
  getLatestProducts: (limit?: number) => request.get('/product/latest', { params: { limit } }),
  
  // 根据用户ID获取商品
  getByUser: (userId: number) => request.get('/product/user/' + userId)
}

// 购物车API
export const cartApi = {
  getList: () => request.get('/cart/list'),
  
  add: (productId: number, quantity?: number) => 
    request.post('/cart/add', { productId, quantity: quantity || 1 }),
  
  updateQuantity: (id: number, quantity: number) => 
    request.put(`/cart/update/${id}`, { quantity }),
  
  remove: (id: number) => request.delete(`/cart/delete/${id}`),
  
  clear: () => request.delete('/cart/clear')
}

// 地址API
export const addressApi = {
  getList: () => request.get('/address/list'),
  
  getDefault: () => request.get('/address/default'),
  
  add: (data: any) => request.post('/address/add', data),
  
  update: (id: number, data: any) => request.put(`/address/update/${id}`, data),
  
  delete: (id: number) => request.delete(`/address/delete/${id}`),
  
  setDefault: (id: number) => request.put(`/address/setDefault/${id}`)
}

// 订单API
export const orderApi = {
  create: (data: any) => request.post('/order/create', data),

  getList: (params: any) => request.get('/order/list', { params }),

  getDetail: (id: number) => request.get(`/order/detail/${id}`),

  cancel: (id: number) => request.put(`/order/cancel/${id}`),

  pay: (id: number) => request.put(`/order/pay/${id}`),

  confirm: (id: number) => request.put(`/order/confirm/${id}`),

  delete: (id: number) => request.delete(`/order/delete/${id}`),

  deleteAll: () => request.delete('/order/delete-all')
}

// 消息API
export const messageApi = {
  getConversations: () => request.get('/message/conversations'),
  
  getDetail: (userId: number) => request.get(`/message/detail/${userId}`),
  
  send: (data: { toUserId: number; productId?: number; content: string }) => 
    request.post('/message/send', data),
  
  getUnreadCount: () => request.get('/message/unread'),
  
  markAsRead: (userId: number) => request.put(`/message/read/${userId}`),
  
  getUserInfo: (userId: number) => request.get(`/message/user/${userId}`)
}

// 评论API
export const commentApi = {
  getList: (productId: number, params?: any) =>
    request.get(`/comment/list/${productId}`, { params }),

  add: (data: { productId: number; content: string; rating?: number; parentId?: number }) =>
    request.post('/comment/add', data)
}

// 管理员API
export const adminApi = {
  dashboard: {
    getStats: () => request.get('/admin/dashboard/stats'),
    getOrdersWeek: () => request.get('/admin/dashboard/orders/week'),
    getSalesWeek: () => request.get('/admin/dashboard/sales/week'),
    getProductsByCategory: () => request.get('/admin/dashboard/products/by-category'),
    getProductsByProductStatus: () => request.get('/admin/dashboard/products/by-product-status'),
    getHotProducts: () => request.get('/admin/dashboard/products/hot'),
    getOrdersByStatus: () => request.get('/admin/dashboard/orders/by-status'),
    getUsersWeek: () => request.get('/admin/dashboard/users/week'),
    getRecentOrders: () => request.get('/admin/dashboard/orders/recent'),
  },
  product: {
    getPending: (params: any) => request.get('/admin/product/pending', { params }),

    approve: (id: number) => request.put(`/admin/product/approve/${id}`),

    reject: (id: number, reason?: string) =>
      request.put(`/admin/product/reject/${id}`, reason),

    getAll: (params: any) => request.get('/admin/product/all', { params }),

    updateStatus: (id: number, status: number) =>
      request.put(`/admin/product/status/${id}?status=${status}`),

    updateStock: (id: number, stock: number) =>
      request.put(`/admin/product/stock/${id}?stock=${stock}`),

    delete: (id: number) => request.delete(`/admin/product/delete/${id}`)
  },

  order: {
    // 获取有已支付订单的用户列表
    getUsers: () => request.get('/admin/order/users'),

    // 获取指定用户的订单
    getUserOrders: (userId: number, params?: any) =>
      request.get(`/admin/order/user/${userId}`, { params })
  }
}

// 文件上传
export const uploadApi = {
  uploadImage: (file: File, type: 'product' | 'avatar' = 'product') => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)
    return request.post('/upload/image', formData)
  }
}

// 警告API
export const warningApi = {
  sendWarning: (userId: number, reason: string) =>
    request.post('/warning/send', { userId, reason }),

  getMyWarnings: () => request.get('/warning/my'),

  getAllWarnings: () => request.get('/warning/all'),

  markAsRead: (id: number) => request.put(`/warning/read/${id}`),

  markAllAsRead: () => request.put('/warning/readAll'),

  getUnreadCount: () => request.get('/warning/unreadCount')
}

// 智能客服API
export const chatbotApi = {
  chat: (data: { message: string; history: { role: string; content: string }[] }) =>
    request.post('/chatbot/chat', data, { timeout: 120000 }),
}
