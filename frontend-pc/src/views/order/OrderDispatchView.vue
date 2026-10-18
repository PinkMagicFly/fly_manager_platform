<template>
  <div class="dispatch-workbench-container">
    <div class="workbench-header">
      <div class="header-left">
        <span class="workbench-title"><el-icon><Compass /></el-icon> 智能调度中枢工作台</span>
        <el-tag type="primary" effect="dark" size="small">实时提交飞行任务审批</el-tag>
        <span class="sub-text">场站就近吸附 / 航线智能避障自适应微调</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="loadPendingOrders"><el-icon><Refresh /></el-icon> 刷新待调度</el-button>
        <el-button size="small" type="info" plain @click="goToBoard">返回看板</el-button>
      </div>
    </div>

    <div class="workbench-body">
      <!-- 左栏：待调度运单池 -->
      <div class="left-panel">
        <div class="panel-head"><span>待调度池 ({{ pendingOrders.length }} 单)</span></div>
        <div class="order-list-scroll" v-loading="loadingOrders">
          <div v-if="pendingOrders.length === 0" class="empty-box"><el-empty description="暂无待调度运单" :image-size="60" /></div>
          <div
            v-for="order in pendingOrders"
            :key="order.orderId"
            class="order-item-card"
            :class="{ active: selectedOrder?.orderId === order.orderId }"
            @click="selectOrder(order)"
          >
            <div class="order-card-top">
              <span class="sn">{{ order.orderSn }}</span>
              <el-tag v-if="order.urgentFlag === 1" size="small" type="danger">加急</el-tag>
            </div>
            <div class="goods-meta">{{ order.goodsTypeDesc }} · <strong>{{ order.goodsWeight }}kg</strong> · ¥{{ Number(order.feeTotal).toFixed(2) }}</div>
            <div class="addr-meta text-green">起: {{ order.senderAddr }}</div>
            <div class="addr-meta text-orange">止: {{ order.receiverAddr }}</div>
          </div>
        </div>
      </div>

      <!-- 中间：高德地图与航线工具栏 -->
      <div class="middle-map-panel">
        <div class="map-toolbar">
          <div class="toolbar-left">
            <el-radio-group v-model="mapMode" size="small" @change="switchMapLayer">
              <el-radio-button label="vector">高德高精路网</el-radio-button>
              <el-radio-button label="satellite">高清遥感卫星</el-radio-button>
            </el-radio-group>
            <el-button-group v-if="selectedOrder && snappedSiteA && snappedSiteB" style="margin-left: 10px">
              <el-button size="small" type="primary" :loading="planningRoute" @click="handleAutoPlan">
                <el-icon><MagicStick /></el-icon> 自动规划航线
              </el-button>
              <el-button size="small" type="warning" plain @click="handleManualDraw">
                <el-icon><EditPen /></el-icon> 手绘/加点
              </el-button>
              <el-button size="small" type="info" plain @click="translateIntermediate(0.005, 0.005)">
                <el-icon><Rank /></el-icon> 平移微调
              </el-button>
              <el-button size="small" type="danger" plain @click="handleResetRoute">
                <el-icon><Delete /></el-icon> 重置折线
              </el-button>
            </el-button-group>
          </div>
          <div class="toolbar-right">
            <span v-if="selectedOrder" class="map-hint">
              选中: {{ selectedOrder.orderSn }} | 折线点数: {{ currentRoutePoints.length }}
            </span>
            <span v-else class="map-hint">请从左侧选择待调度运单</span>
          </div>
        </div>
        <div id="dispatch-map" class="dispatch-map-view"></div>
        <div v-if="routeStatusMsg" class="map-status-overlay" :class="{ 'error-overlay': hasViolation }">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ routeStatusMsg }}</span>
        </div>
      </div>

      <!-- 右栏：决策与申报面板 -->
      <div class="right-panel" v-loading="loadingRecommend || loadingSnap">
        <div class="panel-head"><span>智能调度方案与任务申报</span></div>
        <div v-if="!selectedOrder" class="empty-box"><el-empty description="请选择待调度运单" :image-size="80" /></div>
        <div v-else class="recommend-content">
          <!-- 运单基础信息卡 -->
          <div class="summary-card">
            <div class="summary-row">
              <span>运单总重: <strong>{{ selectedOrder.goodsWeight }} kg</strong></span>
              <span>预估航程: <strong>{{ recommendData?.distanceKm?.toFixed(1) || '-' }} km</strong></span>
            </div>
            <div class="summary-row" style="margin-top: 4px">
              <span>运费总额: <strong class="text-danger">¥{{ Number(selectedOrder.feeTotal).toFixed(2) }}</strong></span>
              <span>时效: {{ selectedOrder.urgentFlag === 1 ? '加急' : '标准' }}</span>
            </div>
          </div>

          <!-- 场站就近吸附状态卡 -->
          <div class="snap-box">
            <div class="snap-title"><el-icon><LocationInformation /></el-icon> 场站就近吸附匹配结果</div>
            <div v-if="snapError" class="snap-error text-danger">
              <el-icon><WarningFilled /></el-icon> {{ snapError }}
            </div>
            <div v-else class="snap-details">
              <div class="snap-item">
                <span class="tag-badge bg-green">起飞</span>
                <span class="site-name">{{ snappedSiteA?.siteName }}</span>
                <span class="site-dist">距寄件 {{ snappedSiteA?.distanceMeters }}m</span>
              </div>
              <div class="snap-item">
                <span class="tag-badge bg-orange">降落</span>
                <span class="site-name">{{ snappedSiteB?.siteName }}</span>
                <span class="site-dist">距收件 {{ snappedSiteB?.distanceMeters }}m</span>
              </div>
              <div class="snap-tip">服务半径 5000 米覆盖圈已在地图上标注 (首末端点自动锚定场站)</div>
            </div>
          </div>

          <!-- 空域合规校验状态 -->
          <div class="compliance-box" :class="{ pass: !hasViolation && currentRoutePoints.length >= 2, fail: hasViolation }">
            <div class="comp-head">
              <span>空域网格碰撞复核: </span>
              <el-tag v-if="validating" size="small" type="info">校验中...</el-tag>
              <el-tag v-else-if="currentRoutePoints.length < 2" size="small" type="info">待规划</el-tag>
              <el-tag v-else-if="!hasViolation" size="small" type="success" effect="dark">合规通过</el-tag>
              <el-tag v-else size="small" type="danger" effect="dark">存在空域冲突</el-tag>
            </div>
            <div v-if="violationDetail" class="comp-detail text-danger">{{ violationDetail }}</div>
          </div>

          <!-- 算法推荐卡 -->
          <div v-if="recommendData?.recommendedUav" class="rec-uav-box">
            <div class="rec-title">
              <el-icon class="text-orange"><Trophy /></el-icon>
              <span>算法排他性优选推荐</span>
              <el-tag size="small" type="success" effect="dark">得分 {{ recommendData.recommendedUav.score }}</el-tag>
            </div>
            <div class="rec-detail">
              <div class="uav-name">{{ recommendData.recommendedUav.uavSn }} ({{ recommendData.recommendedUav.model }})</div>
              <div class="uav-metrics">
                <span>电量: <strong>{{ recommendData.recommendedUav.batteryPct }}%</strong></span>
                <span>载重: <strong>{{ recommendData.recommendedUav.maxLoad }}kg</strong></span>
              </div>
              <div class="rec-reason">{{ recommendData.recommendedUav.matchReason }}</div>
            </div>
          </div>

          <!-- 派单表单 -->
          <el-form label-position="top" class="dispatch-form" size="small">
            <el-form-item label="指派执飞无人机" required>
              <el-select v-model="dispatchForm.uavId" placeholder="选择无人机" style="width: 100%">
                <el-option
                  v-for="u in recommendData?.candidateUavs || []"
                  :key="u.uavId"
                  :label="`${u.uavSn} - ${u.model} (载重${u.maxLoad}kg, 电量${u.batteryPct}%)`"
                  :value="u.uavId"
                  :disabled="u.score === 0"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="指派责任机长" required>
              <el-select v-model="dispatchForm.pilotId" placeholder="选择值班机长" style="width: 100%">
                <el-option
                  v-for="p in recommendData?.availablePilots || []"
                  :key="p.crewId"
                  :label="`${p.realName} (执照:${p.licenseNo}, ${p.flyHours}h)`"
                  :value="p.crewId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="巡航高度 (米)">
              <el-input-number v-model="dispatchForm.altPlanned" :min="60" :max="150" :step="10" style="width: 100%" @change="scheduleRevalidate" />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              :loading="dispatching"
              :disabled="hasViolation || !snappedSiteA || !snappedSiteB || currentRoutePoints.length < 2"
              style="width: 100%; margin-top: 10px"
              @click="submitDispatch"
            >
              <el-icon><Check /></el-icon> 提交飞行任务审批
            </el-button>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Compass, Refresh, MagicStick, EditPen, Rank, Delete, InfoFilled,
  LocationInformation, WarningFilled, Trophy, Check
} from '@element-plus/icons-vue'
import { getOrderPage, getRecommendDispatch, dispatchOrder } from '@/api/order'
import { snapSite } from '@/api/asset'
import { planAirspaceRoute, validateAirspaceRoute } from '@/api/airspace'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const router = useRouter()

