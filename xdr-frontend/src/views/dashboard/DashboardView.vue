<template>
  <div class="dashboard-wrapper animate-slide-up">
    <!-- Bento Grid Dashboard -->
    <div class="bento-grid">
      
      <!-- 1. Real-time Threat Chart (Hero Card - Spans 2x2) -->
      <div class="bento-card hero-card">
        <div class="card-header">
          <div class="header-left">
            <h4 class="font-mono"><el-icon><TrendCharts /></el-icon> 威胁趋势洞察</h4>
            <span class="text-subtitle">实时监控系统威胁态势</span>
          </div>
          <div class="header-right">
            <el-radio-group v-model="trendDuration" size="small" class="custom-radio">
              <el-radio-button value="7d">7D</el-radio-button>
              <el-radio-button value="30d">30D</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div ref="trendChartRef" class="chart-viewport-hero"></div>
      </div>

      <!-- 2. Metric Cards (1x1 each) -->
      <div class="bento-card metric-card" v-for="(card, index) in statCards" :key="index">
        <div class="metric-header">
          <span class="metric-label">{{ card.label }}</span>
          <el-icon :color="card.color" :size="20"><component :is="card.icon" /></el-icon>
        </div>
        <div class="metric-body">
          <div class="metric-value font-mono">{{ card.value }}</div>
          <div class="metric-trend" :class="card.trend > 0 ? 'up' : 'down'">
            <span class="glowing-dot" v-if="card.label === '今日事件'"></span>
            {{ card.trend > 0 ? '+' : '' }}{{ card.trend }}%
          </div>
        </div>
      </div>

      <!-- 3. Risk Distribution (1x2 Vertical Card) -->
      <div class="bento-card distribution-card">
        <div class="card-header">
          <h4 class="font-mono"><el-icon><PieChart /></el-icon> 风险分级</h4>
        </div>
        <div ref="levelChartRef" class="chart-viewport-dist"></div>
      </div>

      <!-- 4. Critical Alerts (2x1 Horizontal Card) -->
      <div class="bento-card alerts-card">
        <div class="card-header">
          <h4 class="font-mono"><el-icon><Warning /></el-icon> 紧迫告警任务</h4>
          <el-button link type="primary" @click="$router.push('/threat/alerts')">管理</el-button>
        </div>
        <div class="alerts-list">
          <div v-for="alert in recentAlerts" :key="alert.id" class="alert-item">
            <div class="alert-info">
              <div class="alert-title text-truncate">{{ alert.title }}</div>
              <div class="alert-time text-muted">{{ alert.createdAt }}</div>
            </div>
            <el-tag :type="getLevelType(alert.level)" size="small" effect="dark" class="cyber-tag">
              {{ alert.level }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 5. Network Topology Mini (1x1 Card) -->
      <div class="bento-card topo-card">
        <div class="card-header">
          <h4 class="font-mono"><el-icon><Share /></el-icon> 拓扑</h4>
        </div>
        <div ref="topoMiniChartRef" class="chart-viewport-topo"></div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw, watch } from 'vue'
import * as echarts from 'echarts'
import { 
  Monitor, Warning, Platform, CircleCheck, 
  TrendCharts, PieChart, Share
} from '@element-plus/icons-vue'
import { getAssetStats } from '@/api/asset'
import { getAlertStats, getAlerts } from '@/api/alert'

const trendChartRef = ref<HTMLElement>()
const levelChartRef = ref<HTMLElement>()
const topoMiniChartRef = ref<HTMLElement>()
const recentAlerts = ref<any[]>([])
const trendDuration = ref('7d')

const statCards = ref([
  { label: '资产总量', value: 0, icon: markRaw(Platform), color: '#38bdf8', trend: 2.1 },
  { label: '防御在线', value: 0, icon: markRaw(CircleCheck), color: '#22c55e', trend: 0.5 },
  { label: '待决告警', value: 0, icon: markRaw(Warning), color: '#fb7185', trend: -12.4 },
  { label: '今日事件', value: 0, icon: markRaw(Monitor), color: '#f59e0b', trend: 8.7 },
])

onMounted(async () => {
  await fetchData()
  initTrendChart()
  initLevelChart()
  initMiniTopoChart()
})

async function fetchData() {
  try {
    const assetRes: any = await getAssetStats()
    statCards.value[0]!.value = assetRes.data?.total ?? 0
    statCards.value[1]!.value = assetRes.data?.online ?? 0
  } catch { /* empty */ }

  try {
    const alertRes: any = await getAlertStats()
    statCards.value[2]!.value = alertRes.data?.new ?? 0
    statCards.value[3]!.value = alertRes.data?.total ?? 0
  } catch { /* empty */ }

  try {
    const alertListRes: any = await getAlerts({ page: 1, size: 5 })
    recentAlerts.value = alertListRes.data?.records ?? []
  } catch { /* empty */ }
}

