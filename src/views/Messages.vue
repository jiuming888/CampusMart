<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatRelativeTime } from '@/utils'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const conversations = ref<any[]>([])
const messages = ref<any[]>([])
const loading = ref(true)
const chatLoading = ref(false)
const activeUserId = ref<number | null>(null)
const activeUserInfo = ref<any>(null)
const newMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

// 定时刷新未读数
let refreshInterval: number | null = null

onMounted(async () => {
  await loadConversations()
  
  // 如果有目标用户ID，直接打开对话
  if (route.query.userId) {
    const userId = Number(route.query.userId)
    // 获取用户信息
    await getUserInfo(userId)
    await openConversation(userId)
  }
  
  // 定时刷新对话列表
  refreshInterval = window.setInterval(() => {
    if (conversations.value.length > 0) {
      loadConversations()
    }
  }, 5000)
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})

async function loadConversations() {
  try {
    const res = await messageApi.getConversations()
    conversations.value = res || []
  } catch (e) {
    console.error('获取对话列表失败', e)
  } finally {
    loading.value = false
  }
}

async function getUserInfo(userId: number) {
  try {
    const res = await messageApi.getUserInfo(userId)
    activeUserInfo.value = res
  } catch (e) {
    // 如果获取失败，使用默认值
    activeUserInfo.value = { id: userId, nickname: '用户' + userId }
  }
}

async function openConversation(userId: number) {
  if (activeUserId.value === userId) return
  
  activeUserId.value = userId
  messages.value = []
  chatLoading.value = true
  
  await getUserInfo(userId)
  
  try {
    const res = await messageApi.getDetail(userId)
    messages.value = res || []
    
    // 标记已读
    await messageApi.markAsRead(userId)
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
    
    // 更新对话列表中的未读数
    await loadConversations()
  } catch (e) {
    console.error('获取消息详情失败', e)
  } finally {
    chatLoading.value = false
  }
}