const loadingOrders = ref(false)
const loadingRecommend = ref(false)
const loadingSnap = ref(false)
const planningRoute = ref(false)
const validating = ref(false)
const dispatching = ref(false)

const pendingOrders = ref<any[]>([])
const selectedOrder = ref<any>(null)
const recommendData = ref<any>(null)

// 场站吸附数据
const snappedSiteA = ref<any>(null)
const snappedSiteB = ref<any>(null)
const snapError = ref('')

// 航线点序列 [[lng, lat], ...]
const currentRoutePoints = ref<[number, number][]>([])
const hasViolation = ref(false)
const violationDetail = ref('')
const routeStatusMsg = ref('')

let revalidateTimer: any = null

const mapMode = ref('vector')
let mapInstance: L.Map | null = null
let currentTileLayer: L.TileLayer | null = null
let siteLayerGroup: L.LayerGroup | null = null
let routeLayerGroup: L.LayerGroup | null = null
let waypointMarkers: L.Marker[] = []
let polylineLayer: L.Polyline | null = null

const dispatchForm = reactive({
  orderId: undefined as number | undefined,
  uavId: undefined as number | undefined,
  pilotId: undefined as number | undefined,
  altPlanned: 110,
  routeLine: ''
})


const initMap = () => {
  const container = document.getElementById('dispatch-map')
  if (!container) return
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }

  mapInstance = L.map('dispatch-map', {
    zoomControl: true,
    attributionControl: false
  }).setView([22.540, 114.000], 12)

  siteLayerGroup = L.layerGroup().addTo(mapInstance)
  routeLayerGroup = L.layerGroup().addTo(mapInstance)
  switchMapLayer(mapMode.value)

  // 绑定地图点击事件：向航线插入途经点
  mapInstance.on('click', (e: L.LeafletMouseEvent) => {
    if (!snappedSiteA.value || !snappedSiteB.value) return
    const clickLng = Number(e.latlng.lng.toFixed(6))
    const clickLat = Number(e.latlng.lat.toFixed(6))
    insertWaypoint(clickLng, clickLat)
  })
}

