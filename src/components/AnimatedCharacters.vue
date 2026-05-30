<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps<{
  isTyping?: boolean
  showPassword?: boolean
  passwordLength?: number
  context?: 'login' | 'register'
  typingField?: 'username' | 'password' | null
}>()

// ── Mouse tracking (original logic) ──
const mouseX = ref(0)
const mouseY = ref(0)
const isPurpleBlinking = ref(false)
const isBlackBlinking = ref(false)
const isLookingAtEachOther = ref(false)

const purpleRef = ref<HTMLElement | null>(null)
const blackRef = ref<HTMLElement | null>(null)
const yellowRef = ref<HTMLElement | null>(null)
const orangeRef = ref<HTMLElement | null>(null)

onMounted(() => {
  window.addEventListener('mousemove', onMove)
  blinkLoop('purple')
  blinkLoop('black')
})
onUnmounted(() => {
  window.removeEventListener('mousemove', onMove)
})

function onMove(e: MouseEvent) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

function pos(el: HTMLElement | null) {
  if (!el) return { fx: 0, fy: 0, sk: 0 }
  const r = el.getBoundingClientRect()
  const dx = mouseX.value - (r.left + r.width / 2)
  const dy = mouseY.value - (r.top + r.height / 3)
  return {
    fx: Math.max(-15, Math.min(15, dx / 20)),
    fy: Math.max(-10, Math.min(10, dy / 30)),
    sk: Math.max(-6, Math.min(6, -dx / 120)),
  }
}

function blinkLoop(who: 'purple' | 'black') {
  const set = who === 'purple' ? isPurpleBlinking : isBlackBlinking
  setTimeout(() => {
    set.value = true
    setTimeout(() => { set.value = false; blinkLoop(who) }, 150)
  }, Math.random() * 4000 + 3000)
}

// ── Looking at each other ──
watch(() => props.isTyping, (v) => {
  if (v) {
    isLookingAtEachOther.value = true
    setTimeout(() => { isLookingAtEachOther.value = false }, 800)
  } else {
    isLookingAtEachOther.value = false
  }
})

// ── Speech bubbles ──
interface Bubble { id: number; text: string; char: string }
const bubbles = ref<Bubble[]>([])
let bid = 0
let bTimer: ReturnType<typeof setInterval> | null = null

