<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { chatbotApi } from '@/api'
import { ChatDotRound, Close, Position, User, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { marked } from 'marked'
import katex from 'katex'
import 'katex/dist/katex.min.css'

marked.setOptions({ breaks: true, gfm: true })

// Decode HTML entities
function decodeEntities(str: string): string {
  return str
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'")
    .replace(/&gt;/g, '>')
    .replace(/&lt;/g, '<')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#x27;/g, "'")
    .replace(/&#x3E;/g, '>')
    .replace(/&#x3C;/g, '<')
}

function renderLatex(html: string): string {
  // Decode entities first so KaTeX can parse the formulas
  html = decodeEntities(html)
  // Display math: $$...$$
  html = html.replace(/\$\$([\s\S]+?)\$\$/g, (_, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: true, throwOnError: false })
    } catch { return _ }
  })
  // Inline math: $...$
  html = html.replace(/\$([^\$\n]+?)\$/g, (_, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: false, throwOnError: false })
    } catch { return _ }
  })
  return html
}

function renderMarkdown(text: string) {
  // Protect LaTeX expressions before marked processes them
  const latexMap: string[] = []
  let protectedText = text

  // Extract $$...$$ display math
  protectedText = protectedText.replace(/\$\$([\s\S]+?)\$\$/g, (_, tex) => {
    const placeholder = `%%LATEX_${latexMap.length}%%`
    latexMap.push(`$${tex}$`)  // Store as $...$ for KaTeX
    return placeholder
  })

  // Extract $...$ inline math
  protectedText = protectedText.replace(/\$([^\$\n]+?)\$/g, (_, tex) => {
    const placeholder = `%%LATEX_${latexMap.length}%%`
    latexMap.push(tex)
    return placeholder
  })

  // Process markdown
  let html = marked.parse(protectedText) as string

  // Decode entities
  html = decodeEntities(html)

  // Restore and render LaTeX
  for (let i = 0; i < latexMap.length; i++) {
    const tex = latexMap[i]
    const isDisplay = tex.startsWith('$$')
    const cleanTex = isDisplay ? tex.slice(2, -2).trim() : tex.trim()
    try {
      const rendered = katex.renderToString(cleanTex, { displayMode: isDisplay, throwOnError: false })
      html = html.replace(`%%LATEX_${i}%%`, rendered)
    } catch {
      html = html.replace(`%%LATEX_${i}%%`, tex)
    }
  }

  return html
}

const userStore = useUserStore()
const isOpen = ref(false)
const inputValue = ref('')
const loading = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

// ── 拖拽状态 ──
const isDragging = ref(false)
const posX = ref(window.innerWidth - 80)
const posY = ref(window.innerHeight - 80)
let dragStartX = 0
let dragStartY = 0
let startPosX = 0
let startPosY = 0
let hasMoved = false

const STORAGE_KEY = 'chatbot-position'

// ── 面板大小调整 ──
const panelWidth = ref(380)
const panelHeight = ref(520)
const isResizing = ref(false)
let resizeStartX = 0
let resizeStartY = 0
let resizeStartW = 0
let resizeStartH = 0

function onResizeStart(e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  isResizing.value = true
  resizeStartX = e.clientX
  resizeStartY = e.clientY
  resizeStartW = panelWidth.value
  resizeStartH = panelHeight.value
  document.addEventListener('mousemove', onResizeMove)
  document.addEventListener('mouseup', onResizeEnd)
}

function onResizeMove(e: MouseEvent) {
  if (!isResizing.value) return
  const dx = resizeStartX - e.clientX
  const dy = e.clientY - resizeStartY
  panelWidth.value = Math.max(300, Math.min(window.innerWidth - 40, resizeStartW + dx))
  panelHeight.value = Math.max(350, Math.min(window.innerHeight - 40, resizeStartH + dy))
}

function onResizeEnd() {
  isResizing.value = false
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
  localStorage.setItem(STORAGE_KEY + '-size', JSON.stringify({ w: panelWidth.value, h: panelHeight.value }))
}

function onDragStart(e: MouseEvent) {
  e.preventDefault()
  isDragging.value = true
  hasMoved = false
  dragStartX = e.clientX
  dragStartY = e.clientY
  startPosX = posX.value
  startPosY = posY.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
}