const switchMapLayer = (mode: string) => {
  if (!mapInstance) return
  if (currentTileLayer) mapInstance.removeLayer(currentTileLayer)

  if (mode === 'vector') {
    currentTileLayer = L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      subdomains: ['1', '2', '3', '4'], minZoom: 3, maxZoom: 18
    }).addTo(mapInstance)
  } else {
    currentTileLayer = L.tileLayer('https://webst0{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}', {
      subdomains: ['1', '2', '3', '4'], minZoom: 3, maxZoom: 18
    }).addTo(mapInstance)
  }
}

const loadPendingOrders = async () => {
  loadingOrders.value = true
  try {
    const res: any = await getOrderPage({ orderState: 30, size: 50 })
    pendingOrders.value = res?.list || []

    const queryOrderId = route.query.orderId ? Number(route.query.orderId) : null
    if (queryOrderId) {
      const target = pendingOrders.value.find(o => o.orderId === queryOrderId)
      if (target) selectOrder(target)
      else if (pendingOrders.value.length > 0) selectOrder(pendingOrders.value[0])
    } else if (pendingOrders.value.length > 0 && !selectedOrder.value) {
      selectOrder(pendingOrders.value[0])
    }
  } catch (err: any) {
    ElMessage.error(err.message || '加载待调度订单失败')
  } finally {
    loadingOrders.value = false
  }
}

