<template>
  <div class="order-board-container">
    <el-row :gutter="12" class="stat-row">
      <el-col :span="4" v-for="(item, idx) in statCards" :key="idx">
        <el-card shadow="hover" class="stat-card" :class="item.border">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value" :class="item.color">{{ item.val }} <span class="unit">{{ item.unit }}</span></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="queryParams.keyword" placeholder="按地址/单号搜索" clearable style="width: 220px" @keyup.enter="handleSearch" />
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
        <div class="filter-right">
          <el-button type="success" :disabled="selectedRows.length === 0" @click="handleBatchAccept">
            <el-icon><Check /></el-icon> 批量接单 ({{ selectedRows.length }})
          </el-button>
          <el-button type="primary" plain @click="goToDispatch">
            <el-icon><Compass /></el-icon> 调度工作台
          </el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="order-tabs">
        <el-tab-pane label="全量看板" name="ALL" />
        <el-tab-pane label="待支付" name="10" />
        <el-tab-pane label="待接单" name="20" />
        <el-tab-pane label="待调度" name="30" />
        <el-tab-pane label="待执飞" name="40" />
        <el-tab-pane label="飞行中" name="50" />
        <el-tab-pane label="已送达" name="60" />
        <el-tab-pane label="已取消" name="70" />
        <el-tab-pane label="异常中断" name="80" />
      </el-tabs>

      <el-table v-loading="loading" :data="orderList" row-key="orderId" stripe style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" :selectable="canSelect" />
        <el-table-column prop="orderSn" label="订单流水号" min-width="170">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="goToDetail(row.orderId)">
              <strong>{{ row.orderSn }}</strong>
            </el-link>
            <el-tag v-if="row.urgentFlag === 1" size="small" type="danger" effect="plain" style="margin-left: 4px">加急</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="goodsTypeDesc" label="货物类别/重量" min-width="140">
          <template #default="{ row }">
            <div>{{ row.goodsTypeDesc }}</div>
            <div class="sub-cell">重量: <strong>{{ row.goodsWeight }}</strong> kg</div>
          </template>
        </el-table-column>
        <el-table-column label="寄收航道" min-width="220">
          <template #default="{ row }">
            <div class="route-box">
              <div class="text-green">起: {{ row.senderAddr }}</div>
              <div class="text-orange">止: {{ row.receiverAddr }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="feeTotal" label="费用/支付" min-width="120">
          <template #default="{ row }">
            <div class="price-val">¥{{ Number(row.feeTotal).toFixed(2) }}</div>
            <el-tag size="small" :type="row.payState === 1 ? 'success' : 'info'">{{ row.payStateDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderState" label="订单状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getStateTagType(row.orderState)" effect="light">{{ row.orderStateDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执飞分配" min-width="130">
          <template #default="{ row }">
            <div v-if="row.uavSn">机号: {{ row.uavSn }}</div>
            <div v-if="row.pilotName" class="sub-cell">机长: {{ row.pilotName }}</div>
            <span v-if="!row.uavSn && !row.pilotName" class="sub-cell text-muted">未指派</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goToDetail(row.orderId)">全景</el-button>
            <el-button v-if="row.orderState === 10" link type="success" size="small" @click="handlePay(row)">模拟支付</el-button>
            <el-button v-if="row.orderState === 20" link type="warning" size="small" @click="handleAccept(row)">确认接单</el-button>
            <el-button v-if="row.orderState === 30" link type="primary" size="small" @click="goToDispatchWithOrder(row.orderId)">指派调度</el-button>
            <el-button v-if="row.orderState === 40" link type="success" size="small" @click="handleTakeoff(row)">确认起飞</el-button>
            <el-button v-if="row.orderState === 50" link type="success" size="small" @click="handleDeliver(row)">送达签收</el-button>
            <el-button v-if="row.orderState < 60" link type="danger" size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getOrderPage,
  getOrderBoardStats,
  payOrder,
  acceptOrder,
  batchAcceptOrders,
  takeoffOrder,
  deliverOrder,
  cancelOrder
} from '@/api/order'

const router = useRouter()
const loading = ref(false)
const orderList = ref<any[]>([])
const total = ref(0)
const selectedRows = ref<any[]>([])
const activeTab = ref('ALL')

const stats = reactive<any>({
  totalCount: 0,
  pendingAcceptCount: 0,
  pendingDispatchCount: 0,
  flyingCount: 0,
  deliveredCount: 0,
  cancelledCount: 0,
  abortedCount: 0
})

const statCards = computed(() => [
  { label: '运单总池', val: stats.totalCount || 0, unit: '单', border: 'border-blue', color: 'text-blue' },
  { label: '待接单审核', val: stats.pendingAcceptCount || 0, unit: '单', border: 'border-orange', color: 'text-orange' },
  { label: '待调度派单', val: stats.pendingDispatchCount || 0, unit: '单', border: 'border-purple', color: 'text-purple' },
  { label: '飞行在途中', val: stats.flyingCount || 0, unit: '架次', border: 'border-cyan', color: 'text-cyan' },
  { label: '已精准送达', val: stats.deliveredCount || 0, unit: '单', border: 'border-green', color: 'text-green' },
  { label: '异常与取消', val: (stats.cancelledCount || 0) + (stats.abortedCount || 0), unit: '单', border: 'border-gray', color: 'text-gray' }
])

const queryParams = reactive({
  keyword: '',
  orderState: undefined as number | undefined,
  page: 1,
  size: 10
})

const loadStats = async () => {
  try {
    const res: any = await getOrderBoardStats()
    Object.assign(stats, res || {})
  } catch (e) {}
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: queryParams.page,
      size: queryParams.size,
      keyword: queryParams.keyword || undefined,
      orderState: queryParams.orderState
    }
    const res: any = await getOrderPage(params)
    orderList.value = res?.list || []
    total.value = res?.total || 0
  } catch (err: any) {
    ElMessage.error(err.message || '加载订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { queryParams.page = 1; loadData() }
const handleReset = () => {
  queryParams.keyword = ''
  queryParams.orderState = undefined
  activeTab.value = 'ALL'
  queryParams.page = 1
  loadData()
}

const handleTabChange = (name: any) => {
  queryParams.orderState = name === 'ALL' ? undefined : Number(name)
  queryParams.page = 1
  loadData()
}

const canSelect = (row: any) => row.orderState === 20
const handleSelectionChange = (selection: any[]) => { selectedRows.value = selection }

const getStateTagType = (state: number) => {
  switch (state) {
    case 10: return 'info'
    case 20: return 'warning'
    case 30: return 'primary'
    case 40: return ''
    case 50: return 'success'
    case 60: return 'success'
    case 70: return 'danger'
    case 80: return 'danger'
    default: return 'info'
  }
}

const goToDetail = (orderId: number) => { router.push(`/order/detail/${orderId}`) }
const goToDispatch = () => { router.push('/order/dispatch') }
const goToDispatchWithOrder = (orderId: number) => { router.push(`/order/dispatch?orderId=${orderId}`) }

const handlePay = async (row: any) => {
  try {
    await payOrder(row.orderId)
    ElMessage.success('模拟支付成功，订单已进入待接单队列！')
    loadData(); loadStats()
  } catch (err: any) { ElMessage.error(err.message || '支付失败') }
}

const handleAccept = async (row: any) => {
  try {
    await acceptOrder(row.orderId)
    ElMessage.success('接单成功，订单已进入待调度池！')
    loadData(); loadStats()
  } catch (err: any) { ElMessage.error(err.message || '接单失败') }
}

const handleBatchAccept = async () => {
  if (selectedRows.value.length === 0) return
  try {
    const ids = selectedRows.value.map(r => r.orderId)
    const count: any = await batchAcceptOrders(ids)
    ElMessage.success(`批量接单成功: ${count} 笔！`)
    loadData(); loadStats()
  } catch (err: any) { ElMessage.error(err.message || '批量接单失败') }
}

const handleTakeoff = async (row: any) => {
  ElMessageBox.confirm(`确认放行起飞运单 [${row.orderSn}] 吗？`, '起飞确认', {
    confirmButtonText: '确认起飞', cancelButtonText: '取消', type: 'success'
  }).then(async () => {
    try {
      await takeoffOrder(row.orderId)
      ElMessage.success('运单已起飞，进入在途监控状态！')
      loadData(); loadStats()
    } catch (err: any) { ElMessage.error(err.message || '起飞失败') }
  })
}

const handleDeliver = async (row: any) => {
  ElMessageBox.confirm(`确认送达并签收运单 [${row.orderSn}] 吗？系统将自动出具对账单。`, '送达签收', {
    confirmButtonText: '确认送达', cancelButtonText: '取消', type: 'success'
  }).then(async () => {
    try {
      await deliverOrder(row.orderId)
      ElMessage.success('签收成功，系统已自动出具财务对账单！')
      loadData(); loadStats()
    } catch (err: any) { ElMessage.error(err.message || '签收失败') }
  })
}

const handleCancel = (row: any) => {
  ElMessageBox.prompt('请输入取消运单的原因说明:', '取消运单', {
    confirmButtonText: '确定取消', cancelButtonText: '放弃', inputPattern: /\S+/, inputErrorMessage: '原因不能为空'
  }).then(async ({ value }) => {
    try {
      await cancelOrder(row.orderId, value)
      ElMessage.success('运单已取消')
      loadData(); loadStats()
    } catch (err: any) { ElMessage.error(err.message || '取消失败') }
  })
}

onMounted(() => { loadStats(); loadData() })
</script>

<style scoped>
.order-board-container { display: flex; flex-direction: column; gap: 16px; }
.stat-row { margin-bottom: 4px; }
.stat-card { border-radius: 8px; background: #ffffff; transition: all 0.2s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-label { font-size: 13px; color: #64748b; margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: 700; line-height: 1.2; }
.unit { font-size: 12px; font-weight: normal; color: #94a3b8; }
.text-blue { color: #0284c7; }
.text-orange { color: #f59e0b; }
.text-purple { color: #8b5cf6; }
.text-cyan { color: #06b6d4; }
.text-green { color: #10b981; }
.text-gray { color: #64748b; }
.border-blue { border-left: 4px solid #0284c7; }
.border-orange { border-left: 4px solid #f59e0b; }
.border-purple { border-left: 4px solid #8b5cf6; }
.border-cyan { border-left: 4px solid #06b6d4; }
.border-green { border-left: 4px solid #10b981; }
.border-gray { border-left: 4px solid #94a3b8; }
.table-card { border-radius: 8px; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 10px; }
.filter-left, .filter-right { display: flex; align-items: center; gap: 8px; }
.order-tabs { margin-bottom: 10px; }
.route-box { display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.sub-cell { font-size: 11px; color: #64748b; }
.price-val { font-weight: 700; color: #0f172a; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
