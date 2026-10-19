<template>
  <div class="subaccount-wrap">
    <el-card shadow="never">
      <div class="table-toolbar">
        <div>
          <h3>商户企业子账号管理</h3>
          <p class="sub-desc">为企业外场飞手、调度员、客服专员及运维专员开设协同子账号与岗位授权</p>
        </div>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon> 新增企业子账号
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="accountId" label="账户ID" width="90" />
        <el-table-column prop="loginName" label="登录用户名" min-width="130" />
        <el-table-column label="分配岗位角色" min-width="160">
          <template #default="{ row }">
            <template v-if="row.roleNames && row.roleNames.length > 0">
              <el-tag
                v-for="r in row.roleNames"
                :key="r"
                size="small"
                :type="r.includes('客服') ? 'success' : (r.includes('调度') ? 'primary' : 'info')"
                style="margin-right: 4px; margin-bottom: 2px;"
              >
                {{ r }}
              </el-tag>
            </template>
            <span v-else class="text-muted">企业成员</span>
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="绑定手机号" width="130">
          <template #default="{ row }">
            {{ formatMobile(row.mobile) }}
          </template>
        </el-table-column>
        <el-table-column prop="email" label="电子邮箱" min-width="150" />
        <el-table-column prop="status" label="账户状态" width="100">
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
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '冻结账户' : '解冻恢复' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增子账号弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建企业子账号" width="500px">
      <el-form :model="createForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="登录账号" prop="loginName">
          <el-input v-model="createForm.loginName" placeholder="如：pilot_dispatcher01" />
        </el-form-item>
        <el-form-item label="手机号码" prop="mobile">
          <el-input v-model="createForm.mobile" placeholder="11位手机号" />
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="工作邮箱" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="岗位角色" prop="roleIds">
          <el-select v-model="createForm.roleIds" multiple placeholder="请选择分配角色" style="width:100%">
            <el-option label="商户客服专员 (ROLE_CS - 负责客户服务工单与跟进答复)" :value="8" />
            <el-option label="商户调度员 (ROLE_DISPATCHER - 负责接单与航线规划)" :value="7" />
            <el-option label="物流商户协同员 (ROLE_MERCHANT - 企业主协同管理)" :value="2" />
            <el-option label="无人机商用机长 (ROLE_PILOT - 移动端执飞)" :value="4" />
            <el-option label="场站运维专员 (ROLE_MAINTAINER - 设施巡检与维保)" :value="5" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import request from '@/utils/request'

const tableData = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const createForm = reactive({
  loginName: '',
  mobile: '',
  email: '',
  password: '123456',
  roleIds: [2]
})

const rules = {
  loginName: [{ required: true, message: '请输入账号名', trigger: 'blur' }],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }]
}

const formatMobile = (m: string) => {
  if (!m || m.length < 11) return m
  return m.substring(0, 3) + '****' + m.substring(7)
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/merchant/sub-accounts')
    tableData.value = res || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  createForm.loginName = ''
  createForm.mobile = ''
  createForm.email = ''
  createForm.password = '123456'
  createForm.roleIds = [2]
  dialogVisible.value = true
}

const submitCreate = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await request.post('/api/v1/merchant/sub-accounts', createForm)
      ElMessage.success('子账号创建成功！')
      dialogVisible.value = false
      loadData()
    } catch (e) {
    } finally {
      saving.value = false
    }
  })
}

const toggleStatus = (row: any) => {
  const target = row.status === 1 ? 2 : 1
  const actionText = target === 1 ? '解冻启用' : '停用冻结'
  ElMessageBox.confirm(`确定要${actionText}子账号 [${row.loginName}] 吗？`, '状态变更', {
    type: 'warning'
  }).then(async () => {
    await request.put(`/api/v1/merchant/sub-accounts/${row.accountId}/status?status=${target}`)
    ElMessage.success(`子账号已成功${actionText}`)
    loadData()
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.table-toolbar h3 {
  margin: 0 0 4px 0;
  color: #0f172a;
}
.sub-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
</style>
