<template>
  <view class="m-order-detail-container">
    <view class="nav-bar">
      <text class="back-btn" @click="goBack">‹ 返回列表</text>
      <text class="page-title">运单全景物流跟踪</text>
      <view style="width: 50px"></view>
    </view>

    <scroll-view scroll-y class="detail-scroll">
      <!-- 状态横幅 -->
      <view class="status-banner">
        <view class="status-main">
          <text class="status-txt">{{ detail?.orderStateDesc }}</text>
          <text class="sn">单号: {{ detail?.orderSn }}</text>
        </view>
        <view class="price-box">
          <text class="p-lbl">应付费用</text>
          <text class="p-val">¥{{ Number(detail?.feeTotal || 0).toFixed(2) }}</text>
        </view>
      </view>
      <!-- 取消/拒单提示 -->
      <view v-if="detail?.cancelReason" class="cancel-banner">
        <text class="banner-lbl">运单说明: </text>
        <text class="banner-txt">{{ detail.cancelReason }}</text>
      </view>


      <!-- 寄收航道与折线可视化 -->
      <view class="card-box">
        <view class="card-title">起止场站与飞行折线</view>
        <view class="addr-item">
          <text class="dot bg-green">起</text>
          <view class="info">
            <text class="addr">{{ detail?.senderAddr }}</text>
          </view>
        </view>
        <view class="addr-item" style="margin-top: 8px">
          <text class="dot bg-orange">止</text>
          <view class="info">
            <text class="addr">{{ detail?.receiverAddr }}</text>
          </view>
        </view>
        <view v-if="mapPolyline.length > 0" class="map-wrap" style="margin-top: 10px">
          <map
            id="order-detail-map"
            class="detail-map"
            :latitude="mapCenter.latitude"
            :longitude="mapCenter.longitude"
            :markers="mapMarkers"
            :polyline="mapPolyline"
            :scale="12"
          />
        </view>
      </view>

      <!-- 执飞与申报信息 -->
      <view class="card-box">
        <view class="card-title">执飞资产与合规联动</view>
        <view class="info-row"><text class="lbl">执飞机号:</text><text class="val">{{ detail?.uavSn || '待指派' }}</text></view>
        <view class="info-row"><text class="lbl">执飞机型:</text><text class="val">{{ detail?.uavModel || '-' }}</text></view>
        <view class="info-row"><text class="lbl">执飞机长:</text><text class="val">{{ detail?.pilotName || '待指派' }}</text></view>
        <view class="info-row" v-if="detail?.flightApplyDetail">
          <text class="lbl">飞行申报:</text>
          <text class="val text-primary">{{ detail.flightApplyDetail.applySn }} ({{ detail.flightApplyDetail.approveStateDesc }})</text>
        <view class="info-row" v-if="detail?.flightApplyDetail?.approveState === 2">
          <text class="lbl" style="color: #ef4444">驳回理由:</text>
          <text class="val" style="color: #ef4444; font-weight: bold">{{ detail.flightApplyDetail.approveNote || '未填写具体原因' }}</text>
        </view>

        </view>
        <view class="info-row" v-if="detail?.settleBill">
          <text class="lbl">财务对账单:</text>
          <text class="val text-success">{{ detail.settleBill.billSn }} ({{ detail.settleBill.billStateDesc }})</text>
        </view>
      </view>

      <!-- 变迁流水时间轴 -->
      <view class="card-box">
        <view class="card-title">物流轨迹全息流水</view>
        <view class="timeline">
          <view v-for="flow in detail?.flowList || []" :key="flow.flowId" class="time-item">
            <view class="node-dot"></view>
            <view class="time-content">
              <view class="time-row">
                <text class="time-state">{{ flow.toStateDesc }}</text>
                <text class="time-date">{{ flow.createdAt }}</text>
              </view>
              <text class="time-desc">{{ flow.remark }}</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'

const detail = ref<any>(null)
const mapCenter = ref({ latitude: 22.540, longitude: 114.000 })
const mapMarkers = ref<any[]>([])
const mapPolyline = ref<any[]>([])

