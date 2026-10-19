<template>
  <div class="ticket-center-container">
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <div class="search-filters">
          <el-select v-model="filterState" placeholder="工单状态" clearable style="width: 120px">
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已关闭" :value="2" />
          </el-select>
          <el-select v-model="filterType" placeholder="工单类型" clearable style="width: 120px">
            <el-option label="业务咨询" :value="1" />
            <el-option label="时效投诉" :value="2" />
            <el-option label="异常报修" :value="3" />
          </el-select>
          <el-input v-model="keyword" placeholder="搜索流水号/标题" clearable style="width: 180px" @keyup.enter="handleSearch" />
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <el-table :data="ticketList" v-loading="loading" border stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="ticketSn" label="工单流水号" width="180">
          <template #default="{ row }">
            <span class="font-mono text-bold">{{ row.ticketSn }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="工单标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="content" label="诉求详情" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" width="120">
          <template #default="{ row }">
            <span>{{ row.customerName || `用户#${row.customerId}` }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderSn" label="关联运单" width="160">
          <template #default="{ row }">
            <span>{{ row.orderSn || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="经办专员" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.assigneeName" size="small" type="info">{{ row.assigneeName }}</el-tag>
            <span v-else class="text-muted">待分派</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStateTagType(row.state)">{{ row.stateLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情答复</el-button>
            <el-button v-if="row.state !== 2" link type="warning" size="small" @click="openAssignDialog(row)">分派</el-button>
            <el-button v-if="row.state !== 2" link type="success" size="small" @click="openReplyDialog(row)">回复</el-button>
            <el-button v-if="row.state !== 2" link type="danger" size="small" @click="handleClose(row)">办结</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination v-model:current-page="pageNo" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20]" layout="total, sizes, prev, pager, next"
          @size-change="loadTickets" @current-change="loadTickets" />
      </div>
    </el-card>

    <!-- 分派专员对话框 -->
    <el-dialog v-model="assignDialogVisible" title="分派工单经办专员" width="460px" destroy-on-close>
      <el-alert
        v-if="csList.length === 0"
        type="warning"
        show-icon
        :closable="false"
        title="当前暂无可用客服专员账号"
        description="请商户总账号前往【企业组织管理 - 子账户与权限设置】为员工配置【商户客服专员】角色。"
        style="margin-bottom: 16px;"
      />
      <el-form :model="assignForm" label-width="90px">
        <el-form-item label="选择专员" required>
          <el-select v-model="assignForm.assigneeId" placeholder="选择处理专员" style="width: 100%" :disabled="csList.length === 0">
            <el-option
              v-for="user in csList"
              :key="user.accountId"
              :label="`${user.loginName} (${user.roleLabel}) - ${user.mobile || ''}`"
              :value="user.accountId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" :disabled="csList.length === 0 || !assignForm.assigneeId" @click="submitAssign">确认分派</el-button>
      </template>
    </el-dialog>

    <!-- 官方回复对话框 -->
    <el-dialog v-model="replyDialogVisible" title="官方客服答复" width="480px" destroy-on-close>
      <el-form :model="replyForm" label-width="90px">
        <el-form-item label="答复内容">
          <el-input v-model="replyForm.content" type="textarea" :rows="4" placeholder="请输入答复客户的具体说明与跟进方案" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitReply">发送答复</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" title="工单全景及答复记录" size="480px">
      <div v-if="currentTicket" class="detail-wrap">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="工单单号">{{ currentTicket.ticketSn }}</el-descriptions-item>
          <el-descriptions-item label="工单类型">{{ currentTicket.typeLabel }}</el-descriptions-item>
          <el-descriptions-item label="工单状态">{{ currentTicket.stateLabel }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentTicket.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="客户名称">{{ currentTicket.customerName || `用户#${currentTicket.customerId}` }}</el-descriptions-item>
          <el-descriptions-item label="关联运单">{{ currentTicket.orderSn || '无' }}</el-descriptions-item>
          <el-descriptions-item label="工单标题" :span="2">{{ currentTicket.title }}</el-descriptions-item>
          <el-descriptions-item label="诉求详情" :span="2">{{ currentTicket.content }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 20px 0 12px 0;">沟通流水与答复记录</h4>
        <el-timeline>
          <el-timeline-item v-for="(reply, idx) in currentTicket.replies" :key="idx" :timestamp="reply.at" placement="top">
            <div class="reply-card">
              <div class="reply-header">
                <span class="reply-by">{{ reply.by }}</span>
                <el-tag
                  size="small"
                  :type="reply.role === '客户' ? 'success' : (reply.role === '官方专员' ? 'warning' : 'primary')"
                >
                  {{ reply.role || '客服' }}
                </el-tag>
              </div>
              <p class="reply-body">{{ reply.content }}</p>
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

const filterState = ref<number | undefined>(undefined)
const filterType = ref<number | undefined>(undefined)
const keyword = ref('')
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const ticketList = ref<any[]>([])
const loading = ref(false)
const submitLoading = ref(false)

const csList = ref<any[]>([])

const assignDialogVisible = ref(false)
const currentTicketId = ref<number | null>(null)
const assignForm = ref({ assigneeId: undefined as number | undefined })

const replyDialogVisible = ref(false)
const replyForm = ref({ content: '' })

const detailDrawerVisible = ref(false)
const currentTicket = ref<any>(null)

const getTypeTagType = (type: number) => {
  if (type === 2) return 'danger'
  if (type === 3) return 'warning'
  return 'info'
}

const getStateTagType = (state: number) => {
  if (state === 2) return 'success'
  if (state === 1) return 'primary'
  return 'danger'
}

const loadTickets = async () => {
  loading.value = true
  try {
    let url = `/api/v1/ticket/page?pageNo=${pageNo.value}&pageSize=${pageSize.value}`
    if (filterState.value !== undefined && filterState.value !== null) url += `&state=${filterState.value}`
    if (filterType.value !== undefined && filterType.value !== null) url += `&type=${filterType.value}`
    if (keyword.value.trim()) url += `&keyword=${encodeURIComponent(keyword.value.trim())}`

    const res: any = await request.get(url)
    if (res) {
      ticketList.value = res.list || []
      total.value = res.total || 0
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取工单失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pageNo.value = 1; loadTickets() }
const handleReset = () => {
  filterState.value = undefined
  filterType.value = undefined
  keyword.value = ''
  pageNo.value = 1
  loadTickets()
}

const loadAssignees = async () => {
  try {
    const res: any = await request.get('/api/v1/ticket/assignees')
    csList.value = Array.isArray(res) ? res : []
  } catch (err: any) {
    ElMessage.error(err.message || '获取客服专员列表失败')
    csList.value = []
  }
}

const openAssignDialog = async (row: any) => {
  currentTicketId.value = row.ticketId
  await loadAssignees()
  if (row.assigneeId && csList.value.some(u => u.accountId === row.assigneeId)) {
    assignForm.value.assigneeId = row.assigneeId
  } else if (csList.value.length > 0) {
    assignForm.value.assigneeId = csList.value[0].accountId
  } else {
    assignForm.value.assigneeId = undefined
  }
  assignDialogVisible.value = true
}

const submitAssign = async () => {
  if (!currentTicketId.value || !assignForm.value.assigneeId) return
  submitLoading.value = true
  try {
    await request.put(`/api/v1/ticket/${currentTicketId.value}/assign`, {
      assigneeId: assignForm.value.assigneeId
    })
    ElMessage.success('工单已成功分派')
    assignDialogVisible.value = false
    loadTickets()
  } catch (err: any) {
    ElMessage.error(err.message || '分派失败')
  } finally {
    submitLoading.value = false
  }
}

const openReplyDialog = (row: any) => {
  currentTicketId.value = row.ticketId
  replyForm.value.content = ''
  replyDialogVisible.value = true
}

const submitReply = async () => {
  if (!currentTicketId.value || !replyForm.value.content.trim()) {
    ElMessage.warning('请输入答复内容')
    return
  }
  submitLoading.value = true
  try {
    await request.put(`/api/v1/ticket/${currentTicketId.value}/reply`, {
      content: replyForm.value.content.trim()
    })
    ElMessage.success('官方答复已发送')
    replyDialogVisible.value = false
    loadTickets()
  } catch (err: any) {
    ElMessage.error(err.message || '回复失败')
  } finally {
    submitLoading.value = false
  }
}

const handleClose = (row: any) => {
  ElMessageBox.prompt('请输入办结说明：', '办结关闭工单', {
    confirmButtonText: '确认办结',
    cancelButtonText: '取消',
    inputValue: '诉求已妥善处理并办结关闭'
  }).then(async ({ value }) => {
    try {
      await request.put(`/api/v1/ticket/${row.ticketId}/close`, { note: value })
      ElMessage.success('工单已成功办结关闭')
      loadTickets()
    } catch (err: any) {
      ElMessage.error(err.message || '操作失败')
    }
  }).catch(() => {})
}

const openDetail = async (row: any) => {
  try {
    const res: any = await request.get(`/api/v1/ticket/${row.ticketId}`)
    currentTicket.value = res || row
    detailDrawerVisible.value = true
  } catch (err: any) {
    currentTicket.value = row
    detailDrawerVisible.value = true
  }
}

onMounted(() => {
  loadTickets()
})
</script>

<style scoped>
.ticket-center-container { padding: 16px; }
.table-card { border-radius: 8px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.search-filters { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.font-mono { font-family: monospace; }
.text-bold { font-weight: 600; }
.text-muted { color: #94a3b8; font-size: 12px; }
.reply-card { background: #f8fafc; padding: 10px 14px; border-radius: 6px; }
.reply-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.reply-by { font-weight: bold; color: #0f172a; font-size: 13px; }
.reply-body { margin: 0; color: #334155; font-size: 13px; line-height: 1.5; }
</style>

