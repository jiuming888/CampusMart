<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '@/api'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()

// 统计数据
const stats = ref({
  totalUsers: 0,
  newUsersThisWeek: 0,
  totalProducts: 0,
  pendingProducts: 0,
  totalOrders: 0,
  todayOrders: 0,
  todaySales: 0,
  totalSales: 0,
  pendingOrders: 0,
  disabledUsers: 0,
})

// 近7天订单趋势
const orderTrendOption = ref({})
// 近7天销售额趋势
const salesTrendOption = ref({})
// 订单状态分布
const orderStatusOption = ref({})
// 商品分类分布
const categoryOption = ref({})
// 商品状态分布
const productStatusOption = ref({})
// 近7天新增用户
const userTrendOption = ref({})
// 热门商品
const hotProducts = ref<any[]>([])
// 近期待处理订单
const recentOrders = ref<any[]>([])

const loading = ref(true)
const orderTrendLoading = ref(true)
const salesTrendLoading = ref(true)
const pieLoading = ref(true)
const hotLoading = ref(true)
const recentLoading = ref(true)

const formatMoney = (val: any) => {
  const num = Number(val) || 0
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toFixed(2)
}

const orderStatusMap: Record<string, string> = {
  'PENDING': '待付款',
  'PAID': '已付款',
  'SHIPPED': '已发货',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消',
}

const orderStatusColor: Record<string, string> = {
  'PENDING': '#FF9F43',
  'PAID': '#3688F4',
  'SHIPPED': '#8E44AD',
  'COMPLETED': '#2ECC71',
  'CANCELLED': '#95A5A6',
}

const productStatusMap: Record<string, string> = {
  '待审核': '待审核',
  '已上架': '已上架',
  '已下架': '已下架',
  '审核拒绝': '审核拒绝',
}

const productStatusColor: Record<string, string> = {
  '待审核': '#FF9F43',
  '已上架': '#2ECC71',
  '已下架': '#95A5A6',
  '审核拒绝': '#E74C3C',
}

const categoryColor = [
  '#5470C6', '#91CC75', '#FAC858', '#EE6666',
  '#73C0DE', '#3BA272', '#FC8452', '#9A60B4',
]

onMounted(async () => {
  await loadStats()
  await Promise.all([
    loadOrderTrend(),
    loadSalesTrend(),
    loadPieCharts(),
    loadHotProducts(),
    loadRecentOrders(),
  ])
  loading.value = false
})

async function loadStats() {
  try {
    const data = await adminApi.dashboard.getStats() as any
    stats.value = data
  } catch (e: any) {
    ElMessage.error('加载统计数据失败: ' + e.message)
  }
}

async function loadOrderTrend() {
  try {
    const data = await adminApi.dashboard.getOrdersWeek() as any[]
    orderTrendOption.value = {
      tooltip: { trigger: 'axis', confine: true },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.map(d => d.date),
        axisLine: { lineStyle: { color: '#E4E7ED' } },
        axisLabel: { color: '#606266' },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#F0F2F5', type: 'dashed' } },
        axisLabel: { color: '#606266' },
      },
      series: [{
        data: data.map(d => d.count),
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#3688F4', width: 3 },
        itemStyle: { color: '#3688F4' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(54,136,244,0.25)' },
              { offset: 1, color: 'rgba(54,136,244,0.02)' },
            ],
          },
        },
        emphasis: { scale: true },
      }],
    }
  } catch (e) {
    console.error('订单趋势加载失败', e)
  } finally {
    orderTrendLoading.value = false
  }
}

async function loadSalesTrend() {
  try {
    const data = await adminApi.dashboard.getSalesWeek() as any[]
    salesTrendOption.value = {
      tooltip: {
        trigger: 'axis',
        confine: true,
        formatter: (params: any) => {
          const p = params[0]
          return `${p.name}<br/><span style="color:#FF9F43;font-weight:bold">¥${Number(p.value).toFixed(2)}</span>`
        },
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.map(d => d.date),
        axisLine: { lineStyle: { color: '#E4E7ED' } },
        axisLabel: { color: '#606266' },
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#F0F2F5', type: 'dashed' } },
        axisLabel: {
          color: '#606266',
          formatter: (val: number) => '¥' + (val >= 1000 ? (val / 1000).toFixed(0) + 'k' : val),
        },
      },
      series: [{
        data: data.map(d => Number(d.amount) || 0),
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#FF9F43', width: 3 },
        itemStyle: { color: '#FF9F43' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(255,159,67,0.25)' },
              { offset: 1, color: 'rgba(255,159,67,0.02)' },
            ],
          },
        },
        emphasis: { scale: true },
      }],
    }
  } catch (e) {
    console.error('销售趋势加载失败', e)
  } finally {
    salesTrendLoading.value = false
  }
}

