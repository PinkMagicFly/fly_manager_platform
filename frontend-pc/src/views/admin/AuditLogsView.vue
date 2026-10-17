<template>
  <div class="audit-log-wrap">
    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <h3>敏感操作全局审计流水</h3>
          <p class="sub-desc">不可逆归档全平台用户登录、权限变更、商户资质审核与参数修改行为流水</p>
        </div>
        <div class="search-box">
          <el-input
            v-model="searchModule"
            placeholder="所属业务模块 (如: 商户入驻)"
            clearable
            style="width: 200px"
          />
          <el-input
            v-model="searchLoginName"
            placeholder="操作人登录账号"
            clearable
            style="width: 180px"
          />
          <el-button type="primary" @click="loadLogs">检索审计流水</el-button>
        </div>
      </div>

      <el-table :data="logList" v-loading="loading" border stripe style="width: 100%; margin-top: 16px">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding: 12px 24px; background: #f8fafc; border-radius: 6px;">
              <p style="margin-bottom: 8px;"><b>请求报文入参详情：</b><code style="word-break: break-all; color: #0f172a;">{{ row.reqParams || '无入参或已过滤' }}</code></p>
              <p v-if="row.errorMsg" style="color: #ef4444; margin-top: 6px;"><b>异常报错堆栈：</b><code>{{ row.errorMsg }}</code></p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="logId" label="日志ID" width="90" />
        <el-table-column prop="moduleName" label="所属模块" width="130">
          <template #default="{ row }">
            <el-tag size="small">{{ row.moduleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="actionName" label="操作动作" width="160" />
        <el-table-column prop="loginName" label="操作人" width="120" />
        <el-table-column prop="requestUri" label="请求路由" min-width="180" show-overflow-tooltip />
        <el-table-column prop="clientIp" label="客户端IP" min-width="150" />
        <el-table-column prop="clientKind" label="发起端" width="80" />
        <el-table-column prop="costTime" label="耗时" width="90">
          <template #default="{ row }">
            <span>{{ row.costTime }} ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="respStatus" label="状态码" width="90">
          <template #default="{ row }">
            <el-tag :type="row.respStatus === 200 ? 'success' : 'danger'" size="small">
              {{ row.respStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作发生时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const searchModule = ref('')
const searchLoginName = ref('')
const logList = ref<any[]>([])
const loading = ref(false)

const loadLogs = async () => {
  loading.value = true
  try {
    let url = `/api/v1/admin/audit-logs?limit=50&`
    if (searchModule.value) url += `moduleName=${encodeURIComponent(searchModule.value)}&`
    if (searchLoginName.value) url += `loginName=${encodeURIComponent(searchLoginName.value)}&`
    const res: any = await request.get(url)
    logList.value = res || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLogs()
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
</style>