const selectOrder = async (order: any) => {
  selectedOrder.value = order
  dispatchForm.orderId = order.orderId
  dispatchForm.uavId = undefined
  dispatchForm.pilotId = undefined
  snapError.value = ''
  currentRoutePoints.value = []
  routeStatusMsg.value = ''
  hasViolation.value = false
  violationDetail.value = ''

  loadingRecommend.value = true
  try {
    const res: any = await getRecommendDispatch(order.orderId)
    recommendData.value = res
    if (res?.recommendedUav) {
      dispatchForm.uavId = res.recommendedUav.uavId
    }
    if (res?.availablePilots && res.availablePilots.length > 0) {
      dispatchForm.pilotId = res.availablePilots[0].crewId
    }
  } catch (e) {
  } finally {
    loadingRecommend.value = false
  }

  // 执行场站就近吸附与航线渲染
  await performSiteSnapAndRoute(order)
}

const performSiteSnapAndRoute = async (order: any) => {
  if (!mapInstance || !siteLayerGroup || !routeLayerGroup) return
  siteLayerGroup.clearLayers()
  routeLayerGroup.clearLayers()
  waypointMarkers = []
  polylineLayer = null

  loadingSnap.value = true
  try {
    // 1. 寄件坐标与收件坐标就近吸附起降场站
    const [siteA, siteB] = await Promise.all([
      snapSite(order.senderLng, order.senderLat),
      snapSite(order.receiverLng, order.receiverLat)
    ])
    snappedSiteA.value = siteA
    snappedSiteB.value = siteB

    // 绘制寄件客户点与收件客户点
    const senderLatLng: [number, number] = [order.senderLat, order.senderLng]
    const receiverLatLng: [number, number] = [order.receiverLat, order.receiverLng]
    L.circleMarker(senderLatLng, {
      radius: 6, fillColor: '#10b981', color: '#fff', weight: 1.5, fillOpacity: 0.9
    }).addTo(siteLayerGroup).bindPopup(`<b>寄件需求点</b>: ${order.senderAddr}`)

    L.circleMarker(receiverLatLng, {
      radius: 6, fillColor: '#f59e0b', color: '#fff', weight: 1.5, fillOpacity: 0.9
    }).addTo(siteLayerGroup).bindPopup(`<b>收件需求点</b>: ${order.receiverAddr}`)

    // 绘制吸附的起飞场站与服务半径圈 (5000m)
    const siteALatLng: [number, number] = [siteA.latitude, siteA.longitude]
    L.circleMarker(siteALatLng, {
      radius: 10, fillColor: '#0284c7', color: '#ffffff', weight: 2.5, fillOpacity: 1
    }).addTo(siteLayerGroup).bindPopup(`<b>起飞场站</b>: ${siteA.siteName}<br/>距寄件点: ${siteA.distanceMeters}米`)

    L.circle(siteALatLng, {
      radius: siteA.serviceRadius || 5000,
      color: '#0284c7', weight: 1.5, dashArray: '5,5', fillColor: '#38bdf8', fillOpacity: 0.08
    }).addTo(siteLayerGroup)

    // 绘制吸附的降落场站与服务半径圈 (5000m)
    const siteBLatLng: [number, number] = [siteB.latitude, siteB.longitude]
    L.circleMarker(siteBLatLng, {
      radius: 10, fillColor: '#f97316', color: '#ffffff', weight: 2.5, fillOpacity: 1
    }).addTo(siteLayerGroup).bindPopup(`<b>降落场站</b>: ${siteB.siteName}<br/>距收件点: ${siteB.distanceMeters}米`)

    L.circle(siteBLatLng, {
      radius: siteB.serviceRadius || 5000,
      color: '#f97316', weight: 1.5, dashArray: '5,5', fillColor: '#fdba74', fillOpacity: 0.08
    }).addTo(siteLayerGroup)

    mapInstance.fitBounds(L.latLngBounds([siteALatLng, siteBLatLng]), { padding: [60, 60] })

    // 自动触发一次智能避障航线规划
    await handleAutoPlan()
  } catch (err: any) {
    snappedSiteA.value = null
    snappedSiteB.value = null
    snapError.value = err.message || '寄件/收件地址不在任何场站服务范围内'
    ElMessage.error(snapError.value)
  } finally {
    loadingSnap.value = false
  }
}