const setupRouteMap = () => {
  if (!detail.value) return
  let pts: any[] = []
  if (detail.value.routePoints && detail.value.routePoints.length >= 2) {
    pts = detail.value.routePoints.map((p: number[]) => ({ latitude: p[1], longitude: p[0] }))
  } else if (detail.value.senderLat && detail.value.receiverLat) {
    pts = [
      { latitude: detail.value.senderLat, longitude: detail.value.senderLng },
      { latitude: detail.value.receiverLat, longitude: detail.value.receiverLng }
    ]
  }

  if (pts.length >= 2) {
    mapCenter.value = {
      latitude: (pts[0].latitude + pts[pts.length - 1].latitude) / 2,
      longitude: (pts[0].longitude + pts[pts.length - 1].longitude) / 2
    }
    mapMarkers.value = [
      { id: 1, latitude: pts[0].latitude, longitude: pts[0].longitude, title: '起飞场站', width: 24, height: 24 },
      { id: 2, latitude: pts[pts.length - 1].latitude, longitude: pts[pts.length - 1].longitude, title: '降落场站', width: 24, height: 24 }
    ]
    mapPolyline.value = [{
      points: pts,
      color: '#0284c7',
      width: 4,
      arrowLine: true
    }]
  }
}

const loadDetail = async (orderId: string) => {
  try {
    const res: any = await request.get(`/api/v1/order/${orderId}`)
    detail.value = res
    setupRouteMap()
  } catch (err: any) {
    uni.showToast({ title: '加载订单详情失败', icon: 'none' })
  }
}

const goBack = () => { uni.navigateBack() }

onLoad((options: any) => {
  if (options?.id) {
    loadDetail(options.id)
  }
})
</script>

<style scoped>
.m-order-detail-container { display: flex; flex-direction: column; height: 100vh; background: #f8fafc; }
.nav-bar { display: flex; justify-content: space-between; align-items: center; padding: 36px 14px 10px 14px; background: #0284c7; color: #fff; }
.back-btn { font-size: 15px; font-weight: 500; }
.page-title { font-size: 16px; font-weight: bold; }
.detail-scroll { flex: 1; padding: 12px; overflow-y: auto; }
.map-wrap { width: 100%; height: 180px; border-radius: 6px; overflow: hidden; background: #e2e8f0; }
.detail-map { width: 100%; height: 100%; }
.cancel-banner { background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; padding: 8px 12px; margin-bottom: 10px; font-size: 12px; }
.banner-lbl { font-weight: bold; color: #dc2626; }
.banner-txt { color: #dc2626; margin-left: 4px; }


.status-banner { display: flex; justify-content: space-between; align-items: center; background: #0284c7; color: #fff; padding: 14px; border-radius: 8px; margin-bottom: 10px; }
.status-txt { font-size: 18px; font-weight: 800; display: block; }
.sn { font-size: 12px; opacity: 0.85; margin-top: 2px; display: block; }
.price-box { text-align: right; }
.p-lbl { font-size: 11px; opacity: 0.8; display: block; }
.p-val { font-size: 20px; font-weight: 800; display: block; }
.card-box { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.card-title { font-size: 14px; font-weight: 700; color: #0f172a; margin-bottom: 10px; }
.addr-item { display: flex; gap: 8px; font-size: 13px; }
.dot { font-size: 11px; color: #fff; padding: 2px 6px; border-radius: 4px; font-weight: bold; height: fit-content; }
.bg-green { background: #10b981; }
.bg-orange { background: #f59e0b; }
.info-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px; }
.lbl { color: #64748b; }
.val { font-weight: 500; color: #1e293b; }
.text-primary { color: #0284c7; font-weight: bold; }
.text-success { color: #10b981; font-weight: bold; }
.timeline { display: flex; flex-direction: column; gap: 12px; padding-left: 6px; }
.time-item { display: flex; gap: 10px; position: relative; }
.node-dot { width: 8px; height: 8px; border-radius: 50%; background: #0284c7; margin-top: 4px; }
.time-content { flex: 1; }
.time-row { display: flex; justify-content: space-between; margin-bottom: 2px; }
.time-state { font-size: 13px; font-weight: bold; color: #0f172a; }
.time-date { font-size: 11px; color: #94a3b8; }
.time-desc { font-size: 12px; color: #64748b; }
</style>
