<template>
  <view class="m-work-container">
    <!-- 顶部状态卡片 -->
    <view class="hero-card">
      <view class="hero-title">
        <text class="main-text">机长与外场作业中枢</text>
        <text class="sub-text">实时任务驱动 · 状态秒级回传</text>
      </view>
      <view class="action-grid">
        <view class="action-item" @click="navTo('/pages/maintain/report')">
          <view class="icon-wrap bg-danger">⚠️</view>
          <text class="act-title">现场故障上报</text>
          <text class="act-sub">提报电机/固件故障</text>
        </view>
        <view class="action-item" @click="navTo('/pages/maintain/inspect')">
          <view class="icon-wrap bg-success">📋</view>
          <text class="act-title">设备巡检打卡</text>
          <text class="act-sub">航前/航后日常维保</text>
        </view>
      </view>
    </view>

    <!-- 今日执飞任务 / 在册无人机速览 -->
    <view class="section-card">
      <view class="section-header">
        <text class="sec-title">常驻场站机队状态 (五态监控)</text>
      </view>
      <view v-for="uav in uavList" :key="uav.uavId" class="uav-item">
        <view class="uav-top">
          <text class="uav-sn">{{ uav.uavSn }}</text>
          <text class="uav-state-tag" :class="'state-color-' + uav.state">{{ uav.stateDesc }}</text>
        </view>
        <view class="uav-model">{{ uav.model }}</view>
        <view class="uav-props">
          <text class="prop">电量: {{ uav.batteryPct }}%</text>
          <text class="prop">健康度: {{ uav.healthScore }}分</text>
          <text class="prop">载重: {{ uav.maxLoad }}kg</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'

const uavList = ref<any[]>([])

const fetchUavs = async () => {
  try {
    const res: any = await request.get('/api/v1/uav/list')
    uavList.value = res || []
  } catch (err) {}
}

const navTo = (url: string) => {
  uni.navigateTo({ url })
}

onShow(() => {
  fetchUavs()
})
</script>

<style scoped>
.m-work-container {
  padding: 16px;
  background-color: #f8fafc;
  min-height: 100vh;
}
.hero-card {
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
  border-radius: 12px;
  padding: 20px;
  color: #fff;
  margin-bottom: 16px;
}
.hero-title {
  margin-bottom: 16px;
}
.main-text {
  font-size: 18px;
  font-weight: bold;
  display: block;
}
.sub-text {
  font-size: 12px;
  color: #bae6fd;
  margin-top: 4px;
  display: block;
}
.action-grid {
  display: flex;
  flex-direction: row;
  gap: 12px;
}
.action-item {
  flex: 1;
  background-color: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-bottom: 6px;
}
.bg-danger { background-color: #ef4444; }
.bg-success { background-color: #10b981; }
.act-title {
  font-size: 13px;
  font-weight: bold;
}
.act-sub {
  font-size: 10px;
  color: #e0f2fe;
  margin-top: 2px;
}
.section-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
}
.sec-title {
  font-size: 15px;
  font-weight: bold;
  color: #0f172a;
}
.uav-item {
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 0;
}
.uav-item:last-child {
  border-bottom: none;
}
.uav-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.uav-sn {
  font-family: monospace;
  font-weight: bold;
  font-size: 14px;
  color: #0284c7;
}
.uav-state-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  color: white;
}
.state-color-0 { background-color: #10b981; }
.state-color-1 { background-color: #3b82f6; }
.state-color-2 { background-color: #f59e0b; }
.state-color-3 { background-color: #ef4444; }
.state-color-4 { background-color: #94a3b8; }
.uav-model {
  font-size: 13px;
  color: #334155;
  margin: 4px 0;
}
.uav-props {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #64748b;
}
</style>