const handleAutoPlan = async () => {
  if (!snappedSiteA.value || !snappedSiteB.value) return
  planningRoute.value = true
  try {
    const res: any = await planAirspaceRoute({
      startLng: snappedSiteA.value.longitude,
      startLat: snappedSiteA.value.latitude,
      endLng: snappedSiteB.value.longitude,
      endLat: snappedSiteB.value.latitude,
      altPlanned: dispatchForm.altPlanned
    })

    if (res && res.routeLine && res.routeLine.length >= 2) {
      currentRoutePoints.value = res.routeLine
      routeStatusMsg.value = res.message || '航线自动规划完成'
      hasViolation.value = !res.pass
      violationDetail.value = res.violations && res.violations.length > 0 ? res.violations.join('; ') : ''
      renderRoute()
      if (res.pass) {
        ElMessage.success(routeStatusMsg.value)
      } else {
        ElMessage.warning(routeStatusMsg.value)
      }
    }
  } catch (err: any) {
    ElMessage.error(err.message || '自动规划航线失败')
  } finally {
    planningRoute.value = false
  }
}

const handleManualDraw = () => {
  ElMessage.info('手绘模式已激活：请在地图上点击添加航路点，右键途经点可直接删除')
}

const handleResetRoute = () => {
  if (!snappedSiteA.value || !snappedSiteB.value) return
  currentRoutePoints.value = [
    [snappedSiteA.value.longitude, snappedSiteA.value.latitude],
    [snappedSiteB.value.longitude, snappedSiteB.value.latitude]
  ]
  renderRoute()
  scheduleRevalidate()
  ElMessage.info('已恢复为两场站直达基准线，可点击地图添加途经点')
}

// 向折线插入新的途经点
const insertWaypoint = (lng: number, lat: number) => {
  if (currentRoutePoints.value.length < 2) return
  const pts = currentRoutePoints.value
  let bestIdx = pts.length - 1
  let minDist = Number.MAX_VALUE

  for (let i = 0; i < pts.length - 1; i++) {
    const midLng = (pts[i][0] + pts[i + 1][0]) / 2
    const midLat = (pts[i][1] + pts[i + 1][1]) / 2
    const d = Math.hypot(lng - midLng, lat - midLat)
    if (d < minDist) {
      minDist = d
      bestIdx = i + 1
    }
  }

  currentRoutePoints.value.splice(bestIdx, 0, [lng, lat])
  renderRoute()
  scheduleRevalidate()
  ElMessage.info(`已插入航路点 [${lng}, ${lat}]，正在自动复核合规性...`)
}

// 平移中间途经点向量 (首尾场站锚定保持不变)
const translateIntermediate = (dLng: number, dLat: number) => {
  if (currentRoutePoints.value.length <= 2) {
    const p1 = currentRoutePoints.value[0]
    const p2 = currentRoutePoints.value[1]
    const midLng = Number(((p1[0] + p2[0]) / 2 + dLng).toFixed(6))
    const midLat = Number(((p1[1] + p2[1]) / 2 + dLat).toFixed(6))
    currentRoutePoints.value.splice(1, 0, [midLng, midLat])
  } else {
    for (let i = 1; i < currentRoutePoints.value.length - 1; i++) {
      currentRoutePoints.value[i][0] = Number((currentRoutePoints.value[i][0] + dLng).toFixed(6))
      currentRoutePoints.value[i][1] = Number((currentRoutePoints.value[i][1] + dLat).toFixed(6))
    }
  }
  renderRoute()
  scheduleRevalidate()
  ElMessage.success('航线中间段已平移微调，正在复检...')
}

const renderRoute = () => {
  if (!mapInstance || !routeLayerGroup) return
  routeLayerGroup.clearLayers()
  waypointMarkers = []

  const latlngs: [number, number][] = currentRoutePoints.value.map(pt => [pt[1], pt[0]])
  if (latlngs.length < 2) return

  // 绘制折线
  polylineLayer = L.polyline(latlngs, {
    color: hasViolation.value ? '#ef4444' : '#0284c7',
    weight: 4,
    dashArray: hasViolation.value ? '6,6' : undefined,
    opacity: 0.95
  }).addTo(routeLayerGroup)

  // 为每个中间途经点生成可拖拽控制点
  for (let i = 1; i < currentRoutePoints.value.length - 1; i++) {
    const pt = currentRoutePoints.value[i]
    const marker = L.circleMarker([pt[1], pt[0]], {
      radius: 7,
      fillColor: '#ffffff',
      color: hasViolation.value ? '#ef4444' : '#0284c7',
      weight: 3,
      fillOpacity: 1
    }).addTo(routeLayerGroup)

    const handleIcon = L.divIcon({
      className: 'waypoint-drag-handle',
      html: `<div style="width:18px;height:18px;border-radius:50%;background:#38bdf8;border:2px solid #fff;box-shadow:0 0 6px rgba(0,0,0,0.3);cursor:move;"></div>`,
      iconSize: [18, 18],
      iconAnchor: [9, 9]
    })

    const dragMarker = L.marker([pt[1], pt[0]], {
      draggable: true,
      icon: handleIcon
    }).addTo(routeLayerGroup)

    dragMarker.on('drag', (e: any) => {
      const newPos = e.target.getLatLng()
      currentRoutePoints.value[i] = [Number(newPos.lng.toFixed(6)), Number(newPos.lat.toFixed(6))]
      marker.setLatLng(newPos)
      if (polylineLayer) {
        polylineLayer.setLatLngs(currentRoutePoints.value.map(p => [p[1], p[0]]))
      }
    })

    dragMarker.on('dragend', () => {
      scheduleRevalidate()
    })

    dragMarker.on('contextmenu', (e: L.LeafletMouseEvent) => {
      L.DomEvent.stopPropagation(e)
      currentRoutePoints.value.splice(i, 1)
      renderRoute()
      scheduleRevalidate()
      ElMessage.warning('已删除该航路点')
    })

    waypointMarkers.push(dragMarker)
  }
}

