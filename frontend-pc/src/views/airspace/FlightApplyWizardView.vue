<template>
  <div class="apply-wizard-container">
    <el-card class="wizard-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon><Guide /></el-icon>
            <span>低空飞行计划在线申报与智能避障向导</span>
          </div>
          <el-tag type="primary" effect="dark">航线安全毫秒级守护</el-tag>
        </div>
      </template>

      <!-- 分步向导指示条 -->
      <el-steps :active="currentStep" finish-status="success" align-center class="step-bar">
        <el-step title="步骤 1：申报参数设定" description="机型/机长/时段/高度" />
        <el-step title="步骤 2：航线规划与即时碰撞检测" description="交互绘制/避障纠偏" />
        <el-step title="步骤 3：合规总览与正式提交" description="审核入库/锁定空域" />
      </el-steps>

      <!-- 步骤 1：基础参数表单 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-form :model="form" label-width="130px" class="form-box">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="执飞无人机" required>
                <el-select v-model="form.uavId" placeholder="请选择无人机" style="width: 100%">
                  <el-option :value="1" label="UAV-FY90-001 (顺丰丰舟-90 / 载重20kg)" />
                  <el-option :value="2" label="UAV-FY90-002 (顺丰丰舟-90 / 载重20kg)" />
                  <el-option :value="3" label="UAV-FC30-001 (大疆运载FC30 / 载重30kg)" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="执飞机长" required>
                <el-select v-model="form.pilotId" placeholder="请选择持证机长" style="width: 100%">
                  <el-option :value="1" label="张志强 (CAAC-UAV-20210988)" />
                  <el-option :value="2" label="李维保 (MAINT-CERT-2022011)" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="计划飞行时段" required>
                <el-date-picker
                  v-model="timeRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="起飞时间"
                  end-placeholder="降落时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="巡航真高(米)" required>
                <el-input-number v-model="form.altPlanned" :min="30" :max="300" :step="10" />
                <span class="tip-text">（法定低空真高上限：120米）</span>
              </el-form-item>
            </el-col>
          </el-row>

          <div class="step-btn-box">
            <el-button type="primary" @click="nextToMapStep">
              下一步：规划航线与碰撞检测 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </el-form>
      </div>

      <!-- 步骤 2：航线交互绘制与碰撞检测 -->
      <div v-show="currentStep === 1" class="step-content">
        <div class="map-action-bar">
          <div class="sample-btn-group">
            <span class="label">快捷用例：</span>
            <el-button size="small" type="danger" plain @click="loadForbiddenRoute">
              <el-icon><Warning /></el-icon> 穿宝安机场禁区航线（触发拦截）
            </el-button>
            <el-button size="small" type="success" plain @click="loadSafeRoute">
              <el-icon><Check /></el-icon> 合规航线（深圳湾->福田）
            </el-button>
          </div>
          <div class="check-btn-group">
            <el-button type="primary" :loading="checking" @click="handleValidate">
              <el-icon><Aim /></el-icon> 毫秒级围栏碰撞检测
            </el-button>
            <el-button
              v-if="validationResult && !validationResult.pass && validationResult.suggestedRoute"
              type="success"
              @click="applySuggestedRoute"
            >
              <el-icon><RefreshRight /></el-icon> 一键采纳避障绕行建议
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="validationResult"
          :title="validationResult.pass ? '空间碰撞检测通过' : '空间碰撞告警拦截'"
          :type="validationResult.pass ? 'success' : 'error'"
          :description="validationResult.message"
          show-icon
          style="margin-bottom: 12px"
        />

        <div id="leaflet-wizard-map" class="wizard-map-view"></div>

        <div class="step-btn-box" style="margin-top: 16px">
          <el-button @click="currentStep = 0">上一步</el-button>
          <el-button type="primary" :disabled="!validationResult || !validationResult.pass" @click="currentStep = 2">
            下一步：提交申报核验 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 步骤 3：正式申报确认 -->
      <div v-show="currentStep === 2" class="step-content">
        <div class="confirm-summary-box">
          <el-descriptions title="飞行计划申报单详情核验" :column="2" border>
            <el-descriptions-item label="申报商户">深圳市丰翼低空物流科技有限公司</el-descriptions-item>
            <el-descriptions-item label="执飞机号">{{ form.uavId === 1 ? 'UAV-FY90-001' : (form.uavId === 2 ? 'UAV-FY90-002' : 'UAV-FC30-001') }}</el-descriptions-item>
            <el-descriptions-item label="执飞机长">{{ form.pilotId === 1 ? '张志强 (CAAC认证)' : '李维保' }}</el-descriptions-item>
            <el-descriptions-item label="计划巡航高度">{{ form.altPlanned }} 米</el-descriptions-item>
            <el-descriptions-item label="围栏安全检测">
              <el-tag type="success">100% 净空合规放行</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="航线航路点数">{{ routePoints.length }} 个经纬度拐点</el-descriptions-item>
          </el-descriptions>

          <div class="submit-action-box" style="margin-top: 24px; text-align: center;">
            <el-button @click="currentStep = 1">返回修改航线</el-button>
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmitApply">
              正式确认提交空管审批
            </el-button>
          </div>
        </div>
      </div>

    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getAirspaceBlocks, validateAirspaceRoute, createFlightApply } from '@/api/airspace'