const usernameTexts = [
  '这名字不错哦~', '想个好听的用户名', '用户名要好记才行', '取名字是门学问',
  '让我看看你叫什么', '这用户名有品味', '起个独一无二的名字', '你的用户名好特别',
  '就决定用这个了？', '这名字我喜欢', '简单好记最重要', '用户名选好了吗',
  '我猜你会选个好名字', '要不要再想想？', '这名字真有创意', '好名字是成功的第一步'
]
const passwordTexts = [
  '在偷偷输密码...', '什么神仙密码', '这密码也太长了吧', '输快点输快点',
  '我什么都没看到', '认真输密码的样子真帅', '密码要设复杂点哦', '安全第一安全第一',
  '这密码我能猜到吗', '密码强度很重要', '设个好记又安全的密码', '别忘了密码哦',
  '我闭眼了我闭眼了', '输密码的手速真快', '这密码保护工作做得好', '密码不能太简单哦',
  '你输密码好认真', '这密码安全性满分', '我不看我不看', '密码长一点更安全'
]
const peekTexts = [
  '不许偷看！', '诶诶诶别看！', '保密工作不错', '眼睛要瞎了',
  '这也太明目张胆了', '你的良心不会痛吗', '偷看密码不礼貌哦', '快把眼睛闭上',
  '我要告诉别人你在偷看', '这也太过分了吧', '密码是隐私懂不懂', '别看别看别看',
  '你在看什么看', '胆子也太大了', '我要生气了哦', 'nonono别看'
]
const loginIdle = [
  '快登录呀~', '今天也要好好交易哦', '闲置物品等你来淘', 'CampusMart 欢迎你',
  '你在犹豫什么呢', '好东西不等人哦', '校园交易就在这里', '登录开启新篇章',
  '有好多宝贝等你选', '今天想买点什么', '二手好物超划算', '来逛逛吧来逛逛吧',
  '你的闲置也能变钱', '同学们都在用呢', '买卖二手就上CampusMart', '新学期新装备'
]
const loginIdleUrging = [
  '还在犹豫什么呢？', '快点登录嘛~', '再不登录东西就被抢光啦', '登录只要3秒钟哦',
  '手别抖直接冲', '别磨蹭啦快登录', '再等下去好货就没了', '你看我们都等急了',
  '快快快快快登录', '急死我了急死我了', '你到底登不登啊', '我都要替你着急了',
  '登录一下又不会怎样', '别让我们干等着呀', '赶紧的赶紧的', '输入账号密码就好啦'
]
const registerIdle = [
  '快来加入我们~', '注册就送惊喜哦', '校园交易从这里开始', '等你好久啦~',
  '新朋友你好呀', '加入CampusMart大家庭', '注册超简单的', '一分钟就能搞定',
  '成为会员享受更多福利', '校园买卖就靠它了', '注册后可以发布闲置哦', '你的大学生活从这里开始',
  '买卖二手先注册', '注册不收费哦', '快来成为第1000位用户', '校园交易等你来'
]
const registerIdleUrging = [
  '注册很快的~', '填完就能开始交易啦', '马上就好了，加油！',
  '就差一点点了', '手指动一动就注册好了', '别放弃就快成功了',
  '再填几项就完事了', '注册好简单的好吗', '快点嘛快点嘛',
  '我们都在等你呢', '你是不是被难住了', '有问题可以问我们哦'
]
const allChars = ['purple', 'black', 'orange', 'yellow']

let idleCount = 0

function getIdleTexts() {
  idleCount++
  // After 3 idle messages, start urging
  if (idleCount > 3) {
    return props.context === 'register' ? registerIdleUrging : loginIdleUrging
  }
  return props.context === 'register' ? registerIdle : loginIdle
}

function getTypingTexts() {
  if (props.typingField === 'username') return usernameTexts
  if (props.typingField === 'password') return passwordTexts
  // Default: mix of both
  return [...usernameTexts, ...passwordTexts]
}

function rand<T>(a: T[]) { return a[Math.floor(Math.random() * a.length)] }

function addBubble(text: string, char: string) {
  const id = ++bid
  bubbles.value.push({ id, text, char })
  setTimeout(() => { bubbles.value = bubbles.value.filter(b => b.id !== id) }, 2500)
}

function bubblePos(char: string) {
  const m: Record<string, { left: string; top: string }> = {
    purple: { left: '90px', top: '-10px' },
    black: { left: '250px', top: '40px' },
    orange: { left: '60px', top: '120px' },
    yellow: { left: '320px', top: '90px' },
  }
  return m[char] || m.purple
}

watch(() => props.isTyping, (v) => {
  if (v) {
    idleCount = 0  // Reset idle counter when typing
    addBubble(rand(getTypingTexts()), rand(allChars))
    bTimer = setInterval(() => {
      if (props.isTyping) {
        // Occasionally show two bubbles at once from different characters
        const c1 = rand(allChars)
        addBubble(rand(getTypingTexts()), c1)
        if (Math.random() > 0.5) {
          const c2 = rand(allChars.filter(c => c !== c1))
          setTimeout(() => addBubble(rand(getTypingTexts()), c2), 400)
        }
      }
    }, 1800)
  } else {
    if (bTimer) { clearInterval(bTimer); bTimer = null }
  }
})

watch(() => props.showPassword, (v) => {
  if (v && props.passwordLength && props.passwordLength > 0) {
    addBubble(rand(peekTexts), 'purple')
    setTimeout(() => addBubble(rand(peekTexts), 'black'), 1200)
  }
})