async function loadPieCharts() {
  try {
    const [orderStatusData, categoryData, productStatusData, userData] = await Promise.all([
      adminApi.dashboard.getOrdersByStatus() as Promise<any[]>,
      adminApi.dashboard.getProductsByCategory() as Promise<any[]>,
      adminApi.dashboard.getProductsByProductStatus() as Promise<any[]>,
      adminApi.dashboard.getUsersWeek() as Promise<any[]>,
    ])

    const pieCommon = {
      tooltip: { trigger: 'item', confine: true, textStyle: { fontSize: 12 } },
      legend: { bottom: '0', textStyle: { color: '#606266', fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
    }

    // 订单状态
    orderStatusOption.value = {
      ...pieCommon,
      series: [{
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: {
          show: false,
        },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 'bold' },
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.1)' },
        },
        data: orderStatusData.map((d: any, i: number) => ({
          name: d.name,
          value: d.value,
          itemStyle: { color: ['#FF9F43', '#3688F4', '#8E44AD', '#2ECC71', '#95A5A6'][i] },
        })),
      }],
    }

    // 商品分类
    categoryOption.value = {
      ...pieCommon,
      series: [{
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 'bold' },
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.1)' },
        },
        data: categoryData.map((d: any, i: number) => ({
          name: d.name,
          value: d.value,
          itemStyle: { color: categoryColor[i % categoryColor.length] },
        })),
      }],
    }

    // 商品状态
    productStatusOption.value = {
      ...pieCommon,
      series: [{
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 'bold' },
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.1)' },
        },
        data: productStatusData.map((d: any, i: number) => ({
          name: d.name,
          value: d.value,
          itemStyle: { color: ['#FF9F43', '#2ECC71', '#95A5A6', '#E74C3C'][i] },
        })),
      }],
    }

    // 新增用户趋势
    userTrendOption.value = {
      tooltip: { trigger: 'axis', confine: true },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: userData.map((d: any) => d.date),
        axisLine: { lineStyle: { color: '#E4E7ED' } },
        axisLabel: { color: '#606266' },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#F0F2F5', type: 'dashed' } },
        axisLabel: { color: '#606266' },
      },
      series: [{
        type: 'bar',
        data: userData.map((d: any) => d.count),
        barWidth: '50%',
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#73C0DE' },
              { offset: 1, color: '#3BA272' },
            ],
          },
          borderRadius: [6, 6, 0, 0],
        },
        emphasis: { itemStyle: { opacity: 0.85 } },
      }],
    }
  } catch (e) {
    console.error('饼图加载失败', e)
  } finally {
    pieLoading.value = false
  }
}

async function loadHotProducts() {
  try {
    hotProducts.value = await adminApi.dashboard.getHotProducts() as any[]
  } catch (e) {
    console.error('热门商品加载失败', e)
  } finally {
    hotLoading.value = false
  }
}

async function loadRecentOrders() {
  try {
    recentOrders.value = await adminApi.dashboard.getRecentOrders() as any[]
  } catch (e) {
    console.error('最近订单加载失败', e)
  } finally {
    recentLoading.value = false
  }
}
</script>

