<template>
  <div class="order-bills-container">
    <el-row :gutter="14" class="kpi-row">
      <el-col :span="6" v-for="(kpi, i) in kpis" :key="i">
        <el-card shadow="hover" class="kpi-card" :class="kpi.border">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value" :class="kpi.color">{{ kpi.val }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <div class="filter-bar">
        <el-input v-model="queryParams.billSn" placeholder="按对账单号检索" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="queryParams.billState" placeholder="账单状态" clearable style="width: 130px">
          <el-option label="全部状态" :value="undefined" />
          <el-option label="待对账" :value="1" />
          <el-option label="已对账" :value="2" />
          <el-option label="已开票" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 检索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="billList" stripe style="width: 100%">
        <el-table-column prop="billSn" label="对账单编号" min-width="170">
          <template #default="{ row }"><strong>{{ row.billSn }}</strong></template>
        </el-table-column>
        <el-table-column prop="orderSn" label="关联运单" min-width="170">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="goToOrderDetail(row.orderId)">{{ row.orderSn }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="结算商户" min-width="150" />
        <el-table-column prop="amount" label="结算金额 (元)" min-width="130">
          <template #default="{ row }"><span class="price-val">¥{{ Number(row.amount).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column prop="billState" label="对账状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getBillStateTagType(row.billState)" effect="light">{{ row.billStateDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceFlag" label="发票状态" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.invoiceFlag === 1" type="success" size="small">已开具</el-tag>
            <el-tag v-else type="info" size="small">未开票</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="settleAt" label="出账时间" min-width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.billState === 1" link type="success" size="small" @click="handleConfirmBill(row)">确认对账</el-button>
            <el-button v-if="row.billState === 2 && row.invoiceFlag === 0" link type="primary" size="small" @click="openInvoiceDialog(row)">申请开票</el-button>
            <el-button v-if="row.invoiceFlag === 1" link type="warning" size="small" @click="viewInvoice(row)">查看发票</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          layout="total, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="invoiceDialogVisible" title="申请开具增值税电子发票" width="440px">
      <el-form :model="invoiceForm" label-position="top" size="small">
        <el-form-item label="发票抬头" required>
          <el-input v-model="invoiceForm.invoiceTitle" placeholder="如：深圳市丰翼低空物流科技有限公司" />
        </el-form-item>
        <el-form-item label="纳税人识别号" required>
          <el-input v-model="invoiceForm.taxNumber" placeholder="如：91440300MA5EXXX123" />
        </el-form-item>
        <el-form-item label="接收邮箱">
          <el-input v-model="invoiceForm.email" placeholder="如：finance@soarcloud.com" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invoiceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingInvoice" @click="submitInvoice">确认开票</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewInvoiceDialogVisible" title="增值税电子发票凭证" width="460px">
      <div class="invoice-ticket">
        <div class="ticket-header">
          <div class="ticket-title">增值税电子普通发票</div>
          <div class="ticket-sn">发票代码: 044002600111 | 号码: 88920192</div>
        </div>
        <el-divider style="margin: 8px 0" />
        <div class="ticket-row"><span>购买方:</span> <strong>{{ activeInvoiceRow?.merchantName || '客户单位' }}</strong></div>
        <div class="ticket-row"><span>开票金额:</span> <strong class="price-val">¥{{ Number(activeInvoiceRow?.amount || 0).toFixed(2) }}</strong></div>
        <div class="ticket-row"><span>出账时间:</span> <span>{{ activeInvoiceRow?.settleAt }}</span></div>
        <el-divider style="margin: 8px 0" />
        <div class="ticket-footer">销售方: 翔云·低空智慧物流运营平台 (电子发票已存证)</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBillPage, confirmBill, applyInvoice } from '@/api/order'

const router = useRouter()
const loading = ref(false)
const billList = ref<any[]>([])
const total = ref(0)

const invoiceDialogVisible = ref(false)
const submittingInvoice = ref(false)
const activeInvoiceRow = ref<any>(null)
const viewInvoiceDialogVisible = ref(false)

const invoiceForm = reactive({
  billId: undefined as number | undefined,
  invoiceTitle: '深圳市丰翼低空物流科技有限公司',
  taxNumber: '91440300MA5EXXX123',
  email: 'finance@sf-express.com',
  remarks: '低空物流运费结算'
})

const queryParams = reactive({
  billSn: '',
  billState: undefined as number | undefined,
  page: 1,
  size: 10
})

