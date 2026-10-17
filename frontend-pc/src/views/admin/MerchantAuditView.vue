<template>
  <div class="audit-wrap">
    <el-card shadow="never">
      <!-- 搜索筛选栏 -->
      <div class="filter-bar">
        <el-radio-group v-model="filterState" @change="loadData" size="default">
          <el-radio-button :value="undefined">全部企业 (全部)</el-radio-button>
          <el-radio-button :value="0">待审核 (待办)</el-radio-button>
          <el-radio-button :value="1">已通过 (正常)</el-radio-button>
          <el-radio-button :value="2">已驳回 (未通过)</el-radio-button>
        </el-radio-group>

        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索企业全称 / 统一信用代码 / 联系人"
            clearable
            style="width: 280px"
            @keyup.enter="loadData"
          />
          <el-button type="primary" @click="loadData">查询</el-button>
        </div>
      </div>

      <!-- 商户档案表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="merchantId" label="商户ID" width="90" />
        <el-table-column prop="corpName" label="企业全称" min-width="180" />
        <el-table-column prop="licenseNo" label="社会信用代码" width="180" />
        <el-table-column prop="contactName" label="法人联系人" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="160">
          <template #default="{ row }">
            <div class="phone-cell">
              <span>{{ showFullPhone[row.merchantId] ? row.contactPhone : maskPhone(row.contactPhone) }}</span>
              <el-button
                type="primary"
                link
                size="small"
                class="eye-btn"
                @click="togglePhone(row.merchantId)"
                :title="showFullPhone[row.merchantId] ? '隐藏手机号' : '查看明文手机号'"
              >
                <el-icon><component :is="showFullPhone[row.merchantId] ? View : Hide" /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="airworthinessCert" label="适航许可证号" min-width="160" />
        <el-table-column prop="auditState" label="审批状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.auditState === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.auditState === 1" type="success">已通过</el-tag>
            <el-tag v-else type="danger">已驳回</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditNote" label="审批批注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申请提交时间" width="170" />
        <el-table-column label="审批操作" width="180" fixed="right">
          <template #default="{ row }">
            <div v-if="row.auditState === 0">
              <el-button size="small" type="success" @click="handlePass(row)">准予通过</el-button>
              <el-button size="small" type="danger" @click="openRejectDialog(row)">驳回</el-button>
            </div>
            <span v-else class="done-text">已审批办结</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 驳回原因填报弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回企业入驻申请" width="460px">
      <el-form label-position="top">
        <el-form-item label="请输入具体驳回理由 (如资质文件模糊、许可证文号过期等)">
          <el-input
            v-model="rejectNote"
            type="textarea"
            :rows="4"
            placeholder="请详实填写驳回原因，系统将自动通知申请商户..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Hide } from '@element-plus/icons-vue'
import request from '@/utils/request'

const filterState = ref<number | undefined>(undefined)
const keyword = ref('')
const tableData = ref<any[]>([])
const loading = ref(false)

const showFullPhone = ref<Record<number, boolean>>({})

const maskPhone = (phone: string) => {
  if (!phone || phone.length < 7) return phone
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

const togglePhone = (id: number) => {
  showFullPhone.value[id] = !showFullPhone.value[id]
}

const rejectDialogVisible = ref(false)
const currentMerchant = ref<any>(null)
const rejectNote = ref('')
const submitting = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    let url = '/api/v1/merchant/list?'
    if (filterState.value !== undefined) url += `auditState=${filterState.value}&`
    if (keyword.value) url += `keyword=${encodeURIComponent(keyword.value)}&`
    const res: any = await request.get(url)
    tableData.value = res || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const handlePass = (row: any) => {
  ElMessageBox.confirm(`确认批准企业 [${row.corpName}] 入驻申请并激活账号吗？`, '入驻审批', {
    type: 'success',
    confirmButtonText: '批准通过',
    cancelButtonText: '取消'
  }).then(async () => {
    await request.post('/api/v1/merchant/audit', {
      merchantId: row.merchantId,
      auditState: 1,
      auditNote: '企业工商资质与适航材料审核通过，准予接入运营'
    })
    ElMessage.success('企业入驻审核已通过，主账号已自动激活！')
    loadData()
  })
}

const openRejectDialog = (row: any) => {
  currentMerchant.value = row
  rejectNote.value = ''
  rejectDialogVisible.value = true
}

const submitReject = async () => {
  if (!rejectNote.value.trim()) {
    return ElMessage.warning('请填写具体的驳回说明')
  }
  submitting.value = true
  try {
    await request.post('/api/v1/merchant/audit', {
      merchantId: currentMerchant.value.merchantId,
      auditState: 2,
      auditNote: rejectNote.value.trim()
    })
    ElMessage.success('已驳回该企业入驻申请')
    rejectDialogVisible.value = false
    loadData()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-box {
  display: flex;
  gap: 8px;
}
.phone-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.eye-btn {
  margin-left: 4px;
  color: #94a3b8;
}
.eye-btn:hover {
  color: #165dff;
}
.done-text {
  font-size: 13px;
  color: #94a3b8;
}
</style>
