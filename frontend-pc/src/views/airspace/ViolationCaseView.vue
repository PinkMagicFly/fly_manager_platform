<template>
  <div class="violation-view-container">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon><WarningFilled /></el-icon>
            <span>低空飞行违规调查与行政执法处置中心</span>
          </div>
          <div class="header-filters">
            <el-select v-model="filterKind" placeholder="违规行为类别" clearable size="small" style="width: 160px" @change="loadData">
              <el-option :value="1" label="航线严重偏航" />
              <el-option :value="2" label="非法入侵禁飞区" />
              <el-option :value="3" label="超越法定限高" />
              <el-option :value="4" label="超时占用空域" />
            </el-select>
            <el-select v-model="filterState" placeholder="处置状态" clearable size="small" style="width: 130px; margin-left: 10px" @change="loadData">
              <el-option :value="0" label="待核查" />
              <el-option :value="1" label="核查中" />
              <el-option :value="2" label="已结案下达文书" />
            </el-select>
          </div>
        </div>
      </template>

      <!-- 违规案件台账表格 -->
      <el-table :data="caseList" border stripe v-loading="loading">
        <el-table-column prop="caseSn" label="案件流水号" width="160" align="center" />
        <el-table-column prop="applySn" label="关联申报单" width="160" align="center" />
        <el-table-column prop="merchantName" label="涉事企业" min-width="160" show-overflow-tooltip />
        <el-table-column prop="uavSn" label="执飞装备" width="140" align="center" />
        <el-table-column prop="pilotName" label="责任机长" width="100" align="center" />
        <el-table-column label="违规行为类别" width="130" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="danger" effect="dark">
              {{ row.violateKindDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处罚分级" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getLevelTag(row.level)">
              {{ row.levelDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处置状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.punishState === 2 ? 'success' : 'warning'">
              {{ row.punishStateDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetailDialog(row)">
              <el-icon><Document /></el-icon> 案件详情与轨迹
            </el-button>
            <el-button v-if="row.punishState !== 2" type="danger" link size="small" @click="openPunishDialog(row)">
              行政处罚
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 案件详情与偏航对比弹窗 -->
    <el-dialog v-model="detailDialogVisible" :title="'违规案件调查详情 - ' + caseDetail?.caseSn" width="850px" @opened="initDetailMap">
      <div v-if="caseDetail" class="detail-container">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="涉事企业">{{ caseDetail.merchantName }}</el-descriptions-item>
          <el-descriptions-item label="执飞机号">{{ caseDetail.uavSn }}</el-descriptions-item>
          <el-descriptions-item label="责任机长">{{ caseDetail.pilotName }}</el-descriptions-item>
          <el-descriptions-item label="违规行为">{{ caseDetail.violateKindDesc }}</el-descriptions-item>
          <el-descriptions-item label="最大偏离距离">
            <span class="text-red font-bold">{{ caseDetail.maxDeviationMeters }} 米</span>（预警红线: 50米）
          </el-descriptions-item>
          <el-descriptions-item label="处置状态">{{ caseDetail.punishStateDesc }}</el-descriptions-item>
        </el-descriptions>

        <!-- 偏航轨迹与计划航线对比地图 -->
        <div class="map-section-title">
          <span><el-icon><Compass /></el-icon> 计划申报航线 (绿实线) 与 雷达偏航轨迹 (红虚线) 空间对比</span>
        </div>
        <div id="leaflet-violation-map" class="violation-map-box"></div>

        <!-- 处罚决定书文书正文 -->
        <div v-if="caseDetail.punishDocText" class="punish-doc-box">
          <div class="doc-title">民用无人机行政处罚决定书存证</div>
          <pre class="doc-content">{{ caseDetail.punishDocText }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="caseDetail?.punishState !== 2" type="danger" @click="openPunishDialog(caseDetail)">
          下达行政处罚通知书
        </el-button>
      </template>
    </el-dialog>

    <!-- 下达行政处罚通知书弹窗 -->
    <el-dialog v-model="punishDialogVisible" title="下达行政处罚决定文书" width="500px">
      <el-form :model="punishForm" label-width="110px" size="small">
        <el-form-item label="处罚分级裁量" required>
          <el-select v-model="punishForm.level" style="width: 100%">
            <el-option :value="1" label="1级：警告留痕（诚信档案记过）" />
            <el-option :value="2" label="2级：企业罚款（罚款5000元）" />
            <el-option :value="3" label="3级：停飞整顿（暂停航线15天）" />
            <el-option :value="4" label="4级：吊销资质（吊销运营资质）" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置决定说明" required>
          <el-input
            v-model="punishForm.punishDesc"
            type="textarea"
            :rows="3"
            placeholder="根据雷达在途监测偏航达128米，责令涉事企业进行机组整改，并处以行政处罚。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="punishDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitPunish">确认送达并结案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getViolationCaseList, getViolationCaseDetail, handleViolationCase } from '@/api/airspace'
import { ElMessage } from 'element-plus'

const caseList = ref<any[]>([])
const loading = ref(false)
const filterKind = ref<number | undefined>(undefined)
const filterState = ref<number | undefined>(undefined)

const detailDialogVisible = ref(false)
const punishDialogVisible = ref(false)
const caseDetail = ref<any>(null)
const selectedCase = ref<any>(null)

const punishForm = ref({
  caseId: 0,
  level: 1,
  punishDesc: '经雷达比对偏航超过预警红线，责令整改留痕。'
})

let detailMap: L.Map | null = null

const getLevelTag = (level: number) => {
  if (level === 1) return 'info'
  if (level === 2) return 'warning'
  if (level === 3) return 'danger'
  return 'danger'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getViolationCaseList({
      violateKind: filterKind.value,
      punishState: filterState.value
    })
    caseList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载违规案件列表失败')
  } finally {
    loading.value = false
  }
}

const openDetailDialog = async (row: any) => {
  selectedCase.value = row
  try {
    const res = await getViolationCaseDetail(row.caseId)
    caseDetail.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取案件详情失败')
  }
}

const initDetailMap = () => {
  const mapEl = document.getElementById('leaflet-violation-map')
  if (!mapEl) return

  if (detailMap) {
    detailMap.remove()
    detailMap = null
  }

  detailMap = L.map(mapEl, {
    center: [22.545, 114.00],
    zoom: 12,
    zoomControl: false
  })

  L.control.zoom({ position: 'bottomright' }).addTo(detailMap)

  // 接入高德地图 AutoNavi 免 Key 极速切片服务（国内毫秒级秒开，全中文路网）
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    minZoom: 3,
    attribution: '© 高德地图 AutoNavi · 翔云低空违规轨迹核查'
  }).addTo(detailMap)

  if (caseDetail.value) {
    // 1. 计划航线 (绿线实线)
    if (caseDetail.value.plannedRoute && caseDetail.value.plannedRoute.length >= 2) {
      const plannedLatLngs = caseDetail.value.plannedRoute.map((pt: number[]) => [pt[1], pt[0]] as [number, number])
      L.polyline(plannedLatLngs, {
        color: '#10b981',
        weight: 4
      }).addTo(detailMap!).bindTooltip('法定批准规划航线')
    }

    // 2. 实际偏航轨迹 (红虚线)
    if (caseDetail.value.actualRoute && caseDetail.value.actualRoute.length >= 2) {
      const actualLatLngs = caseDetail.value.actualRoute.map((pt: number[]) => [pt[1], pt[0]] as [number, number])
      const actualLine = L.polyline(actualLatLngs, {
        color: '#ef4444',
        weight: 4,
        dashArray: '6, 6'
      }).addTo(detailMap!).bindTooltip('实际偏航雷达轨迹 (最大偏离 128 米)')

      detailMap.fitBounds(actualLine.getBounds().pad(0.3))
    }
  }
}

const openPunishDialog = (row: any) => {
  selectedCase.value = row
  punishForm.value = {
    caseId: row.caseId,
    level: row.level || 1,
    punishDesc: '根据雷达在途监测偏航达128米，责令涉事企业进行机组整改，并处以行政处罚。'
  }
  punishDialogVisible.value = true
}

const submitPunish = async () => {
  try {
    await handleViolationCase(punishForm.value)
    ElMessage.success('行政处罚决定书已生成并归档送达！')
    punishDialogVisible.value = false
    detailDialogVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '处罚下达失败')
  }
}

onMounted(() => {
  loadData()
})

onUnmounted(() => {
  if (detailMap) {
    detailMap.remove()
    detailMap = null
  }
})
</script>

<style scoped>
.violation-view-container {
  width: 100%;
}

.table-card {
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

.text-red { color: #ef4444; }
.font-bold { font-weight: 700; }

.map-section-title {
  margin: 16px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 6px;
}

.violation-map-box {
  width: 100%;
  height: 320px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
}

.punish-doc-box {
  margin-top: 16px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  padding: 12px 16px;
}

.doc-title {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  font-size: 13px;
}

.doc-content {
  font-family: monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #475569;
  white-space: pre-wrap;
  margin: 0;
}
</style>