import { ElMessage } from 'element-plus'

const router = useRouter()
const currentStep = ref(0)
const checking = ref(false)
const submitting = ref(false)

const form = ref({
  uavId: 1,
  pilotId: 1,
  altPlanned: 90
})

const timeRange = ref<[Date, Date]>([
  new Date(Date.now() + 3600 * 1000),
  new Date(Date.now() + 7200 * 1000)
])

const routePoints = ref<number[][]>([
  [113.943, 22.540],
  [114.000, 22.545],
  [114.062, 22.553]
])

const validationResult = ref<any>(null)
const blocks = ref<any[]>([])

let mapInstance: L.Map | null = null
let airspaceLayerGroup: L.LayerGroup | null = null
let routePolyline: L.Polyline | null = null
let waypointMarkers: L.Layer[] = []

const initWizardMap = () => {
  const mapEl = document.getElementById('leaflet-wizard-map')
  if (!mapEl || mapInstance) return

  mapInstance = L.map(mapEl, {
    center: [22.60, 114.00],
    zoom: 11,
    zoomControl: false
  })

  L.control.zoom({ position: 'bottomright' }).addTo(mapInstance)

  // 接入高德地图 AutoNavi 免 Key 极速切片服务（国内毫秒级秒开，全中文路网）
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    minZoom: 3,
    attribution: '© 高德地图 AutoNavi · 翔云低空航线申报向导'
  }).addTo(mapInstance)

  airspaceLayerGroup = L.layerGroup().addTo(mapInstance)
  drawAirspaceBlocks()
  redrawRoute()
}

const drawAirspaceBlocks = () => {
  if (!airspaceLayerGroup) return
  airspaceLayerGroup.clearLayers()

  blocks.value.forEach(block => {
    if (!block.coordinates || block.coordinates.length < 3) return
    const latLngs = block.coordinates.map((pt: number[]) => [pt[1], pt[0]])

    const polygon = L.polygon(latLngs, {
      color: block.color || '#ef4444',
      fillColor: block.color || '#ef4444',
      fillOpacity: 0.3,
      weight: 2
    })
    polygon.bindPopup(`<b>${block.blockName}</b><br/>${block.blockKindDesc} (限高: ${block.limitAlt ? block.limitAlt + 'm' : '禁飞'})`)
    airspaceLayerGroup!.addLayer(polygon)
  })
}