onMounted(() => {
  setTimeout(() => addBubble(props.context === 'register' ? '欢迎注册 CampusMart~' : '欢迎来到 CampusMart~', 'yellow'), 800)
  setTimeout(() => addBubble(props.context === 'register' ? '新朋友你好呀~' : '有什么可以帮你的吗~', 'purple'), 2200)
  setTimeout(() => addBubble(props.context === 'register' ? '注册很简单哦~' : '快来试试吧~', 'orange'), 3800)
  setInterval(() => {
    if (!props.isTyping && bubbles.value.length === 0) addBubble(rand(getIdleTexts()), rand(allChars))
  }, 3500)
})
</script>

<template>
  <div class="animated-characters">
    <!-- Speech Bubbles -->
    <TransitionGroup name="bubble">
      <div v-for="b in bubbles" :key="b.id" class="speech-bubble" :style="bubblePos(b.char)">
        {{ b.text }}
      </div>
    </TransitionGroup>

    <!-- Purple tall character -->
    <div ref="purpleRef" class="char char-purple" :style="{
      transform: isLookingAtEachOther
        ? `skewX(${pos(purpleRef).sk - 12}deg) translateX(40px)`
        : `skewX(${pos(purpleRef).sk}deg)`,
    }">
      <div class="eyes" :style="{
        left: isLookingAtEachOther ? '55px' : `${45 + pos(purpleRef).fx}px`,
        top: isLookingAtEachOther ? '65px' : `${40 + pos(purpleRef).fy}px`,
      }">
        <div class="eye-white">
          <div v-if="!isPurpleBlinking" class="pupil" :style="{
            transform: isLookingAtEachOther ? 'translate(3px, 4px)' : `translate(${pos(purpleRef).fx * 0.4}px, ${pos(purpleRef).fy * 0.4}px)`,
          }" />
        </div>
        <div class="eye-white">
          <div v-if="!isPurpleBlinking" class="pupil" :style="{
            transform: isLookingAtEachOther ? 'translate(3px, 4px)' : `translate(${pos(purpleRef).fx * 0.4}px, ${pos(purpleRef).fy * 0.4}px)`,
          }" />
        </div>
      </div>
    </div>

    <!-- Black tall character -->
    <div ref="blackRef" class="char char-black" :style="{
      transform: isLookingAtEachOther
        ? `skewX(${(pos(blackRef).sk) * 1.5 + 10}deg) translateX(20px)`
        : `skewX(${pos(blackRef).sk * 1.5}deg)`,
    }">
      <div class="eyes" :style="{
        left: isLookingAtEachOther ? '32px' : `${26 + pos(blackRef).fx}px`,
        top: isLookingAtEachOther ? '12px' : `${32 + pos(blackRef).fy}px`,
      }">
        <div class="eye-white eye-white-sm">
          <div v-if="!isBlackBlinking" class="pupil pupil-sm" :style="{
            transform: isLookingAtEachOther ? 'translate(0px, -4px)' : `translate(${pos(blackRef).fx * 0.4}px, ${pos(blackRef).fy * 0.4}px)`,
          }" />
        </div>
        <div class="eye-white eye-white-sm">
          <div v-if="!isBlackBlinking" class="pupil pupil-sm" :style="{
            transform: isLookingAtEachOther ? 'translate(0px, -4px)' : `translate(${pos(blackRef).fx * 0.4}px, ${pos(blackRef).fy * 0.4}px)`,
          }" />
        </div>
      </div>
    </div>

    <!-- Orange semi-circle character -->
    <div ref="orangeRef" class="char char-orange" :style="{
      transform: `skewX(${pos(orangeRef).sk}deg)`,
    }">
      <div class="eyes eyes-pupils" :style="{
        left: `${82 + pos(orangeRef).fx}px`,
        top: `${90 + pos(orangeRef).fy}px`,
      }">
        <div class="pupil-only" :style="{ transform: `translate(${pos(orangeRef).fx * 0.4}px, ${pos(orangeRef).fy * 0.4}px)` }" />
        <div class="pupil-only" :style="{ transform: `translate(${pos(orangeRef).fx * 0.4}px, ${pos(orangeRef).fy * 0.4}px)` }" />
      </div>
    </div>

    <!-- Yellow character -->
    <div ref="yellowRef" class="char char-yellow" :style="{
      transform: `skewX(${pos(yellowRef).sk}deg)`,
    }">
      <div class="eyes eyes-pupils" :style="{
        left: `${52 + pos(yellowRef).fx}px`,
        top: `${40 + pos(yellowRef).fy}px`,
      }">
        <div class="pupil-only" :style="{ transform: `translate(${pos(yellowRef).fx * 0.4}px, ${pos(yellowRef).fy * 0.4}px)` }" />
        <div class="pupil-only" :style="{ transform: `translate(${pos(yellowRef).fx * 0.4}px, ${pos(yellowRef).fy * 0.4}px)` }" />
      </div>
      <div class="mouth" :style="{
        left: `${40 + pos(yellowRef).fx}px`,
        top: `${88 + pos(yellowRef).fy}px`,
      }" />
    </div>
  </div>
