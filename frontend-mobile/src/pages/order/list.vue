<template>
  <view class="m-order-list-container">
    <view class="nav-bar">
      <text class="back-btn" @click="goBack">‹ 工作台</text>
      <text class="page-title">低空物流运单池</text>
      <view class="nav-right">
        <text class="ticket-link" @click="goToTicket">🎧 客服工单</text>
        <text class="add-btn" @click="goToCreate">+ 寄件</text>
      </view>
    </view>

    <!-- 状态滑动过滤Tab -->
    <scroll-view scroll-x class="tab-scroll" :show-scrollbar="false">
      <view class="tab-list">
        <view
          v-for="tab in tabList"
          :key="tab.val"
          class="tab-item"
          :class="{ active: currentTab === tab.val }"
          @click="switchTab(tab.val)"
        >
          {{ tab.label }}
        </view>
      </view>
    </scroll-view>

    <!-- 订单列表 -->
    <scroll-view scroll-y class="order-scroll" v-loading="loading">
      <view v-if="orderList.length === 0" class="empty-wrap">
        <text class="empty-txt">暂无相关运单记录</text>
        <button class="empty-create-btn" size="mini" @click="goToCreate">➕ 立即在线寄件下单</button>
      </view>
      <view v-for="item in orderList" :key="item.orderId" class="order-card">
        <view class="card-top">
          <text class="sn">{{ item.orderSn }}</text>
          <view class="badges">
            <text v-if="item.urgentFlag === 1" class="badge bg-red">加急</text>
            <text v-if="isPilot && (item.pilotName === '张志强' || item.pilotId === 1)" class="badge bg-emerald">本人执飞</text>
            <text class="badge" :class="'state-' + item.orderState">{{ item.orderStateDesc }}</text>
          </view>
        </view>

        <view class="addr-box">
          <view class="addr-line"><text class="dot bg-green">起</text> <text class="txt">{{ item.senderAddr }}</text></view>
          <view class="addr-line" style="margin-top: 6px"><text class="dot bg-orange">止</text> <text class="txt">{{ item.receiverAddr }}</text></view>
        </view>

        <!-- 执飞无人机与机长全息透出 -->
        <view class="uav-pilot-row" v-if="item.uavSn || item.pilotName || item.applySn">
          <text v-if="item.uavSn" class="tag-uav">🛸 {{ item.uavSn }}</text>
          <text v-if="item.pilotName" class="tag-pilot">👨‍✈️ 机长: {{ item.pilotName }}</text>
          <text v-if="item.applySn" class="tag-apply">📋 {{ item.applySn }}</text>
        </view>

        <view class="meta-row">
          <text class="meta-item">{{ item.goodsTypeDesc }} · {{ item.goodsWeight }}kg</text>
          <text class="price">¥{{ Number(item.feeTotal).toFixed(2) }}</text>
        </view>

        <view class="card-footer">
          <text class="time">{{ item.createdAt }}</text>
          <view class="btn-group">
            <button class="action-btn btn-ghost" size="mini" @click="goToDetail(item.orderId)">全景跟踪</button>
            <button v-if="item.orderState === 40" class="action-btn btn-primary" size="mini" @click="handleTakeoff(item.orderId)">确认起飞</button>
            <button v-if="item.orderState === 50" class="action-btn btn-success" size="mini" @click="handleDeliver(item.orderId)">确认送达</button>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'

const loading = ref(false)
const orderList = ref<any[]>([])
const currentTab = ref('ALL')
const user = ref<any>(null)
const isPilot = computed(() => user.value?.roles?.includes('ROLE_PILOT'))

const initUser = () => {
  const cached = uni.getStorageSync('soarcloud_mobile_user')
  if (cached) {
    try { user.value = JSON.parse(cached) } catch (e) {}
  }
}

const tabList = [
  { val: 'ALL', label: '全部' },
  { val: '20', label: '待接单' },
  { val: '30', label: '待调度' },
  { val: '40', label: '待执飞' },
  { val: '50', label: '飞行中' },
  { val: '60', label: '已送达' }
]

const loadOrders = async () => {
  loading.value = true
  try {
    const params: any = { size: 30 }
    if (currentTab.value !== 'ALL') {
      params.orderState = Number(currentTab.value)
    }
    const res: any = await request.get('/api/v1/order/page', params)
    orderList.value = res?.list || []
  } catch (err: any) {
    uni.showToast({ title: '加载运单失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const switchTab = (val: string) => {
  currentTab.value = val
  loadOrders()
}

const handleTakeoff = async (orderId: number) => {
  uni.showModal({
    title: '起飞确认',
    content: '确认航前巡检无误并起飞执飞？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request.post(`/api/v1/order/${orderId}/takeoff`, {})
          uni.showToast({ title: '已起飞，进入在途监控', icon: 'success' })
          loadOrders()
        } catch (e: any) {
          uni.showToast({ title: e.message || '起飞失败', icon: 'none' })
        }
      }
    }
  })
}

