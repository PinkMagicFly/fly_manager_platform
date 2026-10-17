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
      </div>
    </div>

    <!-- 右上角图层勾选与管理控制面板 -->
    <div class="layer-control-panel">
      <div class="panel-header">
        <span><el-icon><Filter /></el-icon> 空域图层显隐控制</span>
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
      <div class="drawer-header-btn">
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
        <el-table-column label="操作" width="80" align="center">
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
import { getAirspaceBlocks, deleteAirspaceBlock, saveAirspaceBlock, getFenceRules } from '@/api/airspace'
import { ElMessage, ElMessageBox } from 'element-plus'

const blocks = ref<any[]>([])
const fenceRules = ref<any[]>([])
const filterForbidden = ref(true)
const filterLimited = ref(true)
const filterTemp = ref(true)
const drawerVisible = ref(false)
const ruleDialogVisible = ref(false)
const createDialogVisible = ref(false)

const createForm = ref({
  blockName: '',
  blockKind: 1,
  limitAlt: 0,
  geoPolygon: 'POLYGON((114.05 22.54, 114.08 22.54, 114.08 22.57, 114.05 22.57, 114.05 22.54))'
})

let mapInstance: L.Map | null = null
let polygonLayerGroup: L.LayerGroup | null = null

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

  L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    maxZoom: 18,
    attribution: '&copy; CartoDB &copy; SoarCloud'
  }).addTo(mapInstance)

  polygonLayerGroup = L.layerGroup().addTo(mapInstance)
}

const renderAirspacePolygons = () => {
  if (!mapInstance || !polygonLayerGroup) return
  polygonLayerGroup.clearLayers()

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
    polygonLayerGroup!.addLayer(polygon)
  })
}

const loadData = async () => {
  try {
    const res = await getAirspaceBlocks()
    blocks.value = res.data || []
    renderAirspacePolygons()

    const ruleRes = await getFenceRules()
    fenceRules.value = ruleRes.data || []
  } catch (error) {
    ElMessage.error('加载空域管制数据失败')
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
})

onUnmounted(() => {
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
</style>