</template>

<style scoped>
.animated-characters { position: relative; width: 550px; height: 400px; }

/* Speech Bubbles */
.speech-bubble {
  position: absolute; z-index: 10; background: #fff; color: #333;
  padding: 8px 16px; border-radius: 12px; font-size: 13px; font-weight: 500;
  white-space: nowrap; box-shadow: 0 4px 16px rgba(0,0,0,0.12); pointer-events: none;
}
.speech-bubble::after {
  content: ''; position: absolute; bottom: -8px; left: 20px;
  border-left: 8px solid transparent; border-right: 8px solid transparent; border-top: 8px solid #fff;
}
.bubble-enter-active { transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1); }
.bubble-leave-active { transition: all 0.25s ease-in; }
.bubble-enter-from { opacity: 0; transform: translateY(10px) scale(0.85); }
.bubble-leave-to { opacity: 0; transform: translateY(-8px) scale(0.9); }

/* Characters */
.char { position: absolute; bottom: 0; transition: transform 0.7s cubic-bezier(0.25,0.1,0.25,1); transform-origin: bottom center; }
.char-purple { left: 70px; width: 180px; height: 400px; background: #6C3FF5; border-radius: 10px 10px 0 0; z-index: 1; }
.char-black { left: 240px; width: 120px; height: 310px; background: #2D2D2D; border-radius: 8px 8px 0 0; z-index: 2; }
.char-orange { left: 0; width: 240px; height: 200px; background: #FF9B6B; border-radius: 120px 120px 0 0; z-index: 3; }
.char-yellow { left: 310px; width: 140px; height: 230px; background: #E8D754; border-radius: 70px 70px 0 0; z-index: 4; }

/* Eyes */
.eyes { position: absolute; display: flex; gap: 32px; transition: all 0.7s cubic-bezier(0.25,0.1,0.25,1); }
.eyes-pupils { gap: 32px; transition: all 0.2s ease-out; }
.eye-white { width: 18px; height: 18px; border-radius: 50%; background: white; display: flex; align-items: center; justify-content: center; overflow: hidden; transition: height 0.15s ease; }
.eye-white-sm { width: 16px; height: 16px; }
.pupil { width: 7px; height: 7px; border-radius: 50%; background: #2D2D2D; transition: transform 0.1s ease-out; }
.pupil-sm { width: 6px; height: 6px; }
.pupil-only { width: 12px; height: 12px; border-radius: 50%; background: #2D2D2D; transition: transform 0.2s ease-out; }
.mouth { position: absolute; width: 80px; height: 4px; border-radius: 999px; background: #2D2D2D; transition: all 0.2s ease-out; }
</style>
