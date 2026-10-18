<template>
  <div class="sys-param-container">
    <el-card shadow="never" class="param-card">
      <div class="toolbar">
        <div>
          <h3>平台基础运行参数配置中枢</h3>
          <p class="sub-desc">支持计费规则、订单超时与告警阈值动态热生效（毫秒生效，强审计留痕）</p>
        </div>
        <div>
          <el-button @click="loadParams">刷新最新参数</el-button>
          <el-button type="primary" :loading="saving" @click="handleBatchSave">批量保存全部变更</el-button>
        </div>
      </div>

      <el-collapse v-model="activeCollapse" class="param-collapse" style="margin-top: 20px;">
        <el-collapse-item name="fee">
          <template #title>
            <div class="collapse-title">
              <span class="title-text">💰 运费计费规则参数组 (fee)</span>
              <el-tag size="small" type="success" effect="plain">即时试算生效</el-tag>
            </div>
          </template>
          <el-table :data="feeParams" border stripe style="width: 100%">
            <el-table-column prop="paramKey" label="参数键 (Key)" width="220">
              <template #default="{ row }"><span class="font-mono text-bold">{{ row.paramKey }}</span></template>
            </el-table-column>
            <el-table-column prop="remark" label="参数说明" min-width="220" />
            <el-table-column label="生效值" width="220">
              <template #default="{ row }"><el-input v-model="row.paramValue" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="saveSingleParam(row)">保存热生效</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item name="order">
          <template #title>
            <div class="collapse-title">
              <span class="title-text">⏱️ 订单时效策略参数组 (order)</span>
              <el-tag size="small" type="warning" effect="plain">超时取消调度</el-tag>
            </div>
          </template>
          <el-table :data="orderParams" border stripe style="width: 100%">
            <el-table-column prop="paramKey" label="参数键 (Key)" width="220">
              <template #default="{ row }"><span class="font-mono text-bold">{{ row.paramKey }}</span></template>
            </el-table-column>
            <el-table-column prop="remark" label="参数说明" min-width="220" />
            <el-table-column label="生效值" width="220">
              <template #default="{ row }"><el-input v-model="row.paramValue" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="saveSingleParam(row)">保存热生效</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item name="alarm">
          <template #title>
            <div class="collapse-title">
              <span class="title-text">🛡️ 应急告警预警阈值参数组 (alarm)</span>
              <el-tag size="small" type="danger" effect="plain">安全底线</el-tag>
            </div>
          </template>
          <el-table :data="alarmParams" border stripe style="width: 100%">
            <el-table-column prop="paramKey" label="参数键 (Key)" width="220">
              <template #default="{ row }"><span class="font-mono text-bold">{{ row.paramKey }}</span></template>
            </el-table-column>
            <el-table-column prop="remark" label="参数说明" min-width="220" />
            <el-table-column label="生效值" width="220">
              <template #default="{ row }"><el-input v-model="row.paramValue" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="saveSingleParam(row)">保存热生效</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const activeCollapse = ref(['fee', 'order', 'alarm'])
const allParams = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)

const feeParams = computed(() => allParams.value.filter(p => p.groupName === 'fee' || p.paramKey.startsWith('fee.')))
const orderParams = computed(() => allParams.value.filter(p => (p.groupName === 'order' || p.paramKey.startsWith('order.')) && p.paramKey !== 'order.timeout_pay_minutes' && p.paramKey !== 'ORDER_AUTO_CANCEL_MINUTES'))
const alarmParams = computed(() => allParams.value.filter(p => p.groupName === 'alarm' || p.paramKey.startsWith('alarm.')))

const loadParams = async () => {
  loading.value = true
  try {
    const list: any = await request.get('/api/v1/params')
    allParams.value = list || []
  } catch (err: any) {
    ElMessage.error(err.message || '加载参数失败')
  } finally {
    loading.value = false
  }
}

const saveSingleParam = (row: any) => {
  ElMessageBox.confirm(
    `修改参数【${row.paramKey}】生效值为【${row.paramValue}】将即时热生效并记录全局审计流水，是否确认？`,
    '系统参数热修改二次确认',
    { confirmButtonText: '立即生效', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await request.put(`/api/v1/params/${encodeURIComponent(row.paramKey)}`, {
        paramKey: row.paramKey,
        paramValue: row.paramValue
      })
      ElMessage.success(`参数【${row.paramKey}】已成功更新并热生效`)
      loadParams()
    } catch (err: any) {
      ElMessage.error(err.message || '更新失败')
    }
  }).catch(() => {})
}

const handleBatchSave = () => {
  ElMessageBox.confirm(
    '批量保存修改后的运行参数将全平台即时生效，是否确认提交？',
    '批量保存二次确认',
    { confirmButtonText: '确认保存', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    saving.value = true
    try {
      const payload = allParams.value.map(p => ({
        paramKey: p.paramKey,
        paramValue: p.paramValue,
        remark: p.remark
      }))
      await request.put('/api/v1/params', payload)
      ElMessage.success('系统运行参数已全部更新并热生效')
      loadParams()
    } catch (err: any) {
      ElMessage.error(err.message || '批量保存失败')
    } finally {
      saving.value = false
    }
  }).catch(() => {})
}

onMounted(() => {
  loadParams()
})
</script>

<style scoped>
.sys-param-container { padding: 16px; }
.param-card { border-radius: 8px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.toolbar h3 { margin: 0 0 4px 0; color: #0f172a; }
.sub-desc { margin: 0; font-size: 13px; color: #64748b; }
.collapse-title { display: flex; align-items: center; gap: 10px; font-weight: bold; font-size: 15px; }
.title-text { color: #0f172a; }
.font-mono { font-family: monospace; }
.text-bold { font-weight: 600; }
</style>

