<template>
  <div class="alarm-center-container">
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-pending">
          <div class="stat-val">{{ pendingCount }}</div>
          <div class="stat-lbl">⚠️ 未处置待响应</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-processing">
          <div class="stat-val">{{ processingCount }}</div>
          <div class="stat-lbl">🛠️ 应急处置中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-closed">
          <div class="stat-val">{{ closedCount }}</div>
          <div class="stat-lbl">✓ 已解除已关闭</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-urgent">
          <div class="stat-val">{{ urgentCount }}</div>
          <div class="stat-lbl">🔥 严重/紧急高危</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <div class="search-filters">
          <el-select v-model="filterLevel" placeholder="级别" clearable style="width: 110px">
            <el-option label="一般" :value="1" />
            <el-option label="严重" :value="2" />
            <el-option label="紧急" :value="3" />
          </el-select>
          <el-select v-model="filterState" placeholder="状态" clearable style="width: 110px">
            <el-option label="未处置" :value="0" />
            <el-option label="处置中" :value="1" />
            <el-option label="已关闭" :value="2" />
          </el-select>
          <el-input v-model="keyword" placeholder="搜索流水号/标题" clearable style="width: 180px" @keyup.enter="handleSearch" />
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <el-button type="danger" @click="openCreateDialog">+ 手工登记告警</el-button>
      </div>

      <el-table :data="alarmList" v-loading="loading" border stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="alarmSn" label="告警流水号" width="180">
          <template #default="{ row }">
            <span class="font-mono text-bold">{{ row.alarmSn }}</span>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="95">
          <template #default="{ row }">
            <el-tag :type="getLevelTagType(row.level)" effect="dark">
              {{ row.levelLabel || (row.level === 3 ? '紧急' : row.level === 2 ? '严重' : '一般') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="95">
          <template #default="{ row }">
            <el-tag :type="row.source === 1 ? 'danger' : 'info'" size="small">
              {{ row.sourceLabel || (row.source === 1 ? '故障上报' : '手工登记') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="告警标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="content" label="异常描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="关联机号/单号" width="160">
          <template #default="{ row }">
            <div v-if="row.uavSn" class="link-item">机: {{ row.uavSn }}</div>
            <div v-if="row.orderSn" class="link-item">单: {{ row.orderSn }}</div>
            <span v-if="!row.uavSn && !row.orderSn" class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="处置状态" width="105">
          <template #default="{ row }">
            <el-tag :type="getStateTagType(row.state)">
              {{ row.stateLabel || (row.state === 2 ? '已关闭' : row.state === 1 ? '处置中' : '未处置') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发生时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情流水</el-button>
            <el-button v-if="row.state === 0" link type="warning" size="small" @click="openProcessDialog(row)">开始处置</el-button>
            <el-button v-if="row.state === 1" link type="success" size="small" @click="handleCloseAlarm(row)">办结关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination v-model:current-page="pageNo" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20]" layout="total, sizes, prev, pager, next"
          @size-change="loadAlarms" @current-change="loadAlarms" />
      </div>
    </el-card>

    <!-- 手工登记 -->
    <el-dialog v-model="createDialogVisible" title="手工登记异常告警" width="500px" destroy-on-close>
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="告警级别">
          <el-radio-group v-model="createForm.level">
            <el-radio :label="1">一般</el-radio>
            <el-radio :label="2">严重</el-radio>
            <el-radio :label="3">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="告警标题">
          <el-input v-model="createForm.title" placeholder="如：起降坪大风预警" />
        </el-form-item>
        <el-form-item label="异常描述">
          <el-input v-model="createForm.content" type="textarea" :rows="3" placeholder="详细现场情况" />
        </el-form-item>
        <el-form-item label="关联无人机">
          <el-select v-model="createForm.uavId" placeholder="选择无人机(可选)" clearable style="width: 100%">
            <el-option v-for="uav in uavList" :key="uav.uavId" :label="`${uav.uavSn} (${uav.model})`" :value="uav.uavId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitCreate">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 开始处置 -->
    <el-dialog v-model="processDialogVisible" title="开始应急处置" width="460px" destroy-on-close>
      <el-form :model="processForm" label-width="90px">
        <el-form-item label="处置方案">
          <el-input v-model="processForm.note" type="textarea" :rows="3" placeholder="请输入应急措施" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitProcess">确认启动处置</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" title="告警生命周期时间线" size="480px">
      <div v-if="currentAlarm" class="detail-wrap">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="流水号">{{ currentAlarm.alarmSn }}</el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :type="getLevelTagType(currentAlarm.level)" effect="dark" size="small">{{ currentAlarm.levelLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentAlarm.stateLabel }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ currentAlarm.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ currentAlarm.title }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentAlarm.content }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 20px 0 12px 0;">处置时间线留痕</h4>
        <el-timeline>
          <el-timeline-item v-for="(item, idx) in currentAlarm.timeline" :key="idx" :timestamp="item.at" placement="top">
            <div class="timeline-box">
              <span class="act-name">{{ item.action }}</span> - <span class="act-by">{{ item.by }}</span>
              <p class="act-note">{{ item.note }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const filterLevel = ref<number | undefined>(undefined)
const filterState = ref<number | undefined>(undefined)
const keyword = ref('')
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const alarmList = ref<any[]>([])
const loading = ref(false)
const submitLoading = ref(false)

const pendingCount = ref(0)
const processingCount = ref(0)
const closedCount = ref(0)
const urgentCount = ref(0)
const uavList = ref<any[]>([])

const createDialogVisible = ref(false)
const createForm = ref({ level: 1, title: '', content: '', uavId: undefined as number | undefined })
const processDialogVisible = ref(false)
const currentOperateId = ref<number | null>(null)
const processForm = ref({ note: '' })
const detailDrawerVisible = ref(false)
const currentAlarm = ref<any>(null)

const getLevelTagType = (level: number) => {
  if (level === 3) return 'danger'
  if (level === 2) return 'warning'
  return 'info'
}

const getStateTagType = (state: number) => {
  if (state === 2) return 'success'
  if (state === 1) return 'primary'
  return 'danger'
}

const loadAlarms = async () => {
  loading.value = true
  try {
    let url = `/api/v1/alarm?pageNo=${pageNo.value}&pageSize=${pageSize.value}`
    if (filterLevel.value !== undefined && filterLevel.value !== null) url += `&level=${filterLevel.value}`
    if (filterState.value !== undefined && filterState.value !== null) url += `&state=${filterState.value}`
    if (keyword.value.trim()) url += `&keyword=${encodeURIComponent(keyword.value.trim())}`

    const res: any = await request.get(url)
    if (res) {
      alarmList.value = res.list || []
      total.value = res.total || 0
      let p = 0, pr = 0, c = 0, u = 0
      alarmList.value.forEach(item => {
        if (item.state === 0) p++
        if (item.state === 1) pr++
        if (item.state === 2) c++
        if (item.level === 2 || item.level === 3) u++
      })
      pendingCount.value = p
      processingCount.value = pr
      closedCount.value = c
      urgentCount.value = u
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取告警失败')
  } finally {
    loading.value = false
  }
}

const loadUavs = async () => {
  try {
    const list: any = await request.get('/api/v1/uav/list')
    if (list) uavList.value = list
  } catch (ignored) {}
}

const handleSearch = () => { pageNo.value = 1; loadAlarms() }
const handleReset = () => {
  filterLevel.value = undefined
  filterState.value = undefined
  keyword.value = ''
  pageNo.value = 1
  loadAlarms()
}

const openCreateDialog = () => {
  createForm.value = { level: 1, title: '', content: '', uavId: undefined }
  createDialogVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.value.title.trim() || !createForm.value.content.trim()) {
    ElMessage.warning('请填写完整告警标题和描述')
    return
  }
  submitLoading.value = true
  try {
    await request.post('/api/v1/alarm', createForm.value)
    ElMessage.success('手工告警已成功登记')
    createDialogVisible.value = false
    loadAlarms()
  } catch (err: any) {
    ElMessage.error(err.message || '登记失败')
  } finally {
    submitLoading.value = false
  }
}

const openProcessDialog = (row: any) => {
  currentOperateId.value = row.alarmId
  processForm.value.note = ''
  processDialogVisible.value = true
}

const submitProcess = async () => {
  if (!currentOperateId.value) return
  submitLoading.value = true
  try {
    await request.put(`/api/v1/alarm/${currentOperateId.value}/process`, {
      note: processForm.value.note || '值班人员已接管启动处置预案'
    })
    ElMessage.success('告警已转入应急处置中')
    processDialogVisible.value = false
    loadAlarms()
  } catch (err: any) {
    ElMessage.error(err.message || '处置操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleCloseAlarm = (row: any) => {
  ElMessageBox.prompt('请输入解除风险与办结说明：', '办结解除告警二次确认', {
    confirmButtonText: '确认解除关闭',
    cancelButtonText: '取消',
    inputPlaceholder: '如：现场险情排除，设备已安全降落',
    inputValidator: (val) => (!val || !val.trim() ? '办结说明不能为空' : true)
  }).then(async ({ value }) => {
    try {
      await request.put(`/api/v1/alarm/${row.alarmId}/close`, { note: value })
      ElMessage.success('告警事件已成功办结关闭')
      loadAlarms()
    } catch (err: any) {
      ElMessage.error(err.message || '关闭操作失败')
    }
  }).catch(() => {})
}

const openDetail = async (row: any) => {
  try {
    const res: any = await request.get(`/api/v1/alarm/${row.alarmId}`)
    currentAlarm.value = res || row
    detailDrawerVisible.value = true
  } catch (err: any) {
    currentAlarm.value = row
    detailDrawerVisible.value = true
  }
}

onMounted(() => {
  loadAlarms()
  loadUavs()
})
</script>

<style scoped>
.alarm-center-container { padding: 16px; }
.stat-row { margin-bottom: 16px; }
.stat-card { border-radius: 8px; text-align: center; padding: 6px 0; }
.stat-val { font-size: 26px; font-weight: bold; margin-bottom: 4px; }
.stat-lbl { font-size: 13px; color: #64748b; }
.stat-pending .stat-val { color: #ef4444; }
.stat-processing .stat-val { color: #0284c7; }
.stat-closed .stat-val { color: #10b981; }
.stat-urgent .stat-val { color: #ea580c; }
.table-card { border-radius: 8px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.search-filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.font-mono { font-family: monospace; }
.text-bold { font-weight: 600; }
.link-item { font-size: 12px; color: #334155; line-height: 1.4; }
.text-muted { color: #94a3b8; }
.timeline-box { background: #f8fafc; padding: 8px 12px; border-radius: 6px; }
.act-name { font-weight: bold; color: #0f172a; }
.act-by { color: #64748b; font-size: 12px; }
.act-note { margin: 4px 0 0 0; font-size: 13px; color: #334155; }
</style>


