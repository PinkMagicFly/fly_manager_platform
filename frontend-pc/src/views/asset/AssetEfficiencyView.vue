<template>
  <div class="efficiency-container">
    <!-- 顶部核心指标 KPI 卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-icon-wrap bg-blue"><el-icon><Promotion /></el-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">机队在册总规模</span>
            <span class="kpi-value">{{ overviewData.totalUavs || 0 }} <span class="unit">架</span></span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-icon-wrap bg-green"><el-icon><VideoPlay /></el-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">当前执飞在途架数</span>
            <span class="kpi-value text-green">{{ overviewData.onTaskUavs || 0 }} <span class="unit">架</span></span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-icon-wrap bg-orange"><el-icon><Odometer /></el-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">综合设备稼动率</span>
            <span class="kpi-value text-orange">{{ overviewData.avgUtilizationRate || 0 }} <span class="unit">%</span></span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-icon-wrap bg-purple"><el-icon><Timer /></el-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">机队累计安全飞行</span>
            <span class="kpi-value text-purple">{{ overviewData.totalFlyHours || 0 }} <span class="unit">小时</span></span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 4 大 ECharts 图表栅格 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 图表 1：设备五态分布饼图 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>机队运行五态数量分布</span>
              <el-tag size="small" type="info">实时状态</el-tag>
            </div>
          </template>
          <div id="chart-state-pie" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 图表 2：7日设备稼动率趋势折线 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>近 7 日机队综合稼动率波动趋势</span>
              <el-tag size="small" type="success">日平均 %</el-tag>
            </div>
          </template>
          <div id="chart-util-trend" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 图表 3：飞行总时长 Top 5 排行 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>机身累计执飞时长 Top 5 排行</span>
              <el-tag size="small" type="primary">飞行工时</el-tag>
            </div>
          </template>
          <div id="chart-top-hours" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 图表 4：健康度评分分布柱状图 -->
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>机队设备综合健康评分梯队分布</span>
              <el-tag size="small" type="warning">维保预警</el-tag>
            </div>
          </template>
          <div id="chart-health-bar" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getEfficiencyOverview } from '@/api/asset'

const overviewData = ref<any>({})

let pieChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null
let healthChart: echarts.ECharts | null = null

const initCharts = () => {
  const pieDom = document.getElementById('chart-state-pie')
  if (pieDom) pieChart = echarts.init(pieDom)

  const trendDom = document.getElementById('chart-util-trend')
  if (trendDom) trendChart = echarts.init(trendDom)

  const barDom = document.getElementById('chart-top-hours')
  if (barDom) barChart = echarts.init(barDom)

  const healthDom = document.getElementById('chart-health-bar')
  if (healthDom) healthChart = echarts.init(healthDom)
}

const renderCharts = (data: any) => {
  if (pieChart) {
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 架 ({d}%)' },
      legend: { bottom: '0%', left: 'center' },
      color: ['#10b981', '#3b82f6', '#f59e0b', '#ef4444', '#94a3b8'],
      series: [
        {
          name: '运行状态',
          type: 'pie',
          radius: ['45%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          data: data.stateDistribution || []
        }
      ]
    })
  }

  if (trendChart) {
    trendChart.setOption({
      tooltip: { trigger: 'axis', formatter: '{b}<br/>稼动率: {c}%' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: data.utilizationTrend?.dates || [] },
      yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
      series: [
        {
          name: '稼动率',
          type: 'line',
          smooth: true,
          data: data.utilizationTrend?.rates || [],
          itemStyle: { color: '#0284c7' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(2, 132, 199, 0.4)' },
              { offset: 1, color: 'rgba(2, 132, 199, 0.02)' }
            ])
          }
        }
      ]
    })
  }

  if (barChart) {
    const list = data.topFlyHours || []
    barChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '6%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value', name: '小时' },
      yAxis: { type: 'category', data: list.map((i: any) => i.name), axisLabel: { interval: 0, width: 130, overflow: 'truncate' } },
      series: [
        {
          type: 'bar',
          data: list.map((i: any) => i.value),
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#3b82f6' },
              { offset: 1, color: '#06b6d4' }
            ])
          }
        }
      ]
    })
  }

  if (healthChart) {
    const hList = data.healthDistribution || []
    healthChart.setOption({
      tooltip: { trigger: 'item' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: hList.map((i: any) => i.name) },
      yAxis: { type: 'value', name: '架数' },
      series: [
        {
          type: 'bar',
          barWidth: '40%',
          data: hList.map((i: any, idx: number) => ({
            value: i.value,
            itemStyle: {
              color: idx === 0 ? '#10b981' : idx === 1 ? '#f59e0b' : '#ef4444',
              borderRadius: [4, 4, 0, 0]
            }
          }))
        }
      ]
    })
  }
}

const handleResize = () => {
  pieChart?.resize()
  trendChart?.resize()
  barChart?.resize()
  healthChart?.resize()
}

const fetchData = async () => {
  try {
    const res = await getEfficiencyOverview()
    overviewData.value = res || {}
    renderCharts(overviewData.value)
  } catch (err) {}
}

onMounted(() => {
  initCharts()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  trendChart?.dispose()
  barChart?.dispose()
  healthChart?.dispose()
})
</script>

<style scoped>
.efficiency-container {
  display: flex;
  flex-direction: column;
}
.kpi-card {
  border-radius: 8px;
}
:deep(.kpi-card .el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
}
.kpi-icon-wrap {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}
.bg-blue { background: linear-gradient(135deg, #0284c7, #38bdf8); }
.bg-green { background: linear-gradient(135deg, #059669, #34d399); }
.bg-orange { background: linear-gradient(135deg, #d97706, #fbbf24); }
.bg-purple { background: linear-gradient(135deg, #7c3aed, #a78bfa); }

.kpi-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kpi-label {
  font-size: 13px;
  color: #64748b;
}
.kpi-value {
  font-size: 22px;
  font-weight: bold;
  color: #0f172a;
}
.unit {
  font-size: 12px;
  font-weight: normal;
  color: #94a3b8;
}
.text-green { color: #059669; }
.text-orange { color: #d97706; }
.text-purple { color: #7c3aed; }

.chart-card {
  border-radius: 8px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #0f172a;
  font-size: 14px;
}
.chart-box {
  width: 100%;
  height: 270px;
}
</style>


