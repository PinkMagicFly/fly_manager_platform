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
          <template #header><div class="card-header"><span>4. 空间航线地图 (高德底座)</span></div></template>
          <div id="detail-map" class="map-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail } from '@/api/order'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const router = useRouter()
const detail = ref<any>(null)
let mapInstance: L.Map | null = null

const loadDetail = async () => {
  const id = route.params.id as string
  if (!id) return
  try {
    const res: any = await getOrderDetail(id)
    detail.value = res
    nextTick(() => {
      initMap(res)
    })
  } catch (err: any) {
    ElMessage.error(err.message || '加载订单详情失败')
  }
}

const initMap = (orderData: any) => {
  const container = document.getElementById('detail-map')
  if (!container) return
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }

  // 默认深圳中心
  mapInstance = L.map('detail-map', {
    zoomControl: true,
    attributionControl: false
  }).setView([22.540, 114.000], 12)

  // 挂载高德全中文高精底图
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    minZoom: 3,
    maxZoom: 18
  }).addTo(mapInstance)

  const latlngs: L.LatLngExpression[] = []

  if (orderData?.routePoints && orderData.routePoints.length >= 2) {
    orderData.routePoints.forEach((pt: number[]) => {
      latlngs.push([pt[1], pt[0]])
    })
  } else if (orderData?.senderLat && orderData?.receiverLat) {
    latlngs.push([orderData.senderLat, orderData.senderLng])
    latlngs.push([orderData.receiverLat, orderData.receiverLng])
  }

  if (latlngs.length >= 2) {
    const startPoint = latlngs[0] as [number, number]
    const endPoint = latlngs[latlngs.length - 1] as [number, number]

    // 起点绿标
    L.circleMarker(startPoint, {
      radius: 8,
      fillColor: '#10b981',
      color: '#ffffff',
      weight: 2,
      fillOpacity: 1
    }).addTo(mapInstance).bindPopup(`<b>起点</b>: ${orderData.senderAddr || ''}`)

    // 终点橙标
    L.circleMarker(endPoint, {
      radius: 8,
      fillColor: '#f59e0b',
      color: '#ffffff',
      weight: 2,
      fillOpacity: 1
    }).addTo(mapInstance).bindPopup(`<b>终点</b>: ${orderData.receiverAddr || ''}`)

    // 规划航线折线
    const polyline = L.polyline(latlngs, {
      color: '#0284c7',
      weight: 4,
      dashArray: '6, 6',
      opacity: 0.9
    }).addTo(mapInstance)

    mapInstance.fitBounds(polyline.getBounds(), { padding: [40, 40] })
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
</style>