// 防抖 500ms 触发合规校验复查
const scheduleRevalidate = () => {
  if (revalidateTimer) clearTimeout(revalidateTimer)
  revalidateTimer = setTimeout(() => {
    executeRevalidation()
  }, 500)
}

const executeRevalidation = async () => {
  if (currentRoutePoints.value.length < 2) return
  validating.value = true
  try {
    const res: any = await validateAirspaceRoute({
      routePoints: currentRoutePoints.value,
      altPlanned: dispatchForm.altPlanned
    })
    hasViolation.value = !res.pass
    if (res.pass) {
      routeStatusMsg.value = '航线校验合格，无空域冲突'
      violationDetail.value = ''
    } else {
      routeStatusMsg.value = res.message || '存在空域冲突'
      violationDetail.value = res.message
      ElMessage.warning(`航线空域复核未通过: ${res.message}`)
    }
    if (polylineLayer) {
      polylineLayer.setStyle({
        color: hasViolation.value ? '#ef4444' : '#0284c7',
        dashArray: hasViolation.value ? '6,6' : undefined
      })
    }
  } catch (err: any) {
    hasViolation.value = true
    violationDetail.value = err.message || '校验请求失败'
  } finally {
    validating.value = false
  }
}

const formatLineString = (pts: [number, number][]) => {
  const coordStrs = pts.map(p => `${p[0].toFixed(6)} ${p[1].toFixed(6)}`).join(', ')
  return `LINESTRING(${coordStrs})`
}



const submitDispatch = async () => {
  if (!dispatchForm.uavId) return ElMessage.warning('请选择指派无人机')
  if (!dispatchForm.pilotId) return ElMessage.warning('请选择执飞机长')
  if (hasViolation.value) return ElMessage.error('当前航线存在空域冲突，不可提交！请拖拽微调折线绕开禁飞区')
  if (currentRoutePoints.value.length < 2) return ElMessage.warning('规划航线点数不足')

  dispatchForm.routeLine = formatLineString(currentRoutePoints.value)
  dispatching.value = true
  try {
    await dispatchOrder(dispatchForm)
    ElMessageBox.alert(
      `运单 [${selectedOrder.value?.orderSn}] 飞行任务申请已成功提交！\n任务编号已生成（处于待空管合规审批，未锁定资源），订单保留在【待调度】状态。\n空管审批通过后将正式确权时段、锁定资源并推进订单至【待执飞】。`,
      '提交飞行任务审批成功',
      { type: 'success', confirmButtonText: '确定 / 继续调度' }
    )
    selectedOrder.value = null
    recommendData.value = null
    currentRoutePoints.value = []
    loadPendingOrders()
  } catch (err: any) {
    ElMessage.error(err.message || '提交飞行任务审批失败')
  } finally {
    dispatching.value = false
  }
}

const goToBoard = () => { router.push('/order/board') }

onMounted(() => {
  nextTick(() => {
    initMap()
    loadPendingOrders()
  })
})
</script>


