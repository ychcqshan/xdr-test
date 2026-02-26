<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card">
          <div class="icon-box" :style="{ background: card.bg }">
            <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
          </div>
          <div>
            <div class="value">{{ card.value }}</div>
            <div class="label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="16">
        <div class="chart-card">
          <h4>告警趋势（近7天）</h4>
          <div ref="trendChartRef" style="height: 320px;"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-card">
          <h4>告警级别分布</h4>
          <div ref="levelChartRef" style="height: 320px;"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 最新告警 -->
    <div class="table-card">
      <div class="card-header">
        <h3>最新告警</h3>
        <el-button text type="primary" @click="$router.push('/alerts')">查看全部</el-button>
      </div>
      <el-table :data="recentAlerts" stripe>
        <el-table-column prop="title" label="告警标题" min-width="200" />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <span :class="'level-' + row.level?.toLowerCase()">{{ row.level }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="agentId" label="Agent" width="180" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import * as echarts from 'echarts'
import { Monitor, Warning, Platform, CircleCheck } from '@element-plus/icons-vue'
import { getAssetStats } from '@/api/asset'
import { getAlertStats, getAlerts } from '@/api/alert'

const trendChartRef = ref<HTMLElement>()
const levelChartRef = ref<HTMLElement>()
const recentAlerts = ref<any[]>([])

const statCards = ref([
  { label: '资产总数', value: 0, icon: markRaw(Platform), bg: '#ecf5ff', color: '#409eff' },
  { label: '在线资产', value: 0, icon: markRaw(CircleCheck), bg: '#f0f9eb', color: '#67c23a' },
  { label: '待处理告警', value: 0, icon: markRaw(Warning), bg: '#fef0f0', color: '#f56c6c' },
  { label: '今日事件', value: 0, icon: markRaw(Monitor), bg: '#fdf6ec', color: '#e6a23c' },
])

onMounted(async () => {
  // 加载统计数据（API不可用时用模拟数据）
  try {
    const assetRes: any = await getAssetStats()
    statCards.value[0]!.value = assetRes.data?.total ?? 0
    statCards.value[1]!.value = assetRes.data?.online ?? 0
  } catch { /* 使用默认值 */ }

  try {
    const alertRes: any = await getAlertStats()
    statCards.value[2]!.value = alertRes.data?.new ?? 0
    statCards.value[3]!.value = alertRes.data?.total ?? 0
  } catch { /* 使用默认值 */ }

  try {
    const alertListRes: any = await getAlerts({ page: 1, size: 5 })
    recentAlerts.value = alertListRes.data?.records ?? []
  } catch { /* 空列表 */ }

  initTrendChart()
  initLevelChart()
})

function initTrendChart() {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, bottom: 30, top: 20 },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    },
    yAxis: { type: 'value' },
    series: [{
      name: '告警数',
      type: 'line',
      smooth: true,
      data: [12, 8, 15, 23, 18, 9, 6],
      areaStyle: { color: 'rgba(64,158,255,0.15)' },
      lineStyle: { color: '#409eff', width: 2 },
      itemStyle: { color: '#409eff' },
    }],
  })
  window.addEventListener('resize', () => chart.resize())
}

function initLevelChart() {
  if (!levelChartRef.value) return
  const chart = echarts.init(levelChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      data: [
        { value: 5, name: '严重', itemStyle: { color: '#f56c6c' } },
        { value: 12, name: '高危', itemStyle: { color: '#e6a23c' } },
        { value: 28, name: '中危', itemStyle: { color: '#409eff' } },
        { value: 45, name: '低危', itemStyle: { color: '#c0c4cc' } },
      ],
    }],
  })
  window.addEventListener('resize', () => chart.resize())
}
</script>
