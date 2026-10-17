<template>
  <div class="approval-view-container">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon><Stamp /></el-icon>
            <span>低空飞行计划申报空管审批工作台</span>
          </div>
          <div class="header-filters">
            <el-radio-group v-model="filterState" size="small" @change="loadData">
              <el-radio-button :value="undefined">全部申报</el-radio-button>
              <el-radio-button :value="0">待审批队列</el-radio-button>
              <el-radio-button :value="1">已批准</el-radio-button>
              <el-radio-button :value="2">已驳回</el-radio-button>
              <el-radio-button :value="4">执行中</el-radio-button>
              <el-radio-button :value="5">已完成</el-radio-button>
              <el-radio-button :value="3">已取消</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <!-- 申报单列表表格 -->
      <el-table :data="applyList" border stripe v-loading="loading">
        <el-table-column prop="applySn" label="申报编号" width="160" align="center" />
        <el-table-column prop="merchantName" label="申报商户" min-width="160" show-overflow-tooltip />
        <el-table-column label="执飞装备" min-width="170">
          <template #default="{ row }">
            <b>{{ row.uavSn }}</b><br/>
            <span style="font-size: 11px; color: #64748b">{{ row.uavModel }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pilotName" label="责任机长" width="100" align="center" />
        <el-table-column label="计划高度" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.altPlanned }} 米</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="计划起降时段" min-width="160">
          <template #default="{ row }">
            <span style="font-size: 11px">{{ row.startAt?.replace('T', ' ') }}<br/>至 {{ row.endAt?.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="围栏预校验" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.checkState === 1 ? 'success' : 'danger'">
              {{ row.checkStateDesc || (row.checkState === 1 ? '净空通过' : '告警') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTag(row.approveState)">
              {{ row.approveStateDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openSimulationDialog(row)">
              <el-icon><View /></el-icon> 空间推演
            </el-button>
            <template v-if="row.approveState === 0 && isAtcOrAdmin">
              <el-button type="success" link size="small" @click="handleQuickApprove(row, 1)">
                通过
              </el-button>
              <el-button type="danger" link size="small" @click="openRejectDialog(row)">
                驳回
              </el-button>
            </template>
            <el-button v-if="row.approveState === 0" type="info" link size="small" @click="handleCancel(row)">
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 空间推演与航线冲突核查弹窗 -->
    <el-dialog v-model="simDialogVisible" :title="'飞行计划时空推演核查 - ' + currentApply?.applySn" width="800px" @opened="initSimMap">
      <div v-if="currentApply" class="sim-container">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="空管时空推演系统：航线与宝安机场净空禁飞区、南山高新科技园限飞区、大运中心临时管制区叠合无冲突"
          style="margin-bottom: 12px"
        />
        <div id="leaflet-sim-map" class="sim-map-box"></div>
      </div>
      <template #footer>
        <el-button @click="simDialogVisible = false">关闭推演</el-button>
        <el-button v-if="currentApply?.approveState === 0 && isAtcOrAdmin" type="success" @click="handleQuickApprove(currentApply, 1)">
          核准通过并锁定空域时段
        </el-button>
      </template>
    </el-dialog>

    <!-- 驳回说明弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="空管合规审批驳回决策" width="460px">
      <el-form label-position="top">
        <el-form-item label="驳回原因或修改建议（必填）" required>
          <el-input
            v-model="rejectNote"
            type="textarea"
            :rows="3"
            placeholder="请输入详细驳回理由，如：该时段周边有重要民航航道保障任务，建议顺延起飞时间或降低高度至80米以下"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getFlightApplyList, approveFlightApply, cancelFlightApply, getAirspaceBlocks } from '@/api/airspace'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.roles.includes('ROLE_ADMIN'))
const isAtc = computed(() => userStore.userInfo?.roles.includes('ROLE_ATC') || userStore.userInfo?.accountKind === 4)
const isAtcOrAdmin = computed(() => isAdmin.value || isAtc.value)

const applyList = ref<any[]>([])
const loading = ref(false)
const filterState = ref<number | undefined>(undefined)

const simDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentApply = ref<any>(null)
const rejectNote = ref('')
const blocks = ref<any[]>([])

let simMap: L.Map | null = null

const getStatusTag = (state: number) => {
  if (state === 0) return 'warning'
  if (state === 1) return 'primary'
  if (state === 2) return 'danger'
  if (state === 3) return 'info'
  if (state === 4) return 'warning'
  if (state === 5) return 'success'
  return 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getFlightApplyList({ approveState: filterState.value })
    applyList.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    ElMessage.error('加载申报列表失败')
  } finally {
    loading.value = false
  }
}

const openSimulationDialog = async (row: any) => {
  currentApply.value = row
  simDialogVisible.value = true
}

const initSimMap = () => {
  const mapEl = document.getElementById('leaflet-sim-map')
  if (!mapEl) return

  if (simMap) {
    simMap.remove()
    simMap = null
  }

  simMap = L.map(mapEl, {
    center: [22.58, 114.02],
    zoom: 11,
    zoomControl: false
  })

  L.control.zoom({ position: 'bottomright' }).addTo(simMap)

  // 接入高德地图 AutoNavi 免 Key 极速切片服务（国内毫秒级秒开，全中文路网）
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
    minZoom: 3,
    attribution: '© 高德地图 AutoNavi · 翔云低空审批空间推演'
  }).addTo(simMap)

  // 绘制空域区块
  blocks.value.forEach(block => {
    if (!block.coordinates || block.coordinates.length < 3) return
    const latLngs = block.coordinates.map((pt: number[]) => [pt[1], pt[0]])
    L.polygon(latLngs, {
      color: block.color || '#ef4444',
      fillColor: block.color || '#ef4444',
      fillOpacity: 0.25,
      weight: 1
    }).addTo(simMap!).bindTooltip(block.blockName)
  })

  // 绘制推演航线
  if (currentApply.value && currentApply.value.routePoints && currentApply.value.routePoints.length >= 2) {
    const routeLatLngs = currentApply.value.routePoints.map((pt: number[]) => [pt[1], pt[0]] as [number, number])

    const polyline = L.polyline(routeLatLngs, {
      color: '#0284c7',
      weight: 5
    }).addTo(simMap)

    routeLatLngs.forEach((coord: [number, number], idx: number) => {
      L.circleMarker(coord, {
        radius: idx === 0 || idx === routeLatLngs.length - 1 ? 6 : 4,
        color: idx === 0 ? '#3b82f6' : (idx === routeLatLngs.length - 1 ? '#8b5cf6' : '#10b981'),
        fillColor: '#fff',
        fillOpacity: 1,
        weight: 2
      }).addTo(simMap!).bindTooltip(idx === 0 ? '起飞场站' : (idx === routeLatLngs.length - 1 ? '降落场站' : `航路点 ${idx}`))
    })

    simMap.fitBounds(polyline.getBounds().pad(0.3))
  }
}

const handleQuickApprove = (row: any, state: number) => {
  ElMessageBox.confirm(
    `确认批准飞行计划【${row.applySn}】吗？\n审批通过将执行空域时段确权、排他性锁定无人机与场站机位，并推进订单状态至【待执飞】。`,
    '空管审批核准确认',
    {
      type: 'success',
      confirmButtonText: '核准通过并锁定资源'
    }
  ).then(async () => {
    await approveFlightApply({
      applyId: row.applyId,
      approveState: state,
      approveNote: '空管合规审核通过，空域时段已确权，准予起飞执飞'
    })
    ElMessage.success('审批决策已下达并存证，单据状态变更为【已批准】，资源已正式锁定！')
    simDialogVisible.value = false
    await loadData()
  })
}

const openRejectDialog = (row: any) => {
  currentApply.value = row
  rejectNote.value = ''
  rejectDialogVisible.value = true
}

const submitReject = async () => {
  if (!rejectNote.value || !rejectNote.value.trim()) {
    ElMessage.error('驳回原因必填，请详细说明驳回理由！')
    return
  }
  await approveFlightApply({
    applyId: currentApply.value.applyId,
    approveState: 2,
    approveNote: rejectNote.value.trim()
  })
  ElMessage.warning('申报单已驳回，相关资源已解除，订单保留在待调度状态')
  rejectDialogVisible.value = false
  await loadData()
}

const handleCancel = (row: any) => {
  ElMessageBox.confirm(`确定要撤销飞行计划申报【${row.applySn}】吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await cancelFlightApply(row.applyId)
    ElMessage.success('申报已撤销')
    await loadData()
  })
}

onMounted(async () => {
  loadData()
  try {
    const res: any = await getAirspaceBlocks()
    blocks.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {}
})

onUnmounted(() => {
  if (simMap) {
    simMap.remove()
    simMap = null
  }
})
</script>

<style scoped>
.approval-view-container {
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

.sim-map-box {
  width: 100%;
  height: 420px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
}
</style>

