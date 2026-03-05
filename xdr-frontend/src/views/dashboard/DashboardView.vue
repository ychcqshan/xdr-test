<template>
  <div class="page-main">
    <!-- Hero Stats Row (Elite Floating Cards with Mesh Background) -->
    <div class="hero-header-mesh">
      <div class="mesh-overlay"></div>
      <div class="hero-stats-row">
        <div 
          v-for="(card, index) in statCards" 
          :key="index"
          class="bento-card hero-card entrance-stagger"
          :style="{ '--delay': index * 0.1 + 's' }"
        >
          <div class="hero-card-inner">
            <div class="stat-icon-wrapper" :style="{ backgroundColor: card.bgColor }">
              <el-icon size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-label">{{ card.label }}</span>
              <div class="stat-value-group">
                <span class="stat-value">{{ card.value.toLocaleString() }}</span>
                <div v-if="card.trend" class="stat-trend" :class="card.trend > 0 ? 'up' : 'down'">
                  <el-icon><CaretTop v-if="card.trend > 0" /><CaretBottom v-else /></el-icon>
                  {{ Math.abs(card.trend) }}%
                </div>
              </div>
            </div>
          </div>
          <div class="card-bg-glow" :style="{ background: `radial-gradient(circle at 70% 30%, ${card.color}15, transparent 70%)` }"></div>
        </div>
      </div>
    </div>

    <!-- Main Bento Grid -->
    <div class="bento-grid elite-grid">
      <!-- 1. Real-time Threat Analysis (Large Card) -->
      <div class="bento-card span-3 row-2 threat-map-card entrance-stagger" style="--delay: 0.4s">
        <div class="card-header-elite">
          <div class="title-with-dot">
            <span class="pulse-dot"></span>
            <h3>全球威胁情报态势</h3>
          </div>
          <div class="header-actions">
            <el-radio-group v-model="trendDuration" size="small" class="elite-radio">
              <el-radio-button value="7d">7D</el-radio-button>
              <el-radio-button value="30d">30D</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div ref="trendChartRef" class="chart-viewport"></div>
      </div>

      <!-- 2. Risk Level Distribution -->
      <div class="bento-card span-1 row-1 risk-distribution-card entrance-stagger" style="--delay: 0.5s">
        <div class="card-header-elite">
          <h3>资产风险等级分布</h3>
        </div>
        <div ref="levelChartRef" class="chart-viewport-small"></div>
      </div>

      <!-- 3. Compliance Health -->
      <div class="bento-card span-1 row-1 compliance-card entrance-stagger" style="--delay: 0.6s">
        <div class="card-header-elite">
          <h3>合规健康度中心</h3>
        </div>
        <div class="circular-progress-group">
          <el-progress 
            type="dashboard" 
            :percentage="94" 
            :color="'#4F46E5'"
            :width="120"
            :stroke-width="10"
          >
            <template #default="{ percentage }">
              <div class="progress-label">
                <span class="percentage">{{ percentage }}%</span>
                <span class="label">健康运行</span>
              </div>
            </template>
          </el-progress>
        </div>
      </div>

      <!-- 4. Recent Alerts Table -->
      <div class="bento-card span-4 row-1 alerts-table-card entrance-stagger" style="--delay: 0.7s">
        <div class="card-header-elite border-header">
          <h3>优先处置任务流</h3>
          <el-button link type="primary" class="view-all-btn" @click="$router.push('/threat/alerts')">
            管理情报详情 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <el-table :data="recentAlerts" style="width: 100%" class="elite-table no-padding">
          <el-table-column prop="title" label="威胁描述" min-width="300" />
          <el-table-column prop="level" label="严重程度" width="140">
            <template #default="{ row }">
              <div class="status-pill" :class="row.level.toLowerCase()">
                <span class="status-dot" :class="row.level.toLowerCase()"></span>
                {{ translateLevel(row.level) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="检测时间" width="200" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { 
  Monitor, Warning, Platform, CircleCheck, 
  CaretTop, CaretBottom, ArrowRight
} from '@element-plus/icons-vue'
import { getAssetStats } from '@/api/asset'
import { getAlertStats, getAlerts } from '@/api/alert'

const trendChartRef = ref<HTMLElement>()
const levelChartRef = ref<HTMLElement>()
const recentAlerts = ref<any[]>([])
const trendDuration = ref('7d')

let charts: echarts.ECharts[] = []

const statCards = ref([
  { label: '资产总数', value: 0, icon: markRaw(Platform), color: '#3B82F6', bgColor: '#EEF2FF', trend: 2.1 },
  { label: '在线防御', value: 0, icon: markRaw(CircleCheck), color: '#10B981', bgColor: '#ECFDF5', trend: 0.5 },
  { label: '待处理威胁', value: 0, icon: markRaw(Warning), color: '#EF4444', bgColor: '#FEF2F2', trend: -12.4 },
  { label: '情报事件流', value: 0, icon: markRaw(Monitor), color: '#F59E0B', bgColor: '#FFFBEB', trend: 8.7 },
])

function translateLevel(level: string) {
  const map: any = { 'CRITICAL': '紧急', 'HIGH': '高危', 'MEDIUM': '中危', 'LOW': '低危' }
  return map[level] || level
}

onMounted(async () => {
  await fetchData()
  initTrendChart()
  initLevelChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})

function handleResize() {
  charts.forEach(c => c.resize())
}

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

function initTrendChart() {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  charts.push(chart)
  
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderWidth: 0,
      boxShadow: '0 10px 30px rgba(0,0,0,0.1)',
      textStyle: { color: '#0F172A', fontWeight: 600 }
    },
    grid: { left: '20', right: '20', bottom: '0', top: '40', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#64748B', fontSize: 12, fontWeight: 500 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
      axisLabel: { color: '#64748B' }
    },
    series: [{
      name: 'Detection Score',
      type: 'line',
      smooth: true,
      symbol: 'none',
      itemStyle: { color: '#4F46E5' },
      lineStyle: { width: 4 },
      data: [45, 52, 38, 65, 48, 72, 58],
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(79, 70, 229, 0.2)' },
          { offset: 1, color: 'rgba(79, 70, 229, 0)' }
        ])
      }
    }]
  })
}

