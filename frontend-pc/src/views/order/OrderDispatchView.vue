<template>
  <div class="dispatch-workbench-container">
    <div class="workbench-header">
      <div class="header-left">
        <span class="workbench-title"><el-icon><Compass /></el-icon> 智能调度中枢工作台</span>
        <el-tag type="primary" effect="dark" size="small">实时提交飞行任务审批</el-tag>
        <span class="sub-text">高德底座地图 / 排他性算法匹配</span>
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

      <!-- 中间：高德地图 -->
      <div class="middle-map-panel">
        <div class="map-toolbar">
          <el-radio-group v-model="mapMode" size="small" @change="switchMapLayer">
            <el-radio-button label="vector">高德高精路网</el-radio-button>
            <el-radio-button label="satellite">高清遥感卫星</el-radio-button>
          </el-radio-group>
          <span v-if="selectedOrder" class="map-hint">当前选中: {{ selectedOrder.orderSn }} ({{ selectedOrder.goodsWeight }}kg)</span>
          <span v-else class="map-hint">请从左侧选择运单进行航线规划与资源指派</span>
        </div>
        <div id="dispatch-map" class="dispatch-map-view"></div>
      </div>

      <!-- 右栏：决策面板 -->
      <div class="right-panel" v-loading="loadingRecommend">
        <div class="panel-head"><span>智能调度方案与任务申报</span></div>
        <div v-if="!selectedOrder" class="empty-box"><el-empty description="请选择待调度运单" :image-size="80" /></div>
        <div v-else class="recommend-content">
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
              <el-input-number v-model="dispatchForm.altPlanned" :min="60" :max="150" :step="10" style="width: 100%" />
            </el-form-item>

            <el-button type="primary" size="large" :loading="dispatching" style="width: 100%; margin-top: 10px" @click="submitDispatch">
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
import { getOrderPage, getRecommendDispatch, dispatchOrder } from '@/api/order'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const router = useRouter()

const loadingOrders = ref(false)
const loadingRecommend = ref(false)
const dispatching = ref(false)

const pendingOrders = ref<any[]>([])
const selectedOrder = ref<any>(null)
const recommendData = ref<any>(null)

const mapMode = ref('vector')
let mapInstance: L.Map | null = null
let currentTileLayer: L.TileLayer | null = null
let routeLayerGroup: L.LayerGroup | null = null

const dispatchForm = reactive({
  orderId: undefined as number | undefined,
  uavId: undefined as number | undefined,
  pilotId: undefined as number | undefined,
  altPlanned: 110
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

  routeLayerGroup = L.layerGroup().addTo(mapInstance)
  switchMapLayer(mapMode.value)
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

  drawOrderRoute(order)
}

const drawOrderRoute = (order: any) => {
  if (!mapInstance || !routeLayerGroup) return
  routeLayerGroup.clearLayers()

  if (order.senderLat && order.senderLng && order.receiverLat && order.receiverLng) {
    const start: [number, number] = [order.senderLat, order.senderLng]
    const end: [number, number] = [order.receiverLat, order.receiverLng]

    L.circleMarker(start, {
      radius: 9, fillColor: '#10b981', color: '#fff', weight: 2, fillOpacity: 1
    }).addTo(routeLayerGroup).bindPopup(`<b>起点</b>: ${order.senderAddr}`)

    L.circleMarker(end, {
      radius: 9, fillColor: '#f59e0b', color: '#fff', weight: 2, fillOpacity: 1
    }).addTo(routeLayerGroup).bindPopup(`<b>终点</b>: ${order.receiverAddr}`)

    const line = L.polyline([start, end], {
      color: '#0284c7', weight: 4, dashArray: '6, 6', opacity: 0.9
    }).addTo(routeLayerGroup)

    mapInstance.fitBounds(line.getBounds(), { padding: [50, 50] })
  }
}

const submitDispatch = async () => {
  if (!dispatchForm.uavId) return ElMessage.warning('请选择指派无人机')
  if (!dispatchForm.pilotId) return ElMessage.warning('请选择执飞机长')

  dispatching.value = true
  try {
    const res: any = await dispatchOrder(dispatchForm)
    ElMessageBox.alert(
      `运单 [${selectedOrder.value?.orderSn}] 飞行任务申请已成功提交！\n任务编号已生成（处于待空管合规审批，未锁定资源），订单保留在【待调度】状态。\n空管审批通过后将正式确权时段、锁定资源并推进订单至【待执飞】。`,
      '提交飞行任务审批成功',
      { type: 'success', confirmButtonText: '确定 / 继续调度' }
    )
    selectedOrder.value = null
    recommendData.value = null
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
.middle-map-panel { flex: 1; display: flex; flex-direction: column; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; }
.map-toolbar { padding: 8px 12px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; background: #f8fafc; }
.map-hint { font-size: 12px; color: #64748b; font-weight: 500; }
.dispatch-map-view { flex: 1; width: 100%; min-height: 300px; background: #e2e8f0; }
.right-panel { width: 330px; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; overflow-y: auto; }
.recommend-content { padding: 12px; display: flex; flex-direction: column; gap: 12px; }
.summary-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 10px; font-size: 12px; }
.summary-row { display: flex; justify-content: space-between; color: #475569; }
.text-danger { color: #ef4444; }
.rec-uav-box { background: #fffbeb; border: 1px solid #fde68a; border-radius: 6px; padding: 10px; }
.rec-title { display: flex; justify-content: space-between; align-items: center; font-size: 12px; font-weight: 700; color: #92400e; margin-bottom: 6px; }
.uav-name { font-weight: 700; font-size: 13px; color: #1e293b; margin-bottom: 4px; }
.uav-metrics { font-size: 11px; color: #475569; display: flex; gap: 10px; margin-bottom: 6px; }
.rec-reason { font-size: 11px; color: #d97706; line-height: 1.3; }
.empty-box { padding: 40px 0; display: flex; justify-content: center; }
</style>