async function sendMessage() {
  if (!newMessage.value.trim() || !activeUserId.value) return
  
  const content = newMessage.value.trim()
  newMessage.value = ''
  
  try {
    await messageApi.send({
      toUserId: activeUserId.value,
      productId: route.query.productId ? Number(route.query.productId) : undefined,
      content
    })
    
    // 重新获取消息列表
    const res = await messageApi.getDetail(activeUserId.value)
    messages.value = res || []
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
    newMessage.value = content // 恢复消息内容
  }
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function goBack() {
  activeUserId.value = null
  activeUserInfo.value = null
  messages.value = []
  // 清除URL参数
  router.replace('/messages')
}

// 点击评论人头像跳转私信
function goToMessage(userId: number) {
  router.push({
    path: '/messages',
    query: { userId: userId.toString() }
  })
}

defineExpose({ goToMessage })
</script>

<template>
  <div class="messages-page">
    <div class="container">
      <div class="page-header">
        <h1>私信</h1>
      </div>
      
      <div class="messages-container">
        <!-- 对话列表 -->
        <div class="conversations-list" :class="{ hidden: activeUserId }">
          <div v-if="loading" class="loading-container">
            <el-icon class="is-loading" :size="40"><Loading /></el-icon>
          </div>
          
          <div v-else-if="conversations.length === 0" class="empty-state">
            <div class="icon"><el-icon :size="64"><ChatDotRound /></el-icon></div>
            <p>暂无消息</p>
            <p class="sub-tip">在商品评论区点击用户头像即可发起私信</p>
          </div>
          
          <div v-else class="conversation-items">
            <div 
              v-for="conv in conversations" 
              :key="conv.userId"
              class="conversation-item"
              :class="{ unread: conv.unreadCount > 0 }"
              @click="openConversation(conv.userId)"
            >
              <div class="avatar-wrapper">
                <el-avatar :size="52" :src="conv.avatar || undefined">
                  {{ conv.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span v-if="conv.unreadCount > 0" class="unread-dot"></span>
              </div>
              <div class="conv-info">
                <div class="conv-header">
                  <span class="user-name">{{ conv.nickname || '用户' + conv.userId }}</span>
                  <span class="time">{{ formatRelativeTime(conv.lastTime) }}</span>
                </div>
                <p class="last-message">{{ conv.lastMessage }}</p>
              </div>
              <el-badge 
                v-if="conv.unreadCount > 0" 
                :value="conv.unreadCount" 
                :max="99" 
                class="unread-badge"
              />
            </div>
          </div>
        </div>
        
        <!-- 聊天窗口 -->
        <div class="chat-window" :class="{ active: activeUserId }">
          <template v-if="activeUserId">
            <div class="chat-header">
              <div class="header-left">
                <el-button text @click="goBack" class="back-btn">
                  <el-icon><ArrowLeft /></el-icon>
                </el-button>
                <el-avatar :size="36" :src="activeUserInfo?.avatar || undefined">
                  {{ activeUserInfo?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="chat-with-name">{{ activeUserInfo?.nickname || '用户' + activeUserId }}</span>
              </div>
              <div class="header-right">
                <el-button text @click="router.push(`/seller/${activeUserId}`)">
                  <el-icon><User /></el-icon>
                  <span>TA的主页</span>
                </el-button>
              </div>
            </div>
            
            <div ref="messagesContainer" class="chat-messages">
              <div v-if="chatLoading" class="loading-container">
                <el-icon class="is-loading" :size="32"><Loading /></el-icon>
              </div>
              
              <div v-else-if="messages.length === 0" class="chat-empty-hint">
                <span>开始和对方聊聊吧~</span>
              </div>
              
              <div v-else>
                <div 
                  v-for="msg in messages" 
                  :key="msg.id"
                  class="message-item"
                  :class="{ mine: msg.fromUserId === userStore.userInfo?.id }"
                >
                  <el-avatar 
                    :size="40" 
                    :src="msg.fromUserId === userStore.userInfo?.id ? userStore.userInfo?.avatar : msg.fromUserAvatar"
                    class="msg-avatar"
                  >
                    {{ (msg.fromUserId === userStore.userInfo?.id ? userStore.userInfo?.nickname : msg.fromUserNickname)?.charAt(0) || 'U' }}
                  </el-avatar>
                  <div class="message-bubble">
                    <p>{{ msg.content }}</p>
                    <span class="time">{{ formatRelativeTime(msg.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="chat-input">
              <el-input
                v-model="newMessage"
                placeholder="输入消息..."
                @keyup.enter="sendMessage"
                maxlength="500"
              />
              <el-button type="primary" @click="sendMessage" :disabled="!newMessage.trim()">
                发送
              </el-button>
            </div>
          </template>
          
          <div v-else class="chat-empty">
            <div class="empty-icon"><el-icon :size="64"><ChatDotRound /></el-icon></div>
            <span>选择对话开始聊天</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Loading, ArrowLeft, User, ChatDotRound } from '@element-plus/icons-vue'
export default {
  components: { Loading, ArrowLeft, User, ChatDotRound }
}
</script>

<style scoped>
.messages-page {
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

.messages-container {
  display: flex;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  height: calc(100vh - 250px);
  min-height: 500px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* 对话列表 */
.conversations-list {
  width: 340px;
  border-right: 1px solid #f0f0f0;
  overflow-y: auto;
  background: #fafafa;
}

.conversations-list::-webkit-scrollbar {
  width: 4px;
}

.conversations-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

.conversation-items {
  padding: 8px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.conversation-item:hover {
  background: #f0f0f0;
}

.conversation-item.unread {
  background: #fff5f2;
}

.conversation-item.unread:hover {
  background: #ffe8e2;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.unread-dot {
  position: absolute;
  top: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #FF6B35;
  border-radius: 50%;
  border: 2px solid #fafafa;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.user-name {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.time {
  color: #999;
  font-size: 12px;
}

.last-message {
  color: #666;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-badge {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
}

/* 聊天窗口 */
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  gap: 12px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.5;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  font-size: 20px;
}

.chat-with-name {
  font-weight: 600;
  font-size: 16px;
}

.header-right .el-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-right span {
  font-size: 13px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f8f8;
}

.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 2px;
}

.chat-empty-hint {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-item.mine {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  background: #fff;
  border-radius: 16px;
  position: relative;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-item.mine .message-bubble {
  background: #FF6B35;
  color: #fff;
}

.message-bubble p {
  word-break: break-all;
  line-height: 1.5;
  font-size: 15px;
}

.message-bubble .time {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.message-item.mine .message-bubble .time {
  color: rgba(255, 255, 255, 0.7);
  text-align: right;
}

.chat-input {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.chat-input .el-input {
  flex: 1;
}

.chat-input .el-button--primary {
  background: #FF6B35;
  border-color: #FF6B35;
}

.chat-input .el-button--primary:hover {
  background: #ff5722;
  border-color: #ff5722;
}

.chat-input .el-button--primary:disabled {
  background: #ffccbc;
  border-color: #ffccbc;
}

.hidden {
  display: none;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  gap: 8px;
}

.empty-state .icon {
  font-size: 48px;
  opacity: 0.5;
}

.empty-state .sub-tip {
  font-size: 12px;
  color: #bbb;
}

@media (max-width: 768px) {
  .conversations-list {
    width: 100%;
  }
  
  .conversations-list.hidden {
    display: none;
  }
  
  .chat-window {
    display: none;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 100;
    background: #fff;
  }
  
  .chat-window.active {
    display: flex;
  }
}
</style>