function initLevelChart() {
  if (!levelChartRef.value) return
  const chart = echarts.init(levelChartRef.value)
  charts.push(chart)
  
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '0', icon: 'circle', textStyle: { color: '#64748B', fontWeight: 500 } },
    series: [{
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['50%', '40%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 4 },
      label: { show: false },
      data: [
        { value: 40, name: '紧急 (Critical)', itemStyle: { color: '#EF4444' } },
        { value: 30, name: '高危 (High)', itemStyle: { color: '#F59E0B' } },
        { value: 20, name: '中危 (Medium)', itemStyle: { color: '#3B82F6' } },
        { value: 10, name: '低危 (Low)', itemStyle: { color: '#64748B' } }
      ]
    }]
  })
}
</script>

<style scoped>
.hero-header-mesh {
  position: relative;
  background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
  border-radius: 40px;
  padding: 40px;
  margin-bottom: var(--space-xl);
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.4);
}

.mesh-overlay {
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(at 0% 0%, rgba(79, 70, 229, 0.08) 0, transparent 50%), 
    radial-gradient(at 50% 0%, rgba(59, 130, 246, 0.06) 0, transparent 50%), 
    radial-gradient(at 100% 0%, rgba(16, 185, 129, 0.05) 0, transparent 50%);
  filter: blur(40px);
  opacity: 0.6;
}

.hero-stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-lg);
  position: relative;
  z-index: 1;
}

.hero-card {
  padding: 32px;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
}

.entrance-stagger {
  opacity: 0;
  transform: translateY(20px);
  animation: entranceSlide 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: var(--delay, 0s);
}

@keyframes entranceSlide {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-card-inner {
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  z-index: 1;
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 16px -4px rgba(0, 0, 0, 0.05);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value-group {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
}

.stat-trend {
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 2px;
}
.stat-trend.up { color: #10B981; }
.stat-trend.down { color: #EF4444; }

.card-bg-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.elite-grid {
  grid-auto-rows: minmax(180px, auto);
}

.span-4 { grid-column: span 4; }
.span-3 { grid-column: span 3; }
.span-1 { grid-column: span 1; }
.row-2 { grid-row: span 2; }
.row-1 { grid-row: span 1; }

.card-header-elite {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.border-header {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--card-border);
  margin-bottom: 0;
  padding: var(--space-lg) var(--space-xl);
}

.title-with-dot {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: var(--primary-color);
  border-radius: 50%;
  box-shadow: 0 0 10px var(--primary-glow);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(79, 70, 229, 0.4); }
  70% { transform: scale(1); box-shadow: 0 0 0 10px rgba(79, 70, 229, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(79, 70, 229, 0); }
}

.chart-viewport {
  height: 420px;
  width: 100%;
}

.chart-viewport-small {
  height: 280px;
  width: 100%;
}

.view-all-btn {
  font-weight: 700;
  font-size: 14px;
}

.no-padding {
  padding: 0 var(--space-xl);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}

.status-pill.critical, .status-pill.high { background: #FEF2F2; color: #991B1B; }
.status-pill.medium { background: #FFFBEB; color: #92400E; }
.status-pill.low { background: #F1F5F9; color: #64748B; }

.status-dot.critical, .status-dot.high { background: #EF4444; }
.status-dot.medium { background: #F59E0B; }
.status-dot.low { background: #94A3B8; }

.circular-progress-group {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 0;
}

.progress-label {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.percentage {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
}

.label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-light);
}

.elite-radio :deep(.el-radio-button__inner) {
  background: #F1F5F9 !important;
  border: none !important;
  color: #64748B !important;
  padding: 8px 16px !important;
  font-weight: 700 !important;
  border-radius: 10px !important;
  margin-left: 4px;
}

.elite-radio :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: white !important;
  color: var(--primary-color) !important;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08) !important;
}
</style>