const totalAmount = computed(() => billList.value.reduce((acc, cur) => acc + (Number(cur.amount) || 0), 0))
const pendingCount = computed(() => billList.value.filter(b => b.billState === 1).length)
const reconciledCount = computed(() => billList.value.filter(b => b.billState === 2).length)
const invoicedCount = computed(() => billList.value.filter(b => b.invoiceFlag === 1).length)

const kpis = computed(() => [
  { label: '本页结算总额', val: `¥${totalAmount.value.toFixed(2)}`, border: 'border-blue', color: 'text-blue' },
  { label: '待核对账单', val: `${pendingCount.value} 笔`, border: 'border-orange', color: 'text-orange' },
  { label: '已核对完成', val: `${reconciledCount.value} 笔`, border: 'border-green', color: 'text-green' },
  { label: '已开具发票', val: `${invoicedCount.value} 张`, border: 'border-purple', color: 'text-purple' }
])

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getBillPage(queryParams)
    billList.value = res?.list || []
    total.value = res?.total || 0
  } catch (err: any) {
    ElMessage.error(err.message || '加载账单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { queryParams.page = 1; loadData() }
const handleReset = () => { queryParams.billSn = ''; queryParams.billState = undefined; queryParams.page = 1; loadData() }

const getBillStateTagType = (state: number) => {
  switch (state) {
    case 0: return 'info'
    case 1: return 'warning'
    case 2: return 'primary'
    case 3: return 'success'
    default: return 'info'
  }
}

const goToOrderDetail = (orderId: number) => { router.push(`/order/detail/${orderId}`) }

const handleConfirmBill = async (row: any) => {
  ElMessageBox.confirm(`确认对账单 [${row.billSn}] 结算金额 ¥${row.amount} 核对无误吗？`, '对账确认', {
    confirmButtonText: '确认对账', cancelButtonText: '取消', type: 'success'
  }).then(async () => {
    try {
      await confirmBill(row.billId)
      ElMessage.success('对账确认成功，账单状态更新为已对账！')
      loadData()
    } catch (err: any) { ElMessage.error(err.message || '对账失败') }
  })
}

const openInvoiceDialog = (row: any) => {
  invoiceForm.billId = row.billId
  activeInvoiceRow.value = row
  invoiceDialogVisible.value = true
}

const submitInvoice = async () => {
  if (!invoiceForm.invoiceTitle) return ElMessage.warning('请输入发票抬头')
  if (!invoiceForm.taxNumber) return ElMessage.warning('请输入纳税人识别号')

  submittingInvoice.value = true
  try {
    await applyInvoice(invoiceForm)
    ElMessage.success('发票开具申请已成功提交，电子发票已存证！')
    invoiceDialogVisible.value = false
    loadData()
  } catch (err: any) {
    ElMessage.error(err.message || '开票失败')
  } finally {
    submittingInvoice.value = false
  }
}

const viewInvoice = (row: any) => {
  activeInvoiceRow.value = row
  viewInvoiceDialogVisible.value = true
}

onMounted(() => { loadData() })
</script>

<style scoped>
.order-bills-container { display: flex; flex-direction: column; gap: 14px; }
.kpi-row { margin-bottom: 2px; }
.kpi-card { border-radius: 8px; background: #fff; }
.kpi-label { font-size: 13px; color: #64748b; margin-bottom: 6px; }
.kpi-value { font-size: 22px; font-weight: 700; line-height: 1.2; }
.unit { font-size: 12px; font-weight: normal; color: #94a3b8; }
.text-blue { color: #0284c7; }
.text-orange { color: #f59e0b; }
.text-green { color: #10b981; }
.text-purple { color: #8b5cf6; }
.border-blue { border-left: 4px solid #0284c7; }
.border-orange { border-left: 4px solid #f59e0b; }
.border-green { border-left: 4px solid #10b981; }
.border-purple { border-left: 4px solid #8b5cf6; }
.table-card { border-radius: 8px; }
.filter-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.price-val { font-weight: 700; color: #0f172a; }
.pagination-wrap { margin-top: 14px; display: flex; justify-content: flex-end; }
.invoice-ticket { background: #fdfbf7; border: 1px dashed #d97706; border-radius: 6px; padding: 16px; font-size: 13px; }
.ticket-header { text-align: center; }
.ticket-title { font-size: 16px; font-weight: 700; color: #b45309; }
.ticket-sn { font-size: 11px; color: #92400e; margin-top: 4px; }
.ticket-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
.ticket-footer { text-align: center; font-size: 11px; color: #b45309; margin-top: 8px; }
</style>