const handleDeliver = async (orderId: number) => {
  uni.showModal({
    title: '送达确认',
    content: '确认已安全降落起降点并签收？系统将自动出具对账单。',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request.post(`/api/v1/order/${orderId}/deliver`, {})
          uni.showToast({ title: '已送达签收，自动出具账单', icon: 'success' })
          loadOrders()
        } catch (e: any) {
          uni.showToast({ title: e.message || '签收失败', icon: 'none' })
        }
      }
    }
  })
}

const goToCreate = () => {
  uni.navigateTo({
    url: '/pages/order/create',
    fail: () => {
      uni.reLaunch({ url: '/pages/order/create' })
    }
  })
}
const goToDetail = (orderId: number) => { uni.navigateTo({ url: `/pages/order/detail?id=${orderId}` }) }
const goBack = () => { uni.reLaunch({ url: '/pages/work/index' }) }
const goToTicket = () => { uni.navigateTo({ url: '/pages/ticket/mine' }) }


onShow(() => {
  initUser()
  loadOrders()
})
onMounted(() => {
  initUser()
  loadOrders()
})
</script>

<style scoped>
.uav-pilot-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  background: #f1f5f9;
  padding: 6px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
  font-size: 11px;
}
.tag-uav { color: #0369a1; font-weight: 500; }
.tag-pilot { color: #047857; font-weight: 500; }
.tag-apply { color: #64748b; }
.bg-emerald { background-color: #10b981; color: #fff; font-weight: bold; }
.m-order-list-container { display: flex; flex-direction: column; height: 100vh; background: #f8fafc; }
.nav-bar { display: flex; justify-content: space-between; align-items: center; padding: 36px 14px 10px 14px; background: #0284c7; color: #fff; }
.back-btn { font-size: 15px; font-weight: 500; }
.page-title { font-size: 16px; font-weight: bold; }
.add-btn { font-size: 14px; font-weight: bold; background: rgba(255, 255, 255, 0.2); padding: 4px 10px; border-radius: 12px; }
.nav-right { display: flex; align-items: center; gap: 8px; }
.ticket-link { font-size: 13px; font-weight: 500; background: rgba(255, 255, 255, 0.15); padding: 4px 8px; border-radius: 12px; }

.tab-scroll { background: #fff; border-bottom: 1px solid #e2e8f0; white-space: nowrap; height: 42px; }
.tab-list { display: flex; padding: 0 8px; }
.tab-item { padding: 10px 12px; font-size: 13px; color: #64748b; white-space: nowrap; }
.tab-item.active { color: #0284c7; font-weight: bold; border-bottom: 2px solid #0284c7; }
.order-scroll { flex: 1; padding: 10px; overflow-y: auto; }
.order-card { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.sn { font-weight: 700; font-size: 14px; color: #0f172a; }
.badges { display: flex; gap: 4px; }
.badge { font-size: 11px; padding: 2px 6px; border-radius: 4px; }
.bg-red { background: #ef4444; color: #fff; }
.state-10 { background: #f1f5f9; color: #64748b; }
.state-20 { background: #fef3c7; color: #d97706; }
.state-30 { background: #e0f2fe; color: #0284c7; }
.state-40 { background: #e0e7ff; color: #4338ca; }
.state-50 { background: #dcfce7; color: #15803d; }
.state-60 { background: #f0fdf4; color: #166534; font-weight: bold; }
.addr-box { background: #f8fafc; padding: 8px; border-radius: 6px; font-size: 12px; margin-bottom: 8px; }
.addr-line { display: flex; align-items: center; gap: 6px; }
.dot { font-size: 10px; color: #fff; padding: 1px 4px; border-radius: 3px; font-weight: bold; }
.bg-green { background: #10b981; }
.bg-orange { background: #f59e0b; }
.txt { color: #334155; }
.meta-row { display: flex; justify-content: space-between; align-items: baseline; font-size: 12px; color: #64748b; margin-bottom: 8px; }
.price { font-size: 16px; font-weight: 800; color: #ef4444; }
.card-footer { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #f1f5f9; padding-top: 8px; }
.time { font-size: 11px; color: #94a3b8; }
.btn-group { display: flex; gap: 6px; }
.action-btn { margin: 0; font-size: 11px; border-radius: 12px; }
.btn-ghost { background: #f1f5f9; color: #475569; }
.btn-primary { background: #0284c7; color: #fff; }
.btn-success { background: #10b981; color: #fff; }
.empty-wrap { padding: 40px 0; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-txt { font-size: 13px; color: #94a3b8; }
.empty-create-btn { background: #0284c7; color: #fff; font-size: 13px; border-radius: 16px; padding: 2px 14px; margin: 0; }
</style>