function onDragMove(e: MouseEvent) {
  if (!isDragging.value) return
  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  posX.value = Math.max(0, Math.min(window.innerWidth - 56, startPosX + dx))
  posY.value = Math.max(0, Math.min(window.innerHeight - 56, startPosY + dy))
}

function onDragEnd() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ x: posX.value, y: posY.value }))
}

function onTriggerClick() {
  if (hasMoved) return
  isOpen.value = !isOpen.value
  if (isOpen.value) nextTick(scrollToBottom)
}

onMounted(() => {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '')
    if (saved && typeof saved.x === 'number') {
      posX.value = Math.max(0, Math.min(window.innerWidth - 56, saved.x))
      posY.value = Math.max(0, Math.min(window.innerHeight - 56, saved.y))
    }
    const sizeSaved = JSON.parse(localStorage.getItem(STORAGE_KEY + '-size') || '')
    if (sizeSaved && typeof sizeSaved.w === 'number') {
      panelWidth.value = Math.max(300, sizeSaved.w)
      panelHeight.value = Math.max(350, sizeSaved.h)
    }
  } catch {}
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
})

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const CHAT_STORAGE = 'chatbot-history'
const welcomeMsg: Message = { role: 'assistant', content: '你好！我是CampusMart智能客服助手，有什么可以帮你的吗？' }

function getStorageKey() {
  const user = userStore.userInfo
  return CHAT_STORAGE + '-' + (user?.id || 'guest')
}

function loadMessages(): Message[] {
  try {
    const saved = localStorage.getItem(getStorageKey())
    if (saved) return JSON.parse(saved)
  } catch {}
  return [welcomeMsg]
}

const messages = ref<Message[]>(loadMessages())

function saveMessages() {
  localStorage.setItem(getStorageKey(), JSON.stringify(messages.value))
}

function clearChat() {
  messages.value = [welcomeMsg]
  saveMessages()
}

async function sendMessage() {
  const text = inputValue.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  saveMessages()
  inputValue.value = ''
  loading.value = true
  nextTick(scrollToBottom)

  try {
    const history = messages.value.slice(0, -1).map(m => ({
      role: m.role,
      content: m.content
    }))
    const res = await chatbotApi.chat({ message: text, history })
    messages.value.push({ role: 'assistant', content: res.reply })
    saveMessages()
  } catch (e: any) {
    const errMsg = e?.response?.data?.message || e?.message || '未知错误'
    messages.value.push({ role: 'assistant', content: '请求失败：' + errMsg })
    saveMessages()
  } finally {
    loading.value = false
    nextTick(scrollToBottom)
  }
}

function scrollToBottom() {
  const el = messagesContainer.value
  if (el) el.scrollTop = el.scrollHeight
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}
</script>

<template>
  <div
    class="chat-widget"
    v-if="userStore.isLoggedIn"
    :style="{ left: posX + 'px', top: posY + 'px' }"
  >
    <!-- 浮动按钮（可拖拽） -->
    <div
      class="chat-trigger"
      :class="{ dragging: isDragging }"
      @mousedown="onDragStart"
      @click="onTriggerClick"
    >
      <el-icon :size="24" v-if="!isOpen"><ChatDotRound /></el-icon>
      <el-icon :size="24" v-else><Close /></el-icon>
    </div>

    <!-- 聊天面板 -->
    <Transition name="chat-panel">
      <div class="chat-panel" v-show="isOpen" :style="{ width: panelWidth + 'px', height: panelHeight + 'px' }">
        <div class="chat-header">
          <div class="header-info">
            <el-icon :size="20"><ChatDotRound /></el-icon>
            <span>智能客服</span>
          </div>
          <div class="header-actions">
            <el-icon class="action-btn" :size="16" title="清空记录" @click="clearChat"><Delete /></el-icon>
            <el-icon class="action-btn" :size="18" @click="isOpen = false"><Close /></el-icon>
          </div>
        </div>

        <div class="chat-messages" ref="messagesContainer">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message"
            :class="msg.role"
          >
            <div class="message-avatar" v-if="msg.role === 'assistant'">
              <el-icon :size="16"><ChatDotRound /></el-icon>
            </div>
            <div class="message-bubble" v-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)"></div>
            <div class="message-bubble" v-else>{{ msg.content }}</div>
            <div class="message-avatar" v-if="msg.role === 'user'">
              <el-icon :size="16"><User /></el-icon>
            </div>
          </div>

          <div class="message assistant" v-if="loading">
            <div class="message-avatar">
              <el-icon :size="16"><ChatDotRound /></el-icon>
            </div>
            <div class="message-bubble typing">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputValue"
            placeholder="输入你的问题..."
            :disabled="loading"
            @keydown="handleKeydown"
            size="default"
          >
            <template #append>
              <el-button
                :icon="Position"
                @click="sendMessage"
                :disabled="loading || !inputValue.trim()"
                type="primary"
              />
            </template>
          </el-input>
        </div>

        <!-- Resize handle -->
        <div class="resize-handle" @mousedown="onResizeStart">
          <svg width="12" height="12" viewBox="0 0 12 12"><path d="M11 1v10H1M11 11L1 1" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.chat-widget {
  position: fixed;
  z-index: 9999;
}

