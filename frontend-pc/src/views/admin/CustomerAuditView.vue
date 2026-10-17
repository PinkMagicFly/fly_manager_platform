<template>
  <div class="customer-wrap">
    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <h3>客户实名档案与信用授信</h3>
          <p class="sub-desc">管理下单企业与货运客户身份实名信息，评估核定月结赊销信用额度</p>
        </div>
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索客户姓名 / 身份证"
            clearable
            style="width: 240px"
            @keyup.enter="loadData"
          />
          <el-button type="primary" @click="loadData">查询</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="customerId" label="档案ID" width="90" />
        <el-table-column prop="realName" label="真实姓名/企业主体" min-width="160" />
        <el-table-column prop="idCard" label="身份证/统一信用代码" min-width="200" />
        <el-table-column prop="creditLimit" label="月结授信额度 (元)" width="160">
          <template #default="{ row }">
            <span class="limit-num">¥ {{ Number(row.creditLimit).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="authState" label="认证状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.authState === 1" type="success">已实名认证</el-tag>
            <el-tag v-else-if="row.authState === 0" type="info">待实名</el-tag>
            <el-tag v-else type="danger">认证失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="建档时间" width="180" />
        <el-table-column label="额度与授信调整" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openAdjustDialog(row)">
              调额/复核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 调额与审核弹窗 -->
    <el-dialog v-model="adjustVisible" title="调整客户实名授信" width="450px">
      <el-form label-width="110px">
        <el-form-item label="客户名称">
          <span>{{ currentCustomer?.realName }}</span>
        </el-form-item>
        <el-form-item label="证件/信用代码">
          <span style="font-family: monospace; font-weight: bold;">{{ currentCustomer?.idCard }}</span>
        </el-form-item>
        <el-form-item label="月结授信额度">
          <el-input-number v-model="editLimit" :min="0" :step="1000" style="width:100%" />
        </el-form-item>
        <el-form-item label="实名认证状态">
          <el-select v-model="editAuthState" style="width:100%">
            <el-option label="已认证通过" :value="1" />
            <el-option label="认证未通过" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdjust">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const keyword = ref('')
const tableData = ref<any[]>([])
const loading = ref(false)

const adjustVisible = ref(false)
const currentCustomer = ref<any>(null)
const editLimit = ref(5000)
const editAuthState = ref(1)
const saving = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await request.get(`/api/v1/customer/list?keyword=${encodeURIComponent(keyword.value)}`)
    tableData.value = res || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const openAdjustDialog = (row: any) => {
  currentCustomer.value = row
  editLimit.value = row.creditLimit
  editAuthState.value = row.authState
  adjustVisible.value = true
}

const submitAdjust = async () => {
  saving.value = true
  try {
    await request.put(
      `/api/v1/customer/${currentCustomer.value.customerId}/audit?authState=${editAuthState.value}&creditLimit=${editLimit.value}`
    )
    ElMessage.success('客户实名状态与授信额度已保存更新')
    adjustVisible.value = false
    loadData()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.toolbar h3 {
  margin: 0 0 4px 0;
  color: #0f172a;
}
.sub-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
.search-box {
  display: flex;
  gap: 8px;
}
.limit-num {
  font-weight: 600;
  color: #10b981;
}
</style>