<template>
  <div class="admin-dashboard">
    <!-- 头部 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1>欢迎回来，管理员</h1>
        <p>以下是平台数据概览</p>
      </div>
      <div class="header-right">
        <span class="time">{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="loading">
      <div class="stat-card card-blue" @click="router.push('/admin/users')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        </div>
        <div class="stat-content">
          <p class="stat-label">用户总数</p>
          <p class="stat-value">{{ stats.totalUsers }}</p>
          <p class="stat-sub">本周新增 <span class="highlight">+{{ stats.newUsersThisWeek }}</span></p>
        </div>
      </div>

      <div class="stat-card card-green" @click="router.push('/admin/products')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
        </div>
        <div class="stat-content">
          <p class="stat-label">商品总数</p>
          <p class="stat-value">{{ stats.totalProducts }}</p>
          <p class="stat-sub">待审核 <span class="highlight-warn">{{ stats.pendingProducts }}</span></p>
        </div>
      </div>

      <div class="stat-card card-orange" @click="router.push('/admin/orders')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
        </div>
        <div class="stat-content">
          <p class="stat-label">订单总数</p>
          <p class="stat-value">{{ stats.totalOrders }}</p>
          <p class="stat-sub">待付款 <span class="highlight-warn">{{ stats.pendingOrders }}</span></p>
        </div>
      </div>

      <div class="stat-card card-purple" @click="router.push('/admin/orders')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        </div>
        <div class="stat-content">
          <p class="stat-label">总销售额</p>
          <p class="stat-value money">¥{{ formatMoney(stats.totalSales) }}</p>
          <p class="stat-sub">今日 <span class="highlight">+¥{{ formatMoney(stats.todaySales) }}</span></p>
        </div>
      </div>

      <div class="stat-card card-red">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
        <div class="stat-content">
          <p class="stat-label">待处理</p>
          <p class="stat-value">{{ stats.pendingProducts + stats.pendingOrders }}</p>
          <p class="stat-sub">审核{{ stats.pendingProducts }} · 待付款{{ stats.pendingOrders }}</p>
        </div>
      </div>

      <div class="stat-card card-gray">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
        <div class="stat-content">
          <p class="stat-label">禁用用户</p>
          <p class="stat-value">{{ stats.disabledUsers }}</p>
          <p class="stat-sub">占总数 {{ stats.totalUsers > 0 ? ((stats.disabledUsers / stats.totalUsers) * 100).toFixed(1) : 0 }}%</p>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <!-- 近7天订单趋势 -->
      <div class="chart-card chart-wide">
        <div class="chart-header">
          <span class="chart-title">近7天订单趋势</span>
        </div>
        <v-chart class="chart" :option="orderTrendOption" v-loading="orderTrendLoading" autoresize />
      </div>

      <!-- 近7天销售额趋势 -->
      <div class="chart-card chart-wide">
        <div class="chart-header">
          <span class="chart-title">近7天销售额趋势</span>
        </div>
        <v-chart class="chart" :option="salesTrendOption" v-loading="salesTrendLoading" autoresize />
      </div>
    </div>

    <!-- 中间图表行 -->
    <div class="charts-row charts-row-3">
      <!-- 订单状态分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">订单状态分布</span>
        </div>
        <v-chart class="chart" :option="orderStatusOption" v-loading="pieLoading" autoresize />
      </div>

      <!-- 商品分类分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">商品分类分布</span>
        </div>
        <v-chart class="chart" :option="categoryOption" v-loading="pieLoading" autoresize />
      </div>

      <!-- 商品状态分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">商品状态分布</span>
        </div>
        <v-chart class="chart" :option="productStatusOption" v-loading="pieLoading" autoresize />
      </div>
    </div>

    <!-- 底部数据行 -->
    <div class="charts-row charts-row-bottom">
      <!-- 近7天新增用户 -->
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">近7天新增用户</span>
        </div>
        <v-chart class="chart" :option="userTrendOption" v-loading="pieLoading" autoresize />
      </div>

      <!-- 热门商品 TOP 5 -->
      <div class="table-card">
        <div class="chart-header">
          <span class="chart-title">热门商品 TOP 5</span>
        </div>
        <el-table :data="hotProducts" style="width: 100%" v-loading="hotLoading" stripe :show-header="true">
          <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="price" label="价格" width="90" align="right">
            <template #default="{ row }">
              <span class="price-tag">¥{{ Number(row.price).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览量" width="90" align="center">
            <template #default="{ row }">
              <span class="view-count">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                {{ row.viewCount }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 近期待处理订单 -->
      <div class="table-card">
        <div class="chart-header">
          <span class="chart-title">近期待处理订单</span>
        </div>
        <el-table :data="recentOrders" style="width: 100%" v-loading="recentLoading" stripe>
          <el-table-column prop="orderNo" label="订单号" min-width="130" show-overflow-tooltip />
          <el-table-column prop="buyerName" label="买家" width="90" />
          <el-table-column prop="totalAmount" label="金额" width="90" align="right">
            <template #default="{ row }">
              <span class="price-tag">¥{{ Number(row.totalAmount).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <span class="status-tag" :class="'status-' + row.status.toLowerCase()">
                {{ orderStatusMap[row.status] || row.status }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="itemCount" label="商品数" width="70" align="center" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-dashboard {
  padding: var(--space-6);
  background: var(--color-background);
  min-height: 100vh;
}

/* 头部 */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}
.dashboard-header h1 {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 4px 0;
}
.dashboard-header p {
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  margin: 0;
}
.header-right .time {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  background: var(--color-surface);
  padding: 6px 14px;
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-xs);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.stat-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-xs);
  border: 1px solid var(--color-border-light);
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon svg {
  width: 24px;
  height: 24px;
}

.card-blue .stat-icon { background: #E8F2FF; color: #3688F4; }
.card-blue .stat-value { color: #3688F4; }

.card-green .stat-icon { background: #E8F8EE; color: #2ECC71; }
.card-green .stat-value { color: #2ECC71; }

.card-orange .stat-icon { background: #FFF3E0; color: #FF9F43; }
.card-orange .stat-value { color: #FF9F43; }

.card-purple .stat-icon { background: #F3EBFF; color: #8E44AD; }
.card-purple .stat-value { color: #8E44AD; }

.card-red .stat-icon { background: #FFEAEA; color: #E74C3C; }
.card-red .stat-value { color: #E74C3C; }

.card-gray .stat-icon { background: #F5F5F5; color: #95A5A6; }
.card-gray .stat-value { color: #95A5A6; }

.stat-content .stat-label {
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  margin: 0 0 4px 0;
  white-space: nowrap;
}
.stat-content .stat-value {
  font-size: 22px;
  font-weight: 800;
  margin: 0 0 4px 0;
  line-height: 1;
  font-family: var(--font-heading);
}
.stat-content .stat-value.money {
  font-size: 20px;
}
.stat-content .stat-sub {
  font-size: 11px;
  color: var(--color-text-muted);
  margin: 0;
}
.highlight { color: #2ECC71; font-weight: 600; }
.highlight-warn { color: #FF9F43; font-weight: 600; }

/* 图表行 */
.charts-row {
  display: grid;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
.charts-row.charts-row-3 {
  grid-template-columns: repeat(3, 1fr);
}
.charts-row-bottom {
  grid-template-columns: 1fr 1fr 1fr;
}

.chart-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  box-shadow: var(--shadow-xs);
  border: 1px solid var(--color-border-light);
}
.chart-wide {
  min-height: 280px;
}

.chart-header {
  margin-bottom: var(--space-4);
}
.chart-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  border-left: 3px solid var(--color-primary);
  padding-left: 10px;
}

.chart {
  width: 100%;
  height: 240px;
}

/* 表格卡片 */
.table-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  box-shadow: var(--shadow-xs);
  border: 1px solid var(--color-border-light);
}

.price-tag {
  color: var(--color-primary);
  font-weight: var(--font-semibold);
  font-size: var(--text-xs);
}
.view-count {
  display: flex;
  align-items: center;
  gap: 3px;
  color: var(--color-text-secondary);
  font-size: var(--text-xs);
}
.view-count svg {
  color: var(--color-text-muted);
}

/* 订单状态标签 */
.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}
.status-pending { background: #FFF3E0; color: #FF9F43; }
.status-paid { background: #E8F2FF; color: #3688F4; }
.status-shipped { background: #F3EBFF; color: #8E44AD; }
.status-completed { background: #E8F8EE; color: #2ECC71; }
.status-cancelled { background: #F5F5F5; color: #95A5A6; }

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 1100px) {
  .charts-row-3,
  .charts-row-bottom {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row-3,
  .charts-row-bottom {
    grid-template-columns: 1fr;
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
