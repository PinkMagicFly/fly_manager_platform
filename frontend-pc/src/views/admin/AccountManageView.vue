<template>
  <div class="account-manage-container">
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <div>
          <h3 class="page-title">平台账号与空管监察员管理</h3>
          <p class="sub-desc">超级管理员专属：支持开设空管监察员账号并分配空域审批与动态管制合规权限</p>
        </div>
        <el-button type="primary" size="large" @click="openCreateAtcDialog">
          <el-icon><Plus /></el-icon> 新建空管监察员账号
        </el-button>
      </div>

      <!-- 搜索筛选栏 -->
      <div class="search-filters">
        <el-select v-model="filterKind" placeholder="账户类别" clearable style="width: 140px" @change="handleSearch">
          <el-option label="全部类型" :value="undefined" />
          <el-option label="空管监察员" :value="4" />
          <el-option label="超级管理员" :value="3" />
          <el-option label="物流商户" :value="2" />
          <el-option label="商用机长" :value="5" />
          <el-option label="货运客户" :value="1" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索账号名 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 账号列表表格 -->
      <el-table :data="accountList" v-loading="loading" border stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="accountId" label="账户ID" width="90" />
        <el-table-column prop="loginName" label="登录账号名" min-width="140">
          <template #default="{ row }">
            <span class="font-mono text-bold">{{ row.loginName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分配角色" min-width="160">
          <template #default="{ row }">
            <template v-if="row.roleNames && row.roleNames.length > 0">
              <el-tag
                v-for="r in row.roleNames"
                :key="r"
                size="small"
                :type="getRoleTagType(r)"
                style="margin-right: 4px; margin-bottom: 2px;"
              >
                {{ r }}
              </el-tag>
            </template>
            <span v-else class="text-muted">{{ row.accountKindLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="accountKindLabel" label="账户类型" width="120" />
        <el-table-column prop="mobile" label="绑定手机" width="130" />
        <el-table-column prop="email" label="电子邮箱" min-width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常使用' : '已冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              :disabled="row.accountId === currentUserId"
              @click="toggleAccountStatus(row)"
            >
              {{ row.status === 1 ? '冻结账户' : '解冻启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pageNo"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadAccounts"
          @current-change="loadAccounts"
        />
      </div>
    </el-card>

    <!-- 新建空管监察员弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建空管监察员账号" width="500px" destroy-on-close>
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="110px">
        <el-form-item label="登录账号名" prop="loginName">
          <el-input v-model="createForm.loginName" placeholder="如：atc_officer_02 (3-20位字母数字)" />
        </el-form-item>

        <el-form-item label="管制机构/姓名" prop="orgName">
          <el-input v-model="createForm.orgName" placeholder="如：深圳低空空管站监察处" />
        </el-form-item>

        <el-form-item label="联系手机号" prop="mobile">
          <el-input v-model="createForm.mobile" placeholder="11位手机号" />
        </el-form-item>

        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" placeholder="默认 123456 (直接可用，免强制改密)" />
        </el-form-item>

        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="工作邮箱 (选填)" />
        </el-form-item>

        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="权限说明：账号创建后将自动赋予 ROLE_ATC (空管监察员) 角色，拥有空域三区态势监管、飞行计划在线相交审批与限高处置权限。登录密码开箱即用，无需首登强制改密。"
          style="margin-top: 10px;"
        />
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSaving" @click="submitCreateAtc">确认开设账号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.accountId)

const loading = ref(false)
const accountList = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)

const filterKind = ref<number | undefined>(undefined)
const keyword = ref('')

const createDialogVisible = ref(false)
const createSaving = ref(false)
const createFormRef = ref<FormInstance>()

const createForm = reactive({
  loginName: '',
  orgName: '',
  mobile: '',
  password: '123456',
  email: ''
})

const createRules = {
  loginName: [
    { required: true, message: '请输入登录账号名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '账号需为3-20位字母、数字或下划线', trigger: 'blur' }
  ],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }]
}

const getRoleTagType = (roleName: string) => {
  if (roleName.includes('空管')) return 'warning'
  if (roleName.includes('管理员')) return 'danger'
  if (roleName.includes('客服')) return 'success'
  if (roleName.includes('调度') || roleName.includes('商户')) return 'primary'
  return 'info'
}

const loadAccounts = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    }
    if (filterKind.value !== undefined) params.accountKind = filterKind.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()

    const res: any = await request.get('/api/v1/admin/accounts/page', { params })
    if (res) {
      accountList.value = res.list || []
      total.value = res.total || 0
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取账号列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNo.value = 1
  loadAccounts()
}

const handleReset = () => {
  filterKind.value = undefined
  keyword.value = ''
  pageNo.value = 1
  loadAccounts()
}

const openCreateAtcDialog = () => {
  createForm.loginName = ''
  createForm.orgName = ''
  createForm.mobile = ''
  createForm.password = '123456'
  createForm.email = ''
  createDialogVisible.value = true
}

const submitCreateAtc = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    createSaving.value = true
    try {
      const res: any = await request.post('/api/v1/admin/accounts/atc', createForm)
      ElMessage.success('空管监察员账号开设成功！')
      createDialogVisible.value = false
      loadAccounts()
    } catch (err: any) {
      ElMessage.error(err.message || '创建账号失败')
    } finally {
      createSaving.value = false
    }
  })
}

const toggleAccountStatus = (row: any) => {
  const target = row.status === 1 ? 2 : 1
  const actionText = target === 1 ? '解冻启用' : '停用冻结'
  ElMessageBox.confirm(`确定要${actionText}账号 [${row.loginName}] 吗？`, '状态变更', {
    type: 'warning'
  }).then(async () => {
    try {
      await request.put(`/api/v1/admin/accounts/${row.accountId}/status?status=${target}`)
      ElMessage.success(`账号已成功${actionText}`)
      loadAccounts()
    } catch (err: any) {
      ElMessage.error(err.message || '操作失败')
    }
  })
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped>
.account-manage-container {
  padding: 16px;
}
.table-card {
  border-radius: 8px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title {
  margin: 0 0 6px 0;
  font-size: 18px;
  color: #0f172a;
}
.sub-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
.search-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.font-mono {
  font-family: monospace;
}
.text-bold {
  font-weight: 600;
}
.text-muted {
  color: #94a3b8;
  font-size: 12px;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

