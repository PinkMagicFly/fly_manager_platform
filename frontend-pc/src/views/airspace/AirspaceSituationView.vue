<template>
  <div class="airspace-situation-container">
    <div id="leaflet-airspace-map" class="map-view"></div>

    <!-- 顶部悬浮态势总览浮窗 -->
    <div class="map-overlay-panel">
      <div class="overlay-title">
        <el-icon><Compass /></el-icon>
        <span>深圳低空空域三区分区管制态势图</span>
      </div>
      <div class="overlay-stats">
        <div class="stat-badge">
          <span class="num text-red">{{ forbiddenCount }}</span>
          <span class="lbl">禁飞区</span>
        </div>
        <div class="stat-badge">
          <span class="num text-orange">{{ limitedCount }}</span>
          <span class="lbl">限飞管制区</span>
        </div>
        <div class="stat-badge">
          <span class="num text-yellow">{{ tempCount }}</span>
          <span class="lbl">临时管控区</span>
        </div>
        <div class="stat-badge">
          <span class="num text-blue">{{ flyingUavs.length }}</span>
          <span class="lbl">在飞无人机</span>
        </div>
        <div class="stat-badge">
          <span class="num text-purple">{{ sites.length }}</span>
          <span class="lbl">起降场站</span>
        </div>
      </div>
    </div>

    <!-- 右上角图层勾选与管理控制面板 -->
    <div class="layer-control-panel">
      <div class="panel-header">
        <span><el-icon><Filter /></el-icon> 空域图层与底图控制</span>
      </div>
      <div class="map-type-selector">
        <el-radio-group v-model="mapType" size="small" @change="switchBaseMap" style="margin-bottom: 10px; width: 100%;">
          <el-radio-button value="vector">高德标准路网</el-radio-button>
          <el-radio-button value="satellite">高德高清卫星</el-radio-button>
        </el-radio-group>
      </div>
      <div class="layer-options">
        <el-checkbox v-model="filterForbidden" @change="renderAirspacePolygons">
          <span class="legend-box bg-red"></span> 永久禁飞区 (宝安净空)
        </el-checkbox>
        <el-checkbox v-model="filterLimited" @change="renderAirspacePolygons">
          <span class="legend-box bg-orange"></span> 动态限飞区 (南山总部)
        </el-checkbox>
        <el-checkbox v-model="filterTemp" @change="renderAirspacePolygons">
          <span class="legend-box bg-yellow"></span> 临时管制区 (大运中心)
        </el-checkbox>
        <div class="divider-line"></div>
        <el-checkbox v-model="filterSites" @change="renderSitesLayer">
          <span class="legend-box bg-purple"></span> 起降场站枢纽 ({{ sites.length }})
        </el-checkbox>
        <el-checkbox v-model="filterFlying" @change="renderFlyingLayer">
          <span class="legend-box bg-blue"></span> 在飞无人机 ({{ flyingUavs.length }})
        </el-checkbox>
        <el-checkbox v-model="filterRoutes" @change="renderApprovedRoutesLayer">
          <span class="legend-box bg-green"></span> 近24h已批航线 ({{ approvedRoutes.length }})
        </el-checkbox>
      </div>
      <div class="panel-actions">
        <el-button size="small" type="primary" @click="drawerVisible = true">
          <el-icon><Tickets /></el-icon> 空域网格台账
        </el-button>
        <el-button size="small" type="warning" @click="ruleDialogVisible = true">
          <el-icon><Setting /></el-icon> 电子围栏规则
        </el-button>
      </div>
    </div>

    <!-- 空域区块列表抽屉 -->
    <el-drawer v-model="drawerVisible" title="低空空域管制网格管理台账" size="480px">
      <div class="drawer-header-btn" v-if="isAtcOrAdmin">
        <el-button type="primary" size="small" @click="openCreateDialog">
          <el-icon><Plus /></el-icon> 划设新管制区块
        </el-button>
      </div>
      <el-table :data="blocks" border size="small" style="margin-top: 12px">
        <el-table-column prop="blockId" label="ID" width="50" align="center" />
        <el-table-column prop="blockName" label="区块名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="类别" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getTagType(row.blockKind)">{{ row.blockKindDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="限高" width="70" align="center">
          <template #default="{ row }">
            {{ row.limitAlt ? row.limitAlt + 'm' : '禁飞' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" v-if="isAtcOrAdmin">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDeleteBlock(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 电子围栏合规校验规则弹窗 -->
    <el-dialog v-model="ruleDialogVisible" title="法定电子围栏与合规校验规则字典" width="600px">
      <el-table :data="fenceRules" border size="small">
        <el-table-column prop="ruleId" label="ID" width="50" align="center" />
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="bufferDistance" label="安全缓冲带" width="90" align="center">
          <template #default="{ row }">{{ row.bufferDistance }}米</template>
        </el-table-column>
        <el-table-column prop="maxAltitude" label="法定真高上限" width="100" align="center">
          <template #default="{ row }">{{ row.maxAltitude }}米</template>
        </el-table-column>
        <el-table-column label="触发动作" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.actionPolicy === 3 ? 'danger' : 'warning'">
              {{ row.actionPolicy === 3 ? '阻断申报' : (row.actionPolicy === 2 ? '限高通过' : '预警提示') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 划设空域区块弹窗 -->
    <el-dialog v-model="createDialogVisible" title="划设新空域管制区块" width="500px">
      <el-form :model="createForm" label-width="110px" size="small">
        <el-form-item label="区块名称" required>
          <el-input v-model="createForm.blockName" placeholder="如：福田中心区临时管制区" />
        </el-form-item>
        <el-form-item label="管制类别" required>
          <el-select v-model="createForm.blockKind" style="width: 100%">
            <el-option :value="1" label="永久禁飞区 (高度0米)" />
            <el-option :value="2" label="动态限飞区 (限制高度)" />
            <el-option :value="3" label="临时管制区 (时效性)" />
          </el-select>
        </el-form-item>
        <el-form-item label="限高上限(米)">
          <el-input-number v-model="createForm.limitAlt" :min="0" :max="500" />
        </el-form-item>
        <el-form-item label="多边形WKT坐标" required>
          <el-input
            v-model="createForm.geoPolygon"
            type="textarea"
            :rows="3"
            placeholder="POLYGON((114.05 22.54, 114.08 22.54, 114.08 22.57, 114.05 22.57, 114.05 22.54))"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveBlock">确认划设保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getAirspaceBlocks, getAirspaceMapData, deleteAirspaceBlock, saveAirspaceBlock, getFenceRules } from '@/api/airspace'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.roles.includes('ROLE_ADMIN'))
const isAtc = computed(() => userStore.userInfo?.roles.includes('ROLE_ATC') || userStore.userInfo?.accountKind === 4)
const isAtcOrAdmin = computed(() => isAdmin.value || isAtc.value)

const blocks = ref<any[]>([])
const sites = ref<any[]>([])
const flyingUavs = ref<any[]>([])
const approvedRoutes = ref<any[]>([])
const fenceRules = ref<any[]>([])

const filterForbidden = ref(true)
const filterLimited = ref(true)
const filterTemp = ref(true)
const filterSites = ref(true)
const filterFlying = ref(true)
const filterRoutes = ref(true)

const drawerVisible = ref(false)
const ruleDialogVisible = ref(false)
const createDialogVisible = ref(false)

const createForm = ref({
  blockName: '',
  blockKind: 1,
  limitAlt: 0,
  geoPolygon: 'POLYGON((114.05 22.54, 114.08 22.54, 114.08 22.57, 114.05 22.57, 114.05 22.54))'
})

const mapType = ref<'vector' | 'satellite'>('vector')
let mapInstance: L.Map | null = null
let polygonLayerGroup: L.LayerGroup | null = null
let siteLayerGroup: L.LayerGroup | null = null
let flyingLayerGroup: L.LayerGroup | null = null
let routeLayerGroup: L.LayerGroup | null = null
const flyingMarkerMap = new Map<number, L.Marker>()
let pollTimer: any = null
let vectorLayer: L.TileLayer | null = null
let satelliteLayer: L.LayerGroup | null = null

const forbiddenCount = computed(() => blocks.value.filter(b => b.blockKind === 1).length)
const limitedCount = computed(() => blocks.value.filter(b => b.blockKind === 2).length)
const tempCount = computed(() => blocks.value.filter(b => b.blockKind === 3).length)

const getTagType = (kind: number) => {
  if (kind === 1) return 'danger'
  if (kind === 2) return 'warning'
  return 'info'
}

const initMap = () => {
  const mapEl = document.getElementById('leaflet-airspace-map')
  if (!mapEl) return

  mapInstance = L.map(mapEl, {
    center: [22.60, 114.02],
    zoom: 11,
    zoomControl: false
  })

  L.control.zoom({ position: 'bottomright' }).addTo(mapInstance)

  // 1. 高德中文矢量路网切片层
  vectorLayer = L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    minZoom: 3,
    attribution: '© 高德地图 AutoNavi · 翔云低空空域态势中枢'
  })

  // 2. 高德高清卫星影像 + 路网标注层
  const satTiles = L.tileLayer('https://webst0{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    minZoom: 3
  })
  const satLabels = L.tileLayer('https://wprd0{s}.is.autonavi.com/appmaptile?x={x}&y={y}&z={z}&lang=zh_cn&size=1&scl=1&style=8', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    minZoom: 3,
    attribution: '© 高德卫星影像 AutoNavi · 翔云低空空域态势中枢'
  })
  satelliteLayer = L.layerGroup([satTiles, satLabels])

  vectorLayer.addTo(mapInstance)
  polygonLayerGroup = L.layerGroup().addTo(mapInstance)
  siteLayerGroup = L.layerGroup().addTo(mapInstance)
  routeLayerGroup = L.layerGroup().addTo(mapInstance)
  flyingLayerGroup = L.layerGroup().addTo(mapInstance)
}

const switchBaseMap = () => {
  if (!mapInstance) return
  if (mapType.value === 'vector') {
    if (satelliteLayer && mapInstance.hasLayer(satelliteLayer)) {
      mapInstance.removeLayer(satelliteLayer)
    }
    if (vectorLayer && !mapInstance.hasLayer(vectorLayer)) {
      vectorLayer.addTo(mapInstance)
    }
  } else {
    if (vectorLayer && mapInstance.hasLayer(vectorLayer)) {
      mapInstance.removeLayer(vectorLayer)
    }
    if (satelliteLayer && !mapInstance.hasLayer(satelliteLayer)) {
      satelliteLayer.addTo(mapInstance)
    }
  }
}

const renderAirspacePolygons = () => {
  if (!mapInstance || !polygonLayerGroup) return
  polygonLayerGroup.clearLayers()

  const allBounds: L.LatLngBounds[] = []

  blocks.value.forEach(block => {
    if (block.blockKind === 1 && !filterForbidden.value) return
    if (block.blockKind === 2 && !filterLimited.value) return
    if (block.blockKind === 3 && !filterTemp.value) return

    if (!block.coordinates || block.coordinates.length < 3) return

    const latLngs = block.coordinates.map((pt: number[]) => [pt[1], pt[0]])

    const polygon = L.polygon(latLngs, {
      color: block.color || '#ef4444',
      fillColor: block.color || '#ef4444',
      fillOpacity: block.fillOpacity || 0.35,
      weight: 2
    })

    const popupContent = `
      <div style="font-size:12px; line-height:1.6; min-width:180px;">
        <b style="color:${block.color}; font-size:13px;">${block.blockName}</b><br/>
        <b>管制类别:</b> ${block.blockKindDesc}<br/>
        <b>限高上限:</b> ${block.limitAlt ? block.limitAlt + ' 米' : '禁止一切民用飞行'}<br/>
        <b>数据来源:</b> ${block.source || '空管综合局'}<br/>
        ${block.effectiveAt ? `<b>生效时段:</b> ${block.effectiveAt?.substring(0, 10)} 至 ${block.expireAt?.substring(0, 10)}` : ''}
      </div>
    `
    polygon.bindPopup(popupContent)

    // 添加永久居中高亮名称与限高气泡标牌，一目了然
    const badgeText = block.blockKind === 1
      ? `🔴 ${block.blockName} (永久禁飞)`
      : (block.blockKind === 2
          ? `🟠 ${block.blockName} (限高${block.limitAlt}m)`
          : `🟡 ${block.blockName} (赛事临时管制${block.limitAlt}m)`)

    polygon.bindTooltip(badgeText, {
      permanent: true,
      direction: 'center',
      className: 'airspace-zone-tooltip'
    })

    polygonLayerGroup!.addLayer(polygon)
    allBounds.push(polygon.getBounds())
  })

  // 如果有区块，自动平滑缩放至深圳市三区最佳视野
  if (allBounds.length > 0 && mapInstance) {
    let combinedBounds = allBounds[0]
    for (let i = 1; i < allBounds.length; i++) {
      combinedBounds.extend(allBounds[i])
    }
    mapInstance.fitBounds(combinedBounds.pad(0.15))
  }
}
const renderSitesLayer = () => {
  if (!mapInstance || !siteLayerGroup) return
  siteLayerGroup.clearLayers()
  if (!filterSites.value) return

  sites.value.forEach((site: any) => {
    let lat = site.lat
    let lng = site.lng
    if ((!lat || !lng) && site.siteGeo) {
      const match = site.siteGeo.match(/POINT\(([\d.]+)\s+([\d.]+)\)/)
      if (match) {
        lng = parseFloat(match[1])
        lat = parseFloat(match[2])
      }
    }
    if (!lat || !lng) return

    const circle = L.circle([lat, lng], {
      radius: 5000,
      color: '#8b5cf6',
      fillColor: '#8b5cf6',
      fillOpacity: 0.08,
      weight: 1,
      dashArray: '4, 4'
    })
    siteLayerGroup!.addLayer(circle)

    const siteIcon = L.divIcon({
      className: 'site-custom-marker',
      html: `<div class="site-badge">🏢 ${site.siteName}</div>`,
      iconSize: [120, 24],
      iconAnchor: [60, 12]
    })

    const marker = L.marker([lat, lng], { icon: siteIcon })
    const popupHtml = `
      <div style="font-size:12px; line-height:1.6; min-width:180px;">
        <b style="color:#7c3aed; font-size:13px;">📍 ${site.siteName}</b><br/>
        <b>场站编号:</b> ${site.siteCode}<br/>
        <b>总机位:</b> ${site.capacity || 4} · <b>已占用:</b> ${site.occupied || 0}<br/>
        <b>服务半径:</b> 5.0 公里覆盖圈<br/>
        <b>运行状态:</b> <span style="color:#10b981;">正常运营</span>
      </div>
    `
    marker.bindPopup(popupHtml)
    siteLayerGroup!.addLayer(marker)
  })
}

const renderApprovedRoutesLayer = () => {
  if (!mapInstance || !routeLayerGroup) return
  routeLayerGroup.clearLayers()
  if (!filterRoutes.value) return

  approvedRoutes.value.forEach((route: any) => {
    if (!route.routePoints || route.routePoints.length < 2) return
    const latLngs = route.routePoints.map((p: number[]) => [p[1], p[0]])

    const polyline = L.polyline(latLngs, {
      color: '#0ea5e9',
      weight: 3,
      opacity: 0.8,
      dashArray: '6, 6'
    })

    const tooltipContent = `
      <div style="font-size:11px;">
        <b>申报任务:</b> ${route.applySn}<br/>
        <b>执飞机号:</b> ${route.uavSn || '-'}<br/>
        <b>机长:</b> ${route.pilotName || '-'}<br/>
        <b>巡航高度:</b> ${route.altPlanned}m
      </div>
    `
    polyline.bindTooltip(tooltipContent, { sticky: true })
    routeLayerGroup!.addLayer(polyline)
  })
}

const renderFlyingLayer = () => {
  if (!mapInstance || !flyingLayerGroup) return

  if (!filterFlying.value) {
    flyingLayerGroup.clearLayers()
    flyingMarkerMap.clear()
    return
  }

  const currentUavIds = new Set<number>()

  flyingUavs.value.forEach((uav: any) => {
    if (uav.lat == null || uav.lng == null) return
    const uavId = uav.uavId
    currentUavIds.add(uavId)

    const uavPos: [number, number] = [uav.lat, uav.lng]
    const popupHtml = `
      <div style="font-size:12px; line-height:1.6; min-width:180px;">
        <b style="color:#0284c7; font-size:13px;">🛸 执飞无人机实时态势</b><br/>
        <b>任务编号:</b> ${uav.applySn || '-'}<br/>
        <b>执飞机号:</b> ${uav.uavSn || '-'} (${uav.model || '-' })<br/>
        <b>机长飞手:</b> ${uav.pilotName || '-'}<br/>
        <b>瞬时高度:</b> ${uav.alt != null ? uav.alt + ' 米' : '-'}<br/>
        <b>水平地速:</b> ${uav.speed != null ? uav.speed + ' m/s' : '-'}<br/>
        <b>剩余电量:</b> ${uav.batteryRemain != null ? uav.batteryRemain + '%' : '-'}<br/>
        <b>任务时段:</b> ${uav.startAt ? uav.startAt.substring(11, 16) : '-'} ~ ${uav.endAt ? uav.endAt.substring(11, 16) : '-'}<br/>
        <b>任务状态:</b> <span style="color:#10b981; font-weight:bold;">${uav.approveStateDesc || '执飞中'}</span>
      </div>
    `

    const existingMarker = flyingMarkerMap.get(uavId)
    if (existingMarker) {
      existingMarker.setLatLng(uavPos)
      existingMarker.setPopupContent(popupHtml)
    } else {
      const uavIcon = L.divIcon({
        className: 'custom-uav-marker',
        html: `<div class="uav-pin-pulse">✈️</div>`,
        iconSize: [28, 28],
        iconAnchor: [14, 14]
      })
      const marker = L.marker(uavPos, { icon: uavIcon }).bindPopup(popupHtml)
      flyingLayerGroup!.addLayer(marker)
      flyingMarkerMap.set(uavId, marker)
    }
  })

  // 降落清除
  for (const [id, marker] of flyingMarkerMap.entries()) {
    if (!currentUavIds.has(id)) {
      flyingLayerGroup.removeLayer(marker)
      flyingMarkerMap.delete(id)
    }
  }
}


const loadData = async () => {
  try {
    const res: any = await getAirspaceMapData()
    const data = res?.data || res
    blocks.value = data?.blocks || []
    sites.value = data?.sites || []
    flyingUavs.value = data?.flyingUavs || []
    approvedRoutes.value = data?.approvedRoutes || []

    renderAirspacePolygons()
    renderSitesLayer()
    renderApprovedRoutesLayer()
    renderFlyingLayer()

    const ruleRes: any = await getFenceRules()
    fenceRules.value = Array.isArray(ruleRes) ? ruleRes : (ruleRes?.data || [])
  } catch (error) {
    ElMessage.error('加载空域管制数据失败')
  }
}

const pollMapData = async () => {
  try {
    const res: any = await getAirspaceMapData()
    const data = res?.data || res
    if (data?.flyingUavs) {
      flyingUavs.value = data.flyingUavs
      renderFlyingLayer()
    }
    if (data?.approvedRoutes) {
      approvedRoutes.value = data.approvedRoutes
      if (filterRoutes.value) renderApprovedRoutesLayer()
    }
  } catch (e) {
    console.warn('轮询空域态势数据失败:', e)
  }
}

const openCreateDialog = () => {
  createForm.value = {
    blockName: '',
    blockKind: 1,
    limitAlt: 0,
    geoPolygon: 'POLYGON((114.05 22.54, 114.08 22.54, 114.08 22.57, 114.05 22.57, 114.05 22.54))'
  }
  createDialogVisible.value = true
}

const handleSaveBlock = async () => {
  if (!createForm.value.blockName) {
    ElMessage.warning('请输入区块名称')
    return
  }
  try {
    await saveAirspaceBlock(createForm.value)
    ElMessage.success('空域区块已成功划设并生效')
    createDialogVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const handleDeleteBlock = (row: any) => {
  ElMessageBox.confirm(`确定要移除空域管制区块【${row.blockName}】吗？`, '警告', {
    type: 'warning'
  }).then(async () => {
    await deleteAirspaceBlock(row.blockId)
    ElMessage.success('区块已删除')
    await loadData()
  })
}

onMounted(() => {
  initMap()
  loadData()
  pollTimer = setInterval(pollMapData, 5000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})
</script>

<style scoped>
.airspace-situation-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 100px);
  border-radius: 8px;
  overflow: hidden;
}

.map-view {
  width: 100%;
  height: 100%;
  z-index: 1;
}

.map-overlay-panel {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px 16px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.overlay-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}

.overlay-stats {
  display: flex;
  gap: 16px;
}

.stat-badge {
  display: flex;
  flex-direction: column;
}

.stat-badge .num {
  font-size: 18px;
  font-weight: 700;
}

.stat-badge .lbl {
  font-size: 11px;
  color: #94a3b8;
}

.layer-control-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 12px 16px;
  color: #fff;
  width: 260px;
}

.panel-header {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #e2e8f0;
}

.layer-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.legend-box {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: middle;
}

.bg-red { background: #ef4444; }
.bg-orange { background: #f59e0b; }
.bg-yellow { background: #eab308; }

.text-red { color: #ef4444; }
.text-orange { color: #f59e0b; }
.text-yellow { color: #eab308; }

.panel-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.drawer-header-btn {
  display: flex;
  justify-content: flex-end;
}

:deep(.airspace-zone-tooltip) {
  background: rgba(15, 23, 42, 0.90) !important;
  color: #ffffff !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  border-radius: 4px !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  padding: 3px 8px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4) !important;
  pointer-events: none;
}

.divider-line {
  height: 1px;
  background: rgba(255, 255, 255, 0.12);
  margin: 6px 0;
}
.bg-purple { background: #8b5cf6; }
.bg-blue { background: #0284c7; }
.bg-green { background: #10b981; }

.text-blue { color: #38bdf8; }
.text-purple { color: #a78bfa; }

:deep(.site-badge) {
  background: rgba(139, 92, 246, 0.85);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  text-align: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
  white-space: nowrap;
}

:deep(.uav-pin-pulse) {
  font-size: 18px;
  line-height: 28px;
  text-align: center;
  background: rgba(2, 132, 199, 0.25);
  border-radius: 50%;
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.8);
  animation: float 2s ease-in-out infinite alternate;
}
@keyframes float {
  from { transform: translateY(0); }
  to { transform: translateY(-4px); }
}


</style>