.chat-trigger {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary, #22c55e);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  user-select: none;
  box-shadow: 0 4px 16px rgba(34, 197, 94, 0.4);
  transition: box-shadow 0.2s ease, transform 0.15s ease;
}

.chat-trigger:hover {
  box-shadow: 0 6px 24px rgba(34, 197, 94, 0.5);
}

.chat-trigger.dragging {
  cursor: grabbing;
  transform: scale(1.1);
  box-shadow: 0 8px 32px rgba(34, 197, 94, 0.6);
  transition: none;
}

.chat-panel {
  position: absolute;
  bottom: 72px;
  right: 0;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 16px 20px;
  background: var(--color-primary, #22c55e);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-btn {
  cursor: pointer;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.action-btn:hover {
  opacity: 1;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.message.user {
  justify-content: flex-end;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.assistant .message-avatar {
  background: var(--color-primary-light, #dcfce7);
  color: var(--color-primary, #22c55e);
}

.user .message-avatar {
  background: #f0f0f0;
  color: #666;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-bubble :deep(h1),
.message-bubble :deep(h2),
.message-bubble :deep(h3) {
  font-size: 14px;
  font-weight: 600;
  margin: 8px 0 4px;
}

.message-bubble :deep(p) {
  margin: 4px 0;
}

.message-bubble :deep(ul),
.message-bubble :deep(ol) {
  margin: 4px 0;
  padding-left: 20px;
}

.message-bubble :deep(li) {
  margin: 2px 0;
}

.message-bubble :deep(strong) {
  font-weight: 600;
}

.message-bubble :deep(code) {
  background: rgba(0,0,0,0.06);
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 13px;
}

.message-bubble :deep(pre) {
  background: rgba(0,0,0,0.06);
  padding: 8px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 6px 0;
}

.message-bubble :deep(pre code) {
  background: none;
  padding: 0;
}

.message-bubble :deep(.katex-display) {
  margin: 8px 0;
  overflow-x: auto;
  overflow-y: hidden;
}

.message-bubble :deep(.katex) {
  font-size: 1em;
}

.assistant .message-bubble {
  background: #f5f5f5;
  color: #333;
  border-top-left-radius: 4px;
}

.user .message-bubble {
  background: var(--color-primary, #22c55e);
  color: #fff;
  border-top-right-radius: 4px;
}

.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  animation: bounce 1.4s ease-in-out infinite;
}

.dot:nth-child(1) { animation-delay: 0s; }
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-6px); }
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #eee;
}

.chat-input :deep(.el-input__wrapper) {
  border-radius: 24px;
  padding: 4px 8px 4px 16px;
  box-shadow: none;
  border: 1px solid #e0e0e0;
}

.chat-input :deep(.el-input__wrapper:focus-within) {
  border-color: var(--color-primary, #22c55e);
}

.chat-input :deep(.el-input-group__append) {
  background: var(--color-primary, #22c55e);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-input :deep(.el-input-group__append .el-button) {
  color: #fff;
  margin: 0;
}

/* Resize handle */
.resize-handle {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 20px;
  height: 20px;
  cursor: nwse-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  transition: color 0.2s;
  z-index: 10;
}

.resize-handle:hover {
  color: var(--color-primary, #22c55e);
}

.chat-panel-enter-active,
.chat-panel-leave-active {
  transition: all 0.3s ease;
}

.chat-panel-enter-from,
.chat-panel-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.95);
}

@media (max-width: 480px) {
  .chat-panel {
    width: calc(100vw - 32px) !important;
    right: -8px;
    height: 70vh !important;
  }
}
</style>
