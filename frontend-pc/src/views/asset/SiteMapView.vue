<template>
  <div class="site-map-container">
    <!-- 地图容器 -->
    <div id="leaflet-site-map" class="map-view"></div>

    <!-- 顶部悬浮统计浮窗 -->
    <div class="map-overlay-panel">
      <div class="overlay-title">
        <el-icon><Location /></el-icon>
        <span>深圳低空起降枢纽一张图</span>
      </div>
      <div class="overlay-stats">
        <div class="stat-badge">
          <span class="num">{{ sites.length }}</span>
          <span class="lbl">运营场站</span>
        </div>
        <div class="stat-badge">
          <span class="num text-green">{{ totalCapacity }}</span>
          <span class="lbl">总机位数</span>
        </div>
        <div class="stat-badge">
          <span class="num text-orange">{{ totalOccupied }}</span>
          <span class="lbl">已占机位</span>
        </div>
      </div>
    </div>

    <!-- 右侧场站详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="activeSite?.siteName || '场站机位详情'" size="420px">
      <div v-if="activeSite" class="drawer-content">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="场站编码">{{ activeSite.siteCode }}</el-descriptions-item>
          <el-descriptions-item label="运营状态">
            <el-tag :type="activeSite.siteState === 1 ? 'success' : 'danger'" effect="dark">
              {{ activeSite.siteStateDesc }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="经纬度坐标">
            {{ activeSite.longitude }}, {{ activeSite.latitude }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 机位占用环形进度 -->
        <div class="capacity-gauge-box">
          <el-progress
            type="dashboard"
            :percentage="Math.round((activeSite.occupied / activeSite.capacity) * 100)"
            :color="gaugeColor"
            :width="120"
          >
            <template #default="{ percentage }">
              <span class="gauge-value">{{ activeSite.occupied }} / {{ activeSite.capacity }}</span>
              <span class="gauge-label">机位占用率</span>
            </template>
          </el-progress>
          <div class="gauge-summary">
            <div>空闲可用机位：<b class="text-green">{{ activeSite.available }}</b> 个</div>
            <div>智能换电机柜：备电 <b>{{ activeSite.batteryStock }}</b> 组 (充电中 <b>{{ activeSite.chargingCount }}</b>)</div>
          </div>
        </div>

        <!-- 冲突防冲测试按钮 -->
        <div class="action-bar">
          <el-button type="primary" style="width: 100%" @click="openConflictDialog">
            <el-icon><Timer /></el-icon> 时空防冲预约测试
          </el-button>
        </div>

        <!-- 近期预约列表 -->
        <div class="booking-section">
          <h4>近期起降预约时段</h4>
          <el-table :data="recentBookings" size="small" border>
            <el-table-column prop="bookingId" label="单号" width="60" align="center" />
            <el-table-column label="预约起止时段" min-width="160">
              <template #default="{ row }">
                <span style="font-size: 11px">{{ row.startTime?.replace('T', ' ') }}<br/>至 {{ row.endTime?.replace('T', ' ') }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="75" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.bookingState === 1 ? 'primary' : 'success'">
                  {{ row.bookingState === 1 ? '已预约' : '已占用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>

    <!-- 时空防冲预约与冲突检测弹窗 -->
    <el-dialog v-model="conflictDialogVisible" title="场站机位占用时空防冲预约" width="480px">
      <el-form label-position="top">
        <el-form-item label="目标场站">
          <el-input :value="activeSite?.siteName + ' (总容积: ' + activeSite?.capacity + ' 机位)'" disabled />
        </el-form-item>
        <el-form-item label="拟预约占用时间段" required>
          <el-date-picker
            v-model="bookingTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 实时冲突校验检测卡片 -->
        <div v-if="checkResult" class="check-result-card" :class="{ 'has-conflict': checkResult.hasConflict }">
          <div class="result-header">
            <el-icon v-if="checkResult.hasConflict" class="icon-danger"><CircleCloseFilled /></el-icon>
            <el-icon v-else class="icon-success"><CircleCheckFilled /></el-icon>
            <b>{{ checkResult.hasConflict ? '时空冲突警告' : '时空校验通过' }}</b>
          </div>
          <p class="result-msg">{{ checkResult.message }}</p>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="conflictDialogVisible = false">取消</el-button>
        <el-button type="warning" plain :loading="checking" @click="handleCheckConflict">时空冲突检测</el-button>
        <el-button type="primary" :disabled="checkResult?.hasConflict" :loading="bookingLoading" @click="handleCreateBooking">
          确认预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { getSiteMap, getSiteBookings, checkBookingConflict, createSiteBooking } from '@/api/asset'

let mapInstance: L.Map | null = null
const sites = ref<any[]>([])
const activeSite = ref<any>(null)
const drawerVisible = ref(false)
const recentBookings = ref<any[]>([])

const conflictDialogVisible = ref(false)
const bookingTimeRange = ref<[string, string] | null>(null)
const checking = ref(false)
const bookingLoading = ref(false)
const checkResult = ref<any>(null)

const totalCapacity = computed(() => sites.value.reduce((acc, cur) => acc + (cur.capacity || 0), 0))
const totalOccupied = computed(() => sites.value.reduce((acc, cur) => acc + (cur.occupied || 0), 0))

const gaugeColor = computed(() => {
  if (!activeSite.value) return '#10b981'
  const pct = (activeSite.value.occupied / activeSite.value.capacity) * 100
  if (pct >= 80) return '#ef4444'
  if (pct >= 50) return '#f59e0b'
  return '#10b981'
})

const initLeafletMap = () => {
  if (mapInstance) return
  // 深圳地理中心
  mapInstance = L.map('leaflet-site-map', {
    center: [22.58, 114.10],
    zoom: 11,
    zoomControl: true
  })

  // 采用 CartoDB 高对比度暗色或高德瓦片
  L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    maxZoom: 18,
    attribution: '© OpenStreetMap contributors, © CARTO'
  }).addTo(mapInstance)
}

const renderSiteMarkers = () => {
  if (!mapInstance) return

  sites.value.forEach((site) => {
    const lat = site.latitude || 22.54
    const lng = site.longitude || 113.94

    // 自定义 Leaflet 图标
    const isFull = site.occupied >= site.capacity
    const markerColor = isFull ? '#ef4444' : '#10b981'

    const customHtml = `
      <div style="background-color: ${markerColor}; width: 28px; height: 28px; border-radius: 50%;
                  display: flex; align-items: center; justify-content: center; color: white; font-weight: bold;
                  font-size: 12px; box-shadow: 0 0 10px rgba(0,0,0,0.3); border: 2px solid #ffffff;">
        ${site.capacity - site.occupied}
      </div>
    `
    const icon = L.divIcon({
      html: customHtml,
      className: 'site-leaflet-marker',
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    })

    const marker = L.marker([lat, lng], { icon }).addTo(mapInstance!)

    const popupHtml = `
      <div style="min-width: 180px; padding: 4px;">
        <h4 style="margin: 0 0 6px 0; color: #0f172a; font-size: 14px;">${site.siteName}</h4>
        <div style="font-size: 12px; color: #64748b; line-height: 1.6;">
          <div>机位总容积：<b>${site.capacity}</b> 机位</div>
          <div>当前空闲：<b style="color: ${markerColor}">${site.available}</b> 机位</div>
          <div>运营状态：${site.siteStateDesc}</div>
        </div>
        <button id="btn-site-${site.siteId}" style="margin-top: 8px; width: 100%; padding: 4px 8px;
                background-color: #0284c7; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">
          查看机位详情与预约
        </button>
      </div>
    `
    marker.bindPopup(popupHtml)

    marker.on('popupopen', () => {
      setTimeout(() => {
        const btn = document.getElementById(`btn-site-${site.siteId}`)
        if (btn) {
          btn.onclick = () => openSiteDrawer(site)
        }
      }, 50)
    })
  })
}

const fetchSiteMapData = async () => {
  try {
    const data = await getSiteMap()
    sites.value = data || []
    renderSiteMarkers()
  } catch (err) {}
}

const openSiteDrawer = async (site: any) => {
  activeSite.value = site
  drawerVisible.value = true
  try {
    const bookings = await getSiteBookings(site.siteId, 8)
    recentBookings.value = bookings || []
  } catch (err) {}
}

const openConflictDialog = () => {
  checkResult.value = null
  const now = new Date()
  const tomorrow = new Date(now.getTime() + 24 * 3600 * 1000)
  const format = (d: Date) => d.toISOString().slice(0, 10)
  bookingTimeRange.value = [`${format(tomorrow)} 09:00:00`, `${format(tomorrow)} 10:00:00`]
  conflictDialogVisible.value = true
}

const handleCheckConflict = async () => {
  if (!bookingTimeRange.value) {
    ElMessage.warning('请选择预约起止时段')
    return
  }
  checking.value = true
  try {
    const res = await checkBookingConflict({
      siteId: activeSite.value.siteId,
      startTime: bookingTimeRange.value[0],
      endTime: bookingTimeRange.value[1]
    })
    checkResult.value = res
  } finally {
    checking.value = false
  }
}

const handleCreateBooking = async () => {
  if (!bookingTimeRange.value) {
    ElMessage.warning('请选择预约起止时段')
    return
  }
  bookingLoading.value = true
  try {
    await createSiteBooking({
      siteId: activeSite.value.siteId,
      uavId: 1,
      startTime: bookingTimeRange.value[0],
      endTime: bookingTimeRange.value[1]
    })
    ElMessage.success('场站机位时空预约成功')
    conflictDialogVisible.value = false
    openSiteDrawer(activeSite.value)
  } finally {
    bookingLoading.value = false
  }
}

onMounted(() => {
  initLeafletMap()
  fetchSiteMapData()
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})
</script>

<style scoped>
.site-map-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 100px);
  overflow: hidden;
  border-radius: 8px;
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
  z-index: 999;
  background-color: rgba(15, 23, 42, 0.9);
  backdrop-filter: blur(8px);
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 12px 16px;
  color: white;
  box-shadow: 0 4px 16px rgba(0,0,0,0.3);
}
.overlay-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: bold;
  color: #38bdf8;
  margin-bottom: 8px;
}
.overlay-stats {
  display: flex;
  gap: 16px;
}
.stat-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-badge .num {
  font-size: 18px;
  font-weight: bold;
}
.stat-badge .lbl {
  font-size: 11px;
  color: #94a3b8;
}
.text-green { color: #10b981; }
.text-orange { color: #f59e0b; }

.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.capacity-gauge-box {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px;
  background-color: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.gauge-value {
  font-size: 14px;
  font-weight: bold;
  color: #0f172a;
}
.gauge-label {
  font-size: 11px;
  color: #64748b;
  display: block;
}
.gauge-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #334155;
}
.action-bar {
  margin-top: 4px;
}
.booking-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #0f172a;
}
.check-result-card {
  margin-top: 12px;
  padding: 12px;
  border-radius: 6px;
  background-color: #f0fdf4;
  border: 1px solid #86efac;
}
.check-result-card.has-conflict {
  background-color: #fef2f2;
  border-color: #fca5a5;
}
.result-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 14px;
}
.icon-success { color: #10b981; }
.icon-danger { color: #ef4444; }
.result-msg {
  margin: 0;
  font-size: 12px;
  color: #475569;
}
</style>


