<template>
  <view class="m-approval-container">
    <view class="header-box">
      <text class="title">空管移动端飞行审批</text>
      <text class="subtitle">待审批申报单 ({{ pendingList.length }})</text>
    </view>

    <!-- 申报单列表 -->
    <view class="list-container">
      <view v-if="pendingList.length === 0" class="empty-card">
        <text class="empty-text">当前无待审批飞行申报单</text>
        <button class="refresh-btn" @click="fetchApplies">点击刷新</button>
      </view>

      <view v-for="apply in pendingList" :key="apply.applyId" class="apply-card">
        <view class="card-top">
          <text class="sn">{{ apply.applySn }}</text>
          <text class="status-tag">待空管审批</text>
        </view>

        <view class="info-row">
          <text class="label">申报商户：</text>
          <text class="val">{{ apply.merchantName }}</text>
        </view>
        <view class="info-row">
          <text class="label">执飞机号：</text>
          <text class="val">{{ apply.uavSn }} ({{ apply.uavModel }})</text>
        </view>
        <view class="info-row">
          <text class="label">执飞机长：</text>
          <text class="val">{{ apply.pilotName }}</text>
        </view>
        <view class="info-row">
          <text class="label">计划巡航：</text>
          <text class="val text-blue">{{ apply.altPlanned }} 米</text>
        </view>
        <view class="info-row">
          <text class="label">起降时段：</text>
          <text class="val text-time">{{ formatTime(apply.startAt) }} 至 {{ formatTime(apply.endAt) }}</text>
        </view>

        <view class="action-row">
          <button class="btn btn-reject" @click="handleReject(apply)">驳回</button>
          <button class="btn btn-pass" @click="handlePass(apply)">核准通过</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'

const pendingList = ref<any[]>([])

const formatTime = (timeStr: string) => {
  if (!timeStr) return '--'
  return timeStr.replace('T', ' ').substring(5, 16)
}

const fetchApplies = async () => {
  try {
    const res: any = await request.get('/api/v1/flight-apply/list', { approveState: 0 })
    pendingList.value = res || []
  } catch (error) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const handlePass = (apply: any) => {
  uni.showModal({
    title: '审批通过确认',
    content: `核准批准飞行申报【${apply.applySn}】？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await request.post('/api/v1/flight-apply/approve', {
            applyId: apply.applyId,
            approveState: 1,
            approveNote: '移动端空管快速核准通过'
          })
          uni.showToast({ title: '已核准通过！', icon: 'success' })
          fetchApplies()
        } catch (err: any) {
          uni.showToast({ title: err?.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

const handleReject = (apply: any) => {
  uni.showModal({
    title: '审批驳回确认',
    content: `确认驳回申报【${apply.applySn}】？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await request.post('/api/v1/flight-apply/approve', {
            applyId: apply.applyId,
            approveState: 2,
            approveNote: '空域繁忙临时管控，予以驳回'
          })
          uni.showToast({ title: '已驳回申报', icon: 'none' })
          fetchApplies()
        } catch (err: any) {
          uni.showToast({ title: err?.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

onLoad(() => {
  fetchApplies()
})
</script>

<style scoped>
.m-approval-container {
  min-height: 100vh;
  background-color: #f1f5f9;
  padding: 16px;
}

.header-box {
  margin-bottom: 16px;
}

.header-box .title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  display: block;
}

.header-box .subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
  display: block;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-card {
  background: #fff;
  border-radius: 8px;
  padding: 30px 16px;
  text-align: center;
}

.empty-text {
  font-size: 14px;
  color: #94a3b8;
  display: block;
  margin-bottom: 12px;
}

.refresh-btn {
  font-size: 13px;
  background-color: #0284c7;
  color: #fff;
  width: 120px;
}

.apply-card {
  background-color: #ffffff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 10px;
  margin-bottom: 10px;
}

.sn {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.status-tag {
  font-size: 12px;
  color: #f59e0b;
  background-color: #fef3c7;
  padding: 2px 8px;
  border-radius: 4px;
}

.info-row {
  display: flex;
  font-size: 13px;
  line-height: 1.8;
}

.info-row .label {
  color: #64748b;
  width: 75px;
}

.info-row .val {
  color: #1e293b;
  flex: 1;
}

.text-blue {
  color: #0284c7;
  font-weight: 600;
}

.text-time {
  font-size: 12px;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

.btn {
  flex: 1;
  font-size: 14px;
  height: 38px;
  line-height: 38px;
  border-radius: 6px;
}

.btn-reject {
  background-color: #fee2e2;
  color: #ef4444;
}

.btn-pass {
  background-color: #10b981;
  color: #ffffff;
}
</style>