const redrawRoute = () => {
  if (!mapInstance) return

  if (routePolyline) {
    mapInstance.removeLayer(routePolyline)
    routePolyline = null
  }
  waypointMarkers.forEach(m => mapInstance?.removeLayer(m))
  waypointMarkers = []

  if (routePoints.value.length < 2) return

  const latLngs = routePoints.value.map(pt => [pt[1], pt[0]] as [number, number])
  const isConflict = validationResult.value && !validationResult.value.pass

  routePolyline = L.polyline(latLngs, {
    color: isConflict ? '#ef4444' : '#10b981',
    weight: 4,
    dashArray: isConflict ? '6, 6' : undefined
  }).addTo(mapInstance)

  latLngs.forEach((coord, idx) => {
    const isStart = idx === 0
    const isEnd = idx === latLngs.length - 1
    const marker = L.circleMarker(coord, {
      radius: isStart || isEnd ? 7 : 5,
      color: isStart ? '#3b82f6' : (isEnd ? '#8b5cf6' : '#10b981'),
      fillColor: '#ffffff',
      fillOpacity: 1,
      weight: 3
    }).addTo(mapInstance!)
    marker.bindTooltip(isStart ? '起飞点' : (isEnd ? '降落点' : `航路点 ${idx}`))
    waypointMarkers.push(marker)
  })

  mapInstance.fitBounds(L.latLngBounds(latLngs).pad(0.2))
}

const nextToMapStep = async () => {
  currentStep.value = 1
  await nextTick()
  initWizardMap()
  handleValidate()
}

const loadForbiddenRoute = () => {
  // 穿越宝安机场禁飞区
  routePoints.value = [
    [113.75, 22.63],
    [113.88, 22.63]
  ]
  validationResult.value = null
  redrawRoute()
  handleValidate()
}

const loadSafeRoute = () => {
  // 深圳湾科技生态园 -> 福田深业上城
  routePoints.value = [
    [113.943, 22.540],
    [114.000, 22.545],
    [114.062, 22.553]
  ]
  validationResult.value = null
  redrawRoute()
  handleValidate()
}

const handleValidate = async () => {
  checking.value = true
  try {
    const res = await validateAirspaceRoute({
      routePoints: routePoints.value,
      altPlanned: form.value.altPlanned,
      startAt: timeRange.value[0]?.toISOString(),
      endAt: timeRange.value[1]?.toISOString()
    })
    validationResult.value = res.data
    redrawRoute()
  } catch (error: any) {
    ElMessage.error(error.message || '碰撞校验失败')
  } finally {
    checking.value = false
  }
}

const applySuggestedRoute = () => {
  if (validationResult.value && validationResult.value.suggestedRoute) {
    routePoints.value = validationResult.value.suggestedRoute
    if (validationResult.value.suggestedAlt) {
      form.value.altPlanned = validationResult.value.suggestedAlt
    }
    ElMessage.success('已采纳智能避障绕行航线建议！')
    handleValidate()
  }
}

const handleSubmitApply = async () => {
  submitting.value = true
  try {
    const res = await createFlightApply({
      uavId: form.value.uavId,
      pilotId: form.value.pilotId,
      altPlanned: form.value.altPlanned,
      startAt: timeRange.value[0]?.toISOString(),
      endAt: timeRange.value[1]?.toISOString(),
      routePoints: routePoints.value
    })
    ElMessage.success(`飞行计划申报提交成功！单号: ${res.data?.applySn}`)
    router.push('/airspace/approval')
  } catch (error: any) {
    ElMessage.error(error.message || '提交申报失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getAirspaceBlocks()
    blocks.value = res.data || []
  } catch (error) {}
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})
</script>

<style scoped>
.apply-wizard-container {
  width: 100%;
  min-height: calc(100vh - 100px);
}

.wizard-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.step-bar {
  margin: 20px 0 30px;
}

.step-content {
  padding: 10px 20px 20px;
}

.form-box {
  max-width: 800px;
  margin: 0 auto;
}

.tip-text {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 10px;
}

.step-btn-box {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.map-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.sample-btn-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sample-btn-group .label {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

.check-btn-group {
  display: flex;
  gap: 10px;
}

.wizard-map-view {
  width: 100%;
  height: 480px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.confirm-summary-box {
  max-width: 850px;
  margin: 0 auto;
}
</style>


