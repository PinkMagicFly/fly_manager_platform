<template>
  <div class="order-detail-container">
    <div class="top-nav">
      <el-button @click="goBack"><el-icon><Back /></el-icon> 返回看板</el-button>
      <div class="nav-title">
        <span>运单档案: <strong>{{ detail?.orderSn }}</strong></span>
        <el-tag :type="getStateTagType(detail?.orderState)" size="large" effect="dark" style="margin-left: 10px">
          {{ detail?.orderStateDesc }}
        </el-tag>
      </div>
    </div>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-card shadow="never" class="info-card">
          <template #header><div class="card-header"><span>1. 基础物流与货物</span></div></template>
          <el-alert
            v-if="detail?.cancelReason"
            type="warning"
            :closable="false"
            show-icon
            :title="'运单取消/拒单说明：' + detail?.cancelReason"
            style="margin-bottom: 10px"
          />

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="流水号">{{ detail?.orderSn }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ detail?.customerName || '个人客户' }}</el-descriptions-item>
            <el-descriptions-item label="货物类别">{{ detail?.goodsTypeDesc }}</el-descriptions-item>
            <el-descriptions-item label="重量"><strong>{{ detail?.goodsWeight }}</strong> kg</el-descriptions-item>
            <el-descriptions-item label="时效">
              <el-tag :type="detail?.urgentFlag === 1 ? 'danger' : 'info'" size="small">
                {{ detail?.urgentFlag === 1 ? '加急优先' : '标准配送' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="商户">{{ detail?.merchantName || '自营' }}</el-descriptions-item>
            <el-descriptions-item label="起点" :span="2"><span class="text-green">{{ detail?.senderAddr }}</span></el-descriptions-item>
            <el-descriptions-item label="终点" :span="2"><span class="text-orange">{{ detail?.receiverAddr }}</span></el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="info-card" style="margin-top: 10px">
          <template #header><div class="card-header"><span>2. 运费与账单明细</span></div></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="基础运费">¥{{ Number(detail?.feeBase || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="空域占用费">¥{{ Number(detail?.feeAirspace || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="加急费">¥{{ Number(detail?.feeUrgent || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="优惠">-¥{{ Number(detail?.feeDiscount || 0).toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="应付总额"><span class="price-highlight">¥{{ Number(detail?.feeTotal || 0).toFixed(2) }}</span></el-descriptions-item>
            <el-descriptions-item label="支付状态">
              <el-tag :type="detail?.payState === 1 ? 'success' : 'danger'" size="small">{{ detail?.payStateDesc }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="对账单" :span="2">
              <el-link v-if="detail?.settleBill" type="primary" @click="goToBills">
                {{ detail?.settleBill?.billSn }} ({{ detail?.settleBill?.billStateDesc }})
              </el-link>
              <span v-else class="text-muted">送达签收后自动出账</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="info-card" style="margin-top: 10px">
          <template #header><div class="card-header"><span>状态变迁流水</span></div></template>
          <el-timeline style="padding-left: 6px; margin-top: 6px">
            <el-timeline-item v-for="flow in detail?.flowList || []" :key="flow.flowId" :timestamp="flow.createdAt" size="small">
              <div><strong>{{ flow.fromStateDesc }}</strong> → <strong>{{ flow.toStateDesc }}</strong></div>
              <div class="flow-desc">{{ flow.remark }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="info-card">
          <template #header><div class="card-header"><span>3. 飞行计划申报与审批</span></div></template>
          <div v-if="detail?.flightApplyDetail">
            <el-alert
              v-if="detail?.flightApplyDetail?.approveState === 2"
              type="error"
              :closable="false"
              show-icon
              :title="'空管审批驳回原因：' + (detail?.flightApplyDetail?.approveNote || '未填写具体原因')"
              style="margin-bottom: 8px"
            />

            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="申报号"><strong>{{ detail?.flightApplyDetail?.applySn }}</strong></el-descriptions-item>
              <el-descriptions-item label="审批">
                <el-tag :type="getApproveTagType(detail?.flightApplyDetail?.approveState)" size="small">
                  {{ detail?.flightApplyDetail?.approveStateDesc }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="机号">{{ detail?.uavSn || '-' }}</el-descriptions-item>
              <el-descriptions-item label="机型">{{ detail?.uavModel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="飞手">{{ detail?.pilotName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="高度">{{ detail?.flightApplyDetail?.altPlanned }}m</el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="暂未调度派单" :image-size="40" />
        </el-card>

        <el-card shadow="never" class="info-card" style="margin-top: 10px">
          <template #header>
            <div class="card-header" style="justify-content: space-between; width: 100%;">
              <span>4. 空间航线与在途轨迹回放 (高德底座)</span>
              <div class="trace-legend" v-if="traceData">
                <span class="legend-item"><span class="line bg-plan"></span> 申报航线</span>
                <span class="legend-item"><span class="line bg-actual"></span> 实际在途航迹</span>
                <span class="legend-item" v-if="detail?.orderState === 50"><span class="live-dot"></span> 实时执飞中 (5s轮询)</span>
              </div>
            </div>
          </template>
          <div id="detail-map" class="map-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail, getOrderTrace } from '@/api/order'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const router = useRouter()
const detail = ref<any>(null)
const traceData = ref<any>(null)

let mapInstance: L.Map | null = null
let planPolyline: L.Polyline | null = null
let actualPolyline: L.Polyline | null = null
let uavMarker: L.Marker | null = null
let pollTimer: any = null

const loadDetail = async () => {
  const id = route.params.id as string
  if (!id) return
  try {
    const res: any = await getOrderDetail(id)
    detail.value = res
    await loadTrace(id)
    nextTick(() => {
      initMap()
    })
    if (res.orderState === 50) {
      startPolling(id)
    }
  } catch (err: any) {
    ElMessage.error(err.message || '加载订单详情失败')
  }
}

const loadTrace = async (orderId: string | number) => {
  try {
    const res: any = await getOrderTrace(orderId)
    traceData.value = res?.data || res
  } catch (e) {
    console.warn('获取航迹回放失败:', e)
  }
}

const startPolling = (orderId: string | number) => {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res: any = await getOrderTrace(orderId)
      const data = res?.data || res
      traceData.value = data
      updateActualTraceOnMap()
      if (data.orderState && data.orderState !== 50) {
        stopPolling()
        if (detail.value) {
          detail.value.orderState = data.orderState
          detail.value.orderStateDesc = data.orderStateDesc
        }
      }
    } catch (e) {
      console.warn('航迹轮询失败:', e)
    }
  }, 5000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const initMap = () => {
  const container = document.getElementById('detail-map')
  if (!container) return
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }

  mapInstance = L.map('detail-map', {
    zoomControl: true,
    attributionControl: false
  }).setView([22.540, 114.000], 12)

  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    minZoom: 3,
    maxZoom: 18
  }).addTo(mapInstance)

  const boundsGroup: L.LatLng[] = []

  const planPts: L.LatLngExpression[] = []
  if (traceData.value?.planLine && traceData.value.planLine.length >= 2) {
    traceData.value.planLine.forEach((p: number[]) => {
      planPts.push([p[1], p[0]])
      boundsGroup.push(L.latLng(p[1], p[0]))
    })
  } else if (detail.value?.routePoints && detail.value.routePoints.length >= 2) {
    detail.value.routePoints.forEach((p: number[]) => {
      planPts.push([p[1], p[0]])
      boundsGroup.push(L.latLng(p[1], p[0]))
    })
  } else if (detail.value?.senderLat && detail.value?.receiverLat) {
    planPts.push([detail.value.senderLat, detail.value.senderLng])
    planPts.push([detail.value.receiverLat, detail.value.receiverLng])
    boundsGroup.push(L.latLng(detail.value.senderLat, detail.value.senderLng))
    boundsGroup.push(L.latLng(detail.value.receiverLat, detail.value.receiverLng))
  }

  if (planPts.length >= 2) {
    const startPoint = planPts[0] as [number, number]
    const endPoint = planPts[planPts.length - 1] as [number, number]

    L.circleMarker(startPoint, {
      radius: 8,
      fillColor: '#10b981',
      color: '#ffffff',
      weight: 2,
      fillOpacity: 1
    }).addTo(mapInstance).bindPopup(`<b>起点</b>: ${detail.value?.senderAddr || ''}`)

    L.circleMarker(endPoint, {
      radius: 8,
      fillColor: '#f59e0b',
      color: '#ffffff',
      weight: 2,
      fillOpacity: 1
    }).addTo(mapInstance).bindPopup(`<b>终点</b>: ${detail.value?.receiverAddr || ''}`)

    planPolyline = L.polyline(planPts, {
      color: '#10b981',
      weight: 4,
      opacity: 0.85
    }).addTo(mapInstance).bindTooltip('🟢 申报合规航线', { permanent: false })
  }

  updateActualTraceOnMap()

  if (boundsGroup.length >= 2 && mapInstance) {
    mapInstance.fitBounds(L.latLngBounds(boundsGroup), { padding: [40, 40] })
  }
}

const updateActualTraceOnMap = () => {
  if (!mapInstance) return

  const actualList = traceData.value?.actual || []
  const actualLatLngs: L.LatLngExpression[] = actualList.map((pt: any) => [pt.lat, pt.lng])

  if (actualPolyline) {
    actualPolyline.setLatLngs(actualLatLngs)
  } else if (actualLatLngs.length >= 2) {
    actualPolyline = L.polyline(actualLatLngs, {
      color: '#2563eb',
      weight: 4,
      opacity: 0.95
    }).addTo(mapInstance).bindTooltip('🔵 实际在途航迹', { permanent: false })
  }

  if (actualList.length > 0) {
    const latest = actualList[actualList.length - 1]
    const uavPos: [number, number] = [latest.lat, latest.lng]

    const popupHtml = `
      <div style="font-size:12px; line-height:1.6; min-width:160px;">
        <b style="color:#0284c7; font-size:13px;">✈️ 执飞无人机最新状态</b><br/>
        <b>机号:</b> ${traceData.value?.uavSn || detail.value?.uavSn || '-'}<br/>
        <b>机型:</b> ${traceData.value?.uavModel || detail.value?.uavModel || '-'}<br/>
        <b>高度:</b> ${latest.alt != null ? latest.alt + ' 米' : '-'}<br/>
        <b>地速:</b> ${latest.speed != null ? latest.speed + ' m/s' : '-'}<br/>
        <b>电量:</b> ${latest.batteryRemain != null ? latest.batteryRemain + '%' : '-'}<br/>
        <b>时间:</b> ${latest.reportAt ? latest.reportAt.replace('T', ' ') : '-'}
      </div>
    `

    const uavIcon = L.divIcon({
      className: 'custom-uav-marker',
      html: '<div class="uav-pin-pulse">✈️</div>',
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    })

    if (uavMarker) {
      uavMarker.setLatLng(uavPos)
      uavMarker.setPopupContent(popupHtml)
    } else {
      uavMarker = L.marker(uavPos, { icon: uavIcon }).addTo(mapInstance).bindPopup(popupHtml)
      if (detail.value?.orderState === 50) {
        uavMarker.openPopup()
      }
    }
  }
}


const goBack = () => { router.push('/order/board') }
const goToBills = () => { router.push('/order/bills') }

const getStateTagType = (state: number) => {
  switch (state) {
    case 10: return 'info'
    case 20: return 'warning'
    case 30: return 'primary'
    case 40: return ''
    case 50: return 'success'
    case 60: return 'success'
    case 70: return 'danger'
    case 80: return 'danger'
    default: return 'info'
  }
}

const getApproveTagType = (state: number) => {
  switch (state) {
    case 0: return 'warning'
    case 1: return 'success'
    case 2: return 'danger'
    case 3: return 'info'
    default: return 'info'
  }
}

onMounted(() => { loadDetail() })
onUnmounted(() => {
  stopPolling()
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})
</script>

<style scoped>
.order-detail-container { display: flex; flex-direction: column; gap: 12px; }
.top-nav { display: flex; align-items: center; gap: 14px; margin-bottom: 6px; }
.nav-title { font-size: 16px; color: #1e293b; display: flex; align-items: center; }
.info-card { border-radius: 8px; }
.card-header { font-weight: 600; font-size: 14px; color: #0f172a; display: flex; align-items: center; gap: 6px; }
.text-green { color: #10b981; font-weight: 500; }
.text-orange { color: #f59e0b; font-weight: 500; }
.price-highlight { font-size: 18px; font-weight: 700; color: #ef4444; }
.map-box { width: 100%; height: 380px; border-radius: 6px; overflow: hidden; background: #e2e8f0; }
.flow-title { font-size: 13px; color: #1e293b; margin-bottom: 2px; }
.flow-desc { font-size: 12px; color: #64748b; }

.trace-legend { display: flex; align-items: center; gap: 12px; font-size: 12px; font-weight: normal; color: #475569; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.line { width: 14px; height: 4px; border-radius: 2px; }
.bg-plan { background-color: #10b981; }
.bg-actual { background-color: #2563eb; }
.live-dot {
  width: 8px;
  height: 8px;
  background-color: #10b981;
  border-radius: 50%;
  display: inline-block;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% { transform: scale(0.9); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.8; }
}
:deep(.uav-pin-pulse) {
  font-size: 18px;
  line-height: 28px;
  text-align: center;
  background: rgba(37, 99, 235, 0.2);
  border-radius: 50%;
  box-shadow: 0 0 10px rgba(37, 99, 235, 0.6);
  animation: float 2s ease-in-out infinite alternate;
}
@keyframes float {
  from { transform: translateY(0); }
  to { transform: translateY(-4px); }
}
</style>
