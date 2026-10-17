<template>
  <div class="uav-fleet-container">
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-form :inline="true" :model="queryForm" class="filter-form">
          <el-form-item label="设备机型">
            <el-input v-model="queryForm.model" placeholder="输入机型搜索" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item label="设备五态">
            <el-select v-model="queryForm.state" placeholder="全部状态" clearable style="width: 140px">
              <el-option label="闲置 (0)" :value="0" />
              <el-option label="任务中 (1)" :value="1" />
              <el-option label="维保中 (2)" :value="2" />
              <el-option label="故障 (3)" :value="3" />
              <el-option label="离线 (4)" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchList"><el-icon><Search /></el-icon> 查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="action-buttons">
          <el-button-group>
            <el-button :type="viewMode === 'card' ? 'primary' : 'default'" @click="viewMode = 'card'">卡片视图</el-button>
            <el-button :type="viewMode === 'table' ? 'primary' : 'default'" @click="viewMode = 'table'">表格视图</el-button>
          </el-button-group>
          <el-button type="success" @click="openCreateDialog"><el-icon><Plus /></el-icon> 新增无人机</el-button>
          <el-button type="warning" plain @click="openMaintainDrawer"><el-icon><Tools /></el-icon> 维保工单</el-button>
        </div>
      </div>
    </el-card>

    <div class="fleet-content" v-loading="loading">
      <div v-if="viewMode === 'card'" class="card-grid">
        <el-card v-for="uav in uavList" :key="uav.uavId" class="uav-card" :class="'border-state-' + uav.state" shadow="hover">
          <div class="card-header">
            <div>
              <span class="sn-badge">{{ uav.uavSn }}</span>
              <h3 class="model-title">{{ uav.model }}</h3>
            </div>
            <el-tag :type="getStateTag(uav.state)" effect="dark">{{ uav.stateDesc }}</el-tag>
          </div>
          <div class="card-body">
            <div class="indicator-row">
              <div class="health-box">
                <el-progress type="dashboard" :percentage="uav.healthScore" :width="80" :color="getHealthColor(uav.healthScore)" />
                <span class="sub-label">健康评分</span>
              </div>
              <div class="specs-box">
                <div class="spec-row">
                  <span class="label">电池：</span>
                  <div class="bar-wrap"><el-progress :percentage="uav.batteryPct" :stroke-width="8" :color="getBatteryColor(uav.batteryPct)" /></div>
                </div>
                <div class="spec-row"><span class="label">载重：</span><b>{{ uav.maxLoad }} kg</b></div>
                <div class="spec-row"><span class="label">航程：</span><b>{{ uav.maxRangeKm }} km</b></div>
                <div class="spec-row"><span class="label">飞行：</span><b class="text-blue">{{ uav.flyHours }} 小时</b></div>
                <div class="spec-row"><span class="label">场站：</span><span>{{ uav.homeSiteName || '未分配' }}</span></div>
              </div>
            </div>
          </div>
          <div class="card-footer">
            <el-button size="small" type="primary" plain @click="openStateDialog(uav)">状态流转</el-button>
            <el-button size="small" type="danger" plain @click="openFaultDialog(uav)">故障报修</el-button>
            <el-button size="small" @click="openEditDialog(uav)">编辑</el-button>
          </div>
        </el-card>
      </div>

      <el-card v-else class="table-card" shadow="never">
        <el-table :data="uavList" stripe border style="width: 100%">
          <el-table-column prop="uavId" label="ID" width="70" align="center" />
          <el-table-column prop="uavSn" label="设备编号" width="140" align="center" />
          <el-table-column prop="model" label="机型规格" min-width="170" />
          <el-table-column label="五态" width="100" align="center">
            <template #default="{ row }"><el-tag :type="getStateTag(row.state)" effect="dark">{{ row.stateDesc }}</el-tag></template>
          </el-table-column>
          <el-table-column label="健康分" width="120" align="center">
            <template #default="{ row }"><el-progress :percentage="row.healthScore" :color="getHealthColor(row.healthScore)" /></template>
          </el-table-column>
          <el-table-column label="电量" width="110" align="center">
            <template #default="{ row }"><el-progress :percentage="row.batteryPct" :color="getBatteryColor(row.batteryPct)" /></template>
          </el-table-column>
          <el-table-column prop="maxLoad" label="载重(kg)" width="95" align="right" />
          <el-table-column prop="maxRangeKm" label="航程(km)" width="95" align="right" />
          <el-table-column prop="flyHours" label="飞行时长" width="95" align="right">
            <template #default="{ row }"><b>{{ row.flyHours }}h</b></template>
          </el-table-column>
          <el-table-column prop="homeSiteName" label="常驻场站" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="210" fixed="right" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="openStateDialog(row)">流转</el-button>
              <el-button size="small" type="danger" link @click="openFaultDialog(row)">报修</el-button>
              <el-button size="small" type="info" link @click="openEditDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 五态受控流转弹窗 -->
    <el-dialog v-model="stateDialogVisible" title="设备五态模型受控流转" width="460px" destroy-on-close>
      <div v-if="selectedUav">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="领域状态机守卫守护中"
          description="系统严格拦截非法跃迁（维保中或故障设备不可直接跃迁至任务中）。"
          style="margin-bottom: 14px"
        />
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="设备编号">{{ selectedUav.uavSn }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStateTag(selectedUav.state)" effect="dark">
              {{ selectedUav.stateDesc }} (代码: {{ selectedUav.state }})
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top" style="margin-top: 14px">
          <el-form-item label="拟跃迁目标状态" required>
            <el-select v-model="targetState" placeholder="请选择目标状态" style="width: 100%">
              <el-option label="0 - 闲置 (待命就绪)" :value="0" />
              <el-option label="1 - 任务中 (执行货运)" :value="1" />
              <el-option label="2 - 维保中 (巡检维护)" :value="2" />
              <el-option label="3 - 故障 (停运排查)" :value="3" />
              <el-option label="4 - 离线 (信号中断)" :value="4" />
            </el-select>
          </el-form-item>

          <div v-if="isIllegalTransition" class="guard-banner">
            <el-icon><WarningFilled /></el-icon>
            <span>【状态机拦截】维保中或故障设备不可直接跃迁至任务中！</span>
          </div>

          <el-form-item label="流转操作说明">
            <el-input v-model="stateRemark" placeholder="请输入变更原因或任务单号" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="stateDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="isIllegalTransition" :loading="submitLoading" @click="handleConfirmStateChange">
          确认流转
        </el-button>
      </template>
    </el-dialog>

    <!-- 故障报修弹窗 -->
    <el-dialog v-model="faultDialogVisible" title="设备故障现场上报" width="440px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="关联无人机">
          <el-input :value="selectedUav?.uavSn + ' - ' + selectedUav?.model" disabled />
        </el-form-item>
        <el-form-item label="故障现象描述" required>
          <el-input v-model="faultDesc" type="textarea" :rows="3" placeholder="如：旋翼电机异响、电池压差偏高" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faultDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitLoading" @click="handleSubmitFault">提交故障工单</el-button>
      </template>
    </el-dialog>


    <!-- 新增/编辑抽屉 -->
    <el-drawer v-model="editDrawerVisible" :title="isEdit ? '编辑无人机档案' : '登记新增无人机'" size="380px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="设备编号 (SN)" prop="uavSn">
          <el-input v-model="editForm.uavSn" placeholder="如：UAV-FY90-003" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="机型规格" prop="model">
          <el-input v-model="editForm.model" placeholder="如：顺丰丰舟-90" />
        </el-form-item>
        <el-form-item label="额定载重 (kg)" prop="maxLoad">
          <el-input-number v-model="editForm.maxLoad" :precision="2" :min="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标称航程 (km)" prop="maxRangeKm">
          <el-input-number v-model="editForm.maxRangeKm" :precision="1" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="常驻场站">
          <el-select v-model="editForm.homeSiteId" placeholder="选择常驻场站" style="width: 100%">
            <el-option v-for="s in siteOptions" :key="s.siteId" :label="s.siteName" :value="s.siteId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSaveUav">保存</el-button>
      </template>
    </el-drawer>

    <!-- 维保工单与验收抽屉 -->
    <el-drawer v-model="maintainDrawerVisible" title="维保工单与验收台" size="620px">
      <el-table :data="maintainList" border size="small">
        <el-table-column prop="maintainId" label="ID" width="60" align="center" />
        <el-table-column label="类别" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.maintainType === 1 ? 'info' : 'danger'">
              {{ row.maintainType === 1 ? '定检' : '故障' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="faultDesc" label="现象描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.maintainState === 2 ? 'success' : 'warning'">
              {{ row.maintainState === 2 ? '合格' : '待验' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button v-if="row.maintainState !== 2" size="small" type="success" link @click="openAcceptDialog(row)">验收</el-button>
            <span v-else class="text-muted">已合格</span>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 验收弹窗 -->
    <el-dialog v-model="acceptDialogVisible" title="维保检修验收合格" width="400px">
      <el-form label-position="top">
        <el-form-item label="检修处理与更换配件记录" required>
          <el-input v-model="repairContent" type="textarea" :rows="2" placeholder="如：更换电机电调，自检正常" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitLoading" @click="handleConfirmAccept">验收并放行</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getUavList,
  changeUavState,
  reportFault,
  createUav,
  updateUav,
  getMaintainList,
  acceptMaintain,
  getSiteList
} from '@/api/asset'

const loading = ref(false)
const submitLoading = ref(false)
const viewMode = ref<'card' | 'table'>('card')
const uavList = ref<any[]>([])
const siteOptions = ref<any[]>([])

const queryForm = reactive({
  model: '',
  state: undefined as number | undefined
})

const stateDialogVisible = ref(false)
const selectedUav = ref<any>(null)
const targetState = ref<number | undefined>(undefined)
const stateRemark = ref('')

const isIllegalTransition = computed(() => {
  if (!selectedUav.value || targetState.value === undefined) return false
  const cur = selectedUav.value.state
  const tar = targetState.value
  return (cur === 2 || cur === 3) && tar === 1
})

const faultDialogVisible = ref(false)
const faultDesc = ref('')
const editDrawerVisible = ref(false)
const isEdit = ref(false)
const editFormRef = ref()
const editForm = reactive({
  uavId: undefined as number | undefined,
  uavSn: '',
  model: '',
  merchantId: 1,
  maxLoad: 20.0,
  maxRangeKm: 50.0,
  homeSiteId: undefined as number | undefined
})
const editRules = {
  uavSn: [{ required: true, message: '设备SN不可为空', trigger: 'blur' }],
  model: [{ required: true, message: '机型不可为空', trigger: 'blur' }]
}

const maintainDrawerVisible = ref(false)
const maintainList = ref<any[]>([])
const acceptDialogVisible = ref(false)
const currentMaintainRow = ref<any>(null)
const repairContent = ref('')

const fetchList = async () => {
  loading.value = true
  try {
    const data = await getUavList({
      model: queryForm.model || undefined,
      state: queryForm.state !== undefined ? queryForm.state : undefined
    })
    uavList.value = data || []
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryForm.model = ''
  queryForm.state = undefined
  fetchList()
}

const fetchSites = async () => {
  try {
    const data = await getSiteList()
    siteOptions.value = data || []
  } catch (err) {}
}

const getStateTag = (state: number) => {
  switch (state) {
    case 0: return 'success'
    case 1: return 'primary'
    case 2: return 'warning'
    case 3: return 'danger'
    default: return 'info'
  }
}

const getHealthColor = (score: number) => {
  if (score >= 90) return '#10b981'
  if (score >= 80) return '#f59e0b'
  return '#ef4444'
}

const getBatteryColor = (pct: number) => {
  if (pct >= 50) return '#10b981'
  if (pct >= 20) return '#eab308'
  return '#ef4444'
}

const openStateDialog = (uav: any) => {
  selectedUav.value = uav
  targetState.value = undefined
  stateRemark.value = ''
  stateDialogVisible.value = true
}

const handleConfirmStateChange = async () => {
  if (targetState.value === undefined) {
    ElMessage.warning('请选择目标状态')
    return
  }
  if (isIllegalTransition.value) {
    ElMessage.error('【守卫拦截】维保中或故障设备不可直接跃迁至任务中！')
    return
  }
  submitLoading.value = true
  try {
    await changeUavState(selectedUav.value.uavId, {
      targetState: targetState.value,
      remark: stateRemark.value
    })
    ElMessage.success('设备五态流转成功')
    stateDialogVisible.value = false
    fetchList()
  } finally {
    submitLoading.value = false
  }
}

const openFaultDialog = (uav: any) => {
  selectedUav.value = uav
  faultDesc.value = ''
  faultDialogVisible.value = true
}

const handleSubmitFault = async () => {
  if (!faultDesc.value) {
    ElMessage.warning('请输入故障描述')
    return
  }
  submitLoading.value = true
  try {
    await reportFault({
      uavId: selectedUav.value.uavId,
      faultDesc: faultDesc.value
    })
    ElMessage.success('故障上报成功，设备已切换为故障待修')
    faultDialogVisible.value = false
    fetchList()
  } finally {
    submitLoading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  editForm.uavId = undefined
  editForm.uavSn = ''
  editForm.model = ''
  editForm.maxLoad = 20.0
  editForm.maxRangeKm = 50.0
  editForm.homeSiteId = siteOptions.value.length > 0 ? siteOptions.value[0].siteId : undefined
  editDrawerVisible.value = true
}

const openEditDialog = (uav: any) => {
  isEdit.value = true
  editForm.uavId = uav.uavId
  editForm.uavSn = uav.uavSn
  editForm.model = uav.model
  editForm.maxLoad = Number(uav.maxLoad)
  editForm.maxRangeKm = Number(uav.maxRangeKm)
  editForm.homeSiteId = uav.homeSiteId
  editDrawerVisible.value = true
}

const handleSaveUav = async () => {
  editFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value && editForm.uavId) {
        await updateUav(editForm.uavId, editForm)
        ElMessage.success('档案修订成功')
      } else {
        await createUav(editForm)
        ElMessage.success('新增设备成功')
      }
      editDrawerVisible.value = false
      fetchList()
    } finally {
      submitLoading.value = false
    }
  })
}

const openMaintainDrawer = async () => {
  maintainDrawerVisible.value = true
  const data = await getMaintainList()
  maintainList.value = data || []
}

const openAcceptDialog = (row: any) => {
  currentMaintainRow.value = row
  repairContent.value = '现场检修完毕，自检通过放行。'
  acceptDialogVisible.value = true
}

const handleConfirmAccept = async () => {
  if (!repairContent.value) {
    ElMessage.warning('请输入检修记录')
    return
  }
  submitLoading.value = true
  try {
    await acceptMaintain(currentMaintainRow.value.maintainId, {
      repairContent: repairContent.value,
      healthScore: 98
    })
    ElMessage.success('验收合格，设备已恢复闲置就绪')
    acceptDialogVisible.value = false
    maintainDrawerVisible.value = false
    fetchList()
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchList()
  fetchSites()
})
</script>

<style scoped>
.uav-fleet-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.filter-form {
  margin-bottom: 0;
}
.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
}
.uav-card {
  border-radius: 8px;
  border-left: 4px solid #cbd5e1;
}
.border-state-0 { border-left-color: #10b981; }
.border-state-1 { border-left-color: #3b82f6; }
.border-state-2 { border-left-color: #f59e0b; }
.border-state-3 { border-left-color: #ef4444; }
.border-state-4 { border-left-color: #94a3b8; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.sn-badge {
  font-size: 12px;
  color: #64748b;
  font-family: monospace;
}
.model-title {
  margin: 2px 0 0 0;
  font-size: 15px;
  color: #0f172a;
}
.indicator-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.health-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.sub-label {
  font-size: 11px;
  color: #64748b;
  margin-top: 4px;
}
.specs-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.spec-row {
  display: flex;
  align-items: center;
  font-size: 12px;
}
.spec-row .label {
  width: 50px;
  color: #64748b;
}
.bar-wrap {
  flex: 1;
}
.text-blue {
  color: #0284c7;
}
.card-footer {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-end;
  gap: 6px;
}
.guard-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background-color: #fef2f2;
  border: 1px solid #fca5a5;
  border-radius: 4px;
  color: #b91c1c;
  font-size: 12px;
  margin-bottom: 10px;
}
.text-muted {
  color: #94a3b8;
  font-size: 12px;
}
</style>


