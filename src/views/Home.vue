<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productApi } from '@/api'
import { formatPrice, getFirstImage } from '@/utils'
import { HomeFilled, Right, Loading } from '@element-plus/icons-vue'

const router = useRouter()

const hotProducts = ref<any[]>([])
const latestProducts = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [hotRes, latestRes] = await Promise.all([
      productApi.getHotProducts(6),
      productApi.getLatestProducts(8)
    ])
    hotProducts.value = hotRes || []
    latestProducts.value = latestRes || []
  } catch (e) {
    console.error('获取数据失败', e)
  } finally {
    loading.value = false
  }
})

function goToProduct(id: number) {
  router.push(`/product/${id}`)
}

const banners = [
  { id: 1, title: '毕业季特惠', subtitle: '海量二手好物等你来', image: '/images/icons/banner-1.jpg' },
  { id: 2, title: '书籍回收', subtitle: '旧书换现金，省钱又环保', image: '/images/icons/banner-2.jpg' },
  { id: 3, title: '电子设备专区', subtitle: '学长学姐的数码好物', image: '/images/icons/banner-3.jpg' }
]
</script>

<template>
  <div class="home-page">
    <!-- Banner -->
    <section class="banner-section">
      <el-carousel height="420px" :interval="5000" indicator-position="none" arrow="always">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="banner-item" :style="{ backgroundImage: `url(${banner.image})` }">
            <div class="container banner-inner">
              <el-button class="banner-cta" size="large" round @click="router.push('/category/0')">
                去逛一逛
                <el-icon><Right /></el-icon>
              </el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <div class="container">
      <!-- 热门商品 -->
      <section class="product-section">
        <div class="section-header">
          <h2>热门商品</h2>
          <span class="subtitle">浏览量最高的宝贝</span>
          <router-link to="/category/0" class="more-link">
            查看更多 <el-icon><Right /></el-icon>
          </router-link>
        </div>

        <div v-if="loading" class="loading-grid">
          <div v-for="n in 4" :key="n" class="product-card-skeleton">
            <div class="skeleton skeleton-img" />
            <div class="skeleton-info">
              <div class="skeleton skeleton-title" />
              <div class="skeleton skeleton-price" />
              <div class="skeleton skeleton-meta" />
            </div>
          </div>
        </div>

        <div v-else class="product-grid">
          <div
            v-for="product in hotProducts"
            :key="product.id"
            class="product-card"
            @click="goToProduct(product.id)"
          >
            <div class="image-wrapper">
              <img
                :src="getFirstImage(product.images) || '/images/default-product.svg'"
                :alt="product.name"
                loading="lazy"
              />
              <span v-if="product.conditionLevel" class="condition-tag">
                {{ product.conditionLevel }}
              </span>
            </div>
            <div class="info">
              <h3 class="name">{{ product.name }}</h3>
              <div class="price-row">
                <span class="price">{{ formatPrice(product.price) }}</span>
                <span v-if="product.originalPrice" class="original-price">
                  {{ formatPrice(product.originalPrice) }}
                </span>
              </div>
              <div class="meta">
                <span class="location">
                  <el-icon :size="12"><HomeFilled /></el-icon>
                  {{ product.location || '未知地点' }}
                </span>
                <span class="views">{{ product.viewCount || 0 }}次浏览</span>
                <span class="stock-tag" :class="(product.stock ?? 0) > 0 ? 'in-stock' : 'out-stock'">
                  库存{{ product.stock ?? 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 最新上架 -->
      <section class="product-section">
        <div class="section-header">
          <h2>最新上架</h2>
          <span class="subtitle">刚刚上架的好物</span>
          <router-link to="/category/0" class="more-link">
            查看更多 <el-icon><Right /></el-icon>
          </router-link>
        </div>

        <div v-if="loading" class="loading-grid">
          <div v-for="n in 4" :key="n" class="product-card-skeleton">
            <div class="skeleton skeleton-img" />
            <div class="skeleton-info">
              <div class="skeleton skeleton-title" />
              <div class="skeleton skeleton-price" />
              <div class="skeleton skeleton-meta" />
            </div>
          </div>
        </div>

        <div v-else class="product-grid">
          <div
            v-for="product in latestProducts"
            :key="product.id"
            class="product-card"
            @click="goToProduct(product.id)"
          >
            <div class="image-wrapper">
              <img
                :src="getFirstImage(product.images) || '/images/default-product.svg'"
                :alt="product.name"
                loading="lazy"
              />
              <span v-if="product.conditionLevel" class="condition-tag">
                {{ product.conditionLevel }}
              </span>
            </div>
            <div class="info">
              <h3 class="name">{{ product.name }}</h3>
              <div class="price-row">
                <span class="price">{{ formatPrice(product.price) }}</span>
                <span v-if="product.originalPrice" class="original-price">
                  {{ formatPrice(product.originalPrice) }}
                </span>
              </div>
              <div class="meta">
                <span class="location">{{ product.location || '未知地点' }}</span>
                <span class="category">{{ product.categoryName }}</span>
                <span class="stock-tag" :class="(product.stock ?? 0) > 0 ? 'in-stock' : 'out-stock'">
                  库存{{ product.stock ?? 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  background: var(--color-background);
}

/* ── Banner ── */
.banner-section {
  margin-bottom: var(--space-12);
}

.banner-item {
  height: 420px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.banner-inner {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  height: 100%;
  padding-bottom: var(--space-8);
}

.banner-cta {
  --el-button-bg-color: rgba(255, 255, 255, 0.28);
  --el-button-border-color: rgba(255, 255, 255, 0.72);
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.38);
  --el-button-hover-border-color: #fff;
  min-width: 132px;
  font-weight: var(--font-semibold);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.22);
  backdrop-filter: blur(8px);
}

/* ── Section Header ── */
.section-header {
  display: flex;
  align-items: baseline;
  margin-bottom: var(--space-6);
}

.section-header h2 {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.section-header .subtitle {
  margin-left: var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.section-header .more-link {
  margin-left: auto;
  color: var(--color-primary);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  display: flex;
  align-items: center;
  gap: var(--space-1);
  transition: gap var(--transition-fast);
}

.section-header .more-link:hover {
  gap: var(--space-2);
}

/* ── Product Sections ── */
.product-section {
  margin-bottom: var(--space-12);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-5);
}

/* ── Loading skeleton ── */
.loading-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-5);
}

.product-card-skeleton {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--color-border-light);
}

.skeleton-img {
  padding-top: 100%;
}

.skeleton-info {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.skeleton-title {
  height: 16px;
  width: 80%;
}

.skeleton-price {
  height: 20px;
  width: 40%;
}

.skeleton-meta {
  height: 12px;
  width: 60%;
}

/* ── Condition tag ── */
.condition-tag {
  position: absolute;
  top: var(--space-3);
  left: var(--space-3);
  background: var(--color-primary-light);
  color: var(--color-primary-hover);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  padding: 2px 10px;
  border-radius: var(--radius-full);
}

/* ── Stock tag ── */
.stock-tag {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-weight: var(--font-medium);
}

.stock-tag.in-stock {
  background: var(--color-success-light);
  color: var(--color-success);
}

.stock-tag.out-stock {
  background: var(--color-error-light);
  color: var(--color-error);
}

/* ── Responsive ── */
@media (max-width: 1200px) {
  .product-grid, .loading-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .banner-item {
    height: 280px;
  }
  .banner-text h2 {
    font-size: var(--text-2xl);
  }
  .banner-text p {
    font-size: var(--text-base);
  }
  .product-grid, .loading-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-3);
  }
}
</style>