<style scoped>
.dispatch-workbench-container { display: flex; flex-direction: column; height: calc(100vh - 105px); gap: 10px; }
.workbench-header { display: flex; justify-content: space-between; align-items: center; background: #ffffff; padding: 10px 16px; border-radius: 8px; border: 1px solid #e2e8f0; }
.header-left { display: flex; align-items: center; gap: 10px; }
.workbench-title { font-size: 16px; font-weight: 700; color: #0f172a; display: flex; align-items: center; gap: 6px; }
.sub-text { font-size: 12px; color: #64748b; margin-left: 8px; }
.workbench-body { display: flex; flex: 1; gap: 10px; min-height: 0; }
.left-panel { width: 300px; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; overflow: hidden; }
.panel-head { padding: 12px 14px; font-weight: 700; color: #0f172a; border-bottom: 1px solid #f1f5f9; background: #f8fafc; font-size: 14px; }
.order-list-scroll { flex: 1; overflow-y: auto; padding: 10px; display: flex; flex-direction: column; gap: 10px; }
.order-item-card { border: 1px solid #e2e8f0; border-radius: 6px; padding: 10px; cursor: pointer; transition: all 0.2s; background: #fff; }
.order-item-card:hover { border-color: #0284c7; background: #f0f9ff; }
.order-item-card.active { border-color: #0284c7; background: #e0f2fe; box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.2); }
.order-card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.order-card-top .sn { font-weight: 700; font-size: 13px; color: #0f172a; }
.goods-meta { font-size: 12px; color: #334155; margin-bottom: 4px; }
.addr-meta { font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.4; }
.text-green { color: #10b981; }
.text-orange { color: #f59e0b; }
.middle-map-panel { flex: 1; display: flex; flex-direction: column; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; position: relative; }
.map-toolbar { padding: 8px 12px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; background: #f8fafc; flex-wrap: wrap; gap: 8px; }
.toolbar-left { display: flex; align-items: center; }
.map-hint { font-size: 12px; color: #64748b; font-weight: 500; }
.dispatch-map-view { flex: 1; width: 100%; min-height: 300px; background: #e2e8f0; }
.map-status-overlay { position: absolute; bottom: 12px; left: 12px; z-index: 1000; background: rgba(15, 23, 42, 0.85); color: #fff; font-size: 12px; padding: 6px 12px; border-radius: 4px; display: flex; align-items: center; gap: 6px; backdrop-filter: blur(4px); }
.map-status-overlay.error-overlay { background: rgba(239, 68, 68, 0.9); }
.right-panel { width: 340px; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; overflow-y: auto; }
.recommend-content { padding: 12px; display: flex; flex-direction: column; gap: 10px; }
.summary-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 10px; font-size: 12px; }
.summary-row { display: flex; justify-content: space-between; color: #475569; }
.text-danger { color: #ef4444; }
.snap-box { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 6px; padding: 10px; }
.snap-title { font-size: 12px; font-weight: 700; color: #166534; margin-bottom: 6px; display: flex; align-items: center; gap: 4px; }
.snap-item { display: flex; align-items: center; gap: 6px; font-size: 12px; margin-bottom: 4px; }
.tag-badge { font-size: 10px; color: #fff; padding: 1px 5px; border-radius: 3px; font-weight: bold; }
.bg-green { background: #10b981; }
.bg-orange { background: #f59e0b; }
.site-name { font-weight: 600; color: #0f172a; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.site-dist { font-size: 11px; color: #64748b; }
.snap-tip { font-size: 10px; color: #15803d; margin-top: 4px; }
.snap-error { font-size: 12px; padding: 4px 0; }
.compliance-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 10px; font-size: 12px; }
.compliance-box.pass { background: #f0fdf4; border-color: #86efac; }
.compliance-box.fail { background: #fef2f2; border-color: #fca5a5; }
.comp-head { display: flex; justify-content: space-between; align-items: center; font-weight: 600; color: #0f172a; }
.comp-detail { font-size: 11px; margin-top: 6px; line-height: 1.3; }
.rec-uav-box { background: #fffbeb; border: 1px solid #fde68a; border-radius: 6px; padding: 10px; }
.rec-title { display: flex; justify-content: space-between; align-items: center; font-size: 12px; font-weight: 700; color: #92400e; margin-bottom: 6px; }
.uav-name { font-weight: 700; font-size: 13px; color: #1e293b; margin-bottom: 4px; }
.uav-metrics { font-size: 11px; color: #475569; display: flex; gap: 10px; margin-bottom: 6px; }
.rec-reason { font-size: 11px; color: #d97706; line-height: 1.3; }
.empty-box { padding: 40px 0; display: flex; justify-content: center; }
</style>