function getLevelType(level: string) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'HIGH') return 'warning'
  return 'info'
}

// Custom ECharts Options for Minimalist Look
const commonChartOptions = {
  backgroundColor: 'transparent',
  textStyle: { fontFamily: 'Fira Sans', color: '#94a3b8' }
}

function initTrendChart() {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    ...commonChartOptions,
    tooltip: { 
      trigger: 'axis', 
      backgroundColor: '#0f172a', 
      borderColor: '#1e293b', 
      textStyle: { color: '#f8fafc' },
      padding: [10, 15]
    },
    grid: { left: 0, right: 0, bottom: 20, top: 10, containLabel: false },
    xAxis: {
      type: 'category',
      data: ['02-24', '02-25', '02-26', '02-27', '02-28', '03-01', '03-02'],
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 10 }
    },
    yAxis: { show: false },
    series: [{
      name: '威胁事件',
      type: 'line',
      smooth: 0.4,
      showSymbol: false,
      data: [120, 132, 101, 134, 90, 230, 210],
      lineStyle: { width: 3, color: '#22c55e' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(34, 197, 94, 0.2)' },
          { offset: 1, color: 'rgba(34, 197, 94, 0)' }
        ])
      }
    }]
  })
}

function initLevelChart() {
  if (!levelChartRef.value) return
  const chart = echarts.init(levelChartRef.value)
  chart.setOption({
    ...commonChartOptions,
    series: [{
      type: 'pie',
      radius: ['60%', '85%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#0f172a', borderWidth: 2 },
      label: { show: false },
      data: [
        { value: 1048, name: 'CRITICAL', itemStyle: { color: '#ef4444' } },
        { value: 735, name: 'HIGH', itemStyle: { color: '#f59e0b' } },
        { value: 580, name: 'MEDIUM', itemStyle: { color: '#38bdf8' } },
        { value: 484, name: 'LOW', itemStyle: { color: '#64748b' } }
      ]
    }]
  })
}

function initMiniTopoChart() {
  if (!topoMiniChartRef.value) return
  const chart = echarts.init(topoMiniChartRef.value)
  chart.setOption({
    ...commonChartOptions,
    series: [{
      type: 'graph',
      layout: 'force',
      force: { repulsion: 60, gravity: 0.1 },
      data: [
        { id: '1', name: 'GW', symbolSize: 12, itemStyle: { color: '#f59e0b' } },
        { id: '2', name: 'A', symbolSize: 8, itemStyle: { color: '#38bdf8' } },
        { id: '3', name: 'B', symbolSize: 8, itemStyle: { color: '#38bdf8' } },
        { id: '4', name: 'C', symbolSize: 6, itemStyle: { color: '#64748b' } }
      ],
      links: [{ source: '1', target: '2' }, { source: '1', target: '3' }, { source: '3', target: '4' }],
      lineStyle: { color: '#334155', width: 1, curveness: 0.2 },
      emphasis: { focus: 'adjacency', lineStyle: { width: 2 } }
    }]
  })
}
</script>

<style scoped>
.dashboard-wrapper {
  padding: 12px;
}

.hero-card {
  grid-column: span 2;
  grid-row: span 2;
}

.distribution-card {
  grid-row: span 2;
}

.alerts-card {
  grid-column: span 2;
}

/* Card Header Styles */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left h4 {
  margin: 0;
  font-size: 0.95rem;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left .text-subtitle {
  font-size: 0.75rem;
  color: var(--text-muted);
}

/* Metric Card Styles */
.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.metric-label {
  font-size: 0.8rem;
  color: var(--text-secondary);
  font-weight: 500;
}

.metric-body {
  display: flex;
  flex-direction: column;
}

.metric-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #fff;
}

.metric-trend {
  font-size: 0.7rem;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.metric-trend.up { color: var(--primary-color); }
.metric-trend.down { color: #fb7185; }

/* Alert List Styles */
.alerts-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
}

.alert-item:last-child { border: none; }

.alert-title {
  font-size: 0.85rem;
  color: var(--text-primary);
}

.alert-time {
  font-size: 0.7rem;
}

/* Chart Viewports */
.chart-viewport-hero { height: 280px; }
.chart-viewport-dist { height: 240px; }
.chart-viewport-topo { height: 120px; }

/* Custom Radio Styles */
:deep(.custom-radio .el-radio-button__inner) {
  background: rgba(30, 41, 59, 0.5) !important;
  color: var(--text-muted) !important;
  border-color: var(--card-border) !important;
  font-size: 10px;
  padding: 5px 10px;
}

:deep(.custom-radio .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--card-border) !important;
  color: #fff !important;
}

.text-truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 250px;
}

.cyber-tag {
  font-family: var(--font-heading);
  font-size: 10px !important;
  font-weight: 600;
}
</style>
